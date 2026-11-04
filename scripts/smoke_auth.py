"""Destructive test-data creation: run only against a disposable local/CI Compose stack."""
import base64
import hashlib
import hmac
import json
import os
import subprocess
import time
import urllib.error
import urllib.request
import uuid


def call(port, method, path, body=None, token=None, expected=200):
    data = None if body is None else json.dumps(body).encode()
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = urllib.request.Request(f"http://localhost:{port}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            code, raw = response.status, response.read()
    except urllib.error.HTTPError as error:
        code, raw = error.code, error.read()
    assert code == expected, f"{method} {path}: expected {expected}, got {code}"
    return json.loads(raw) if raw else None


def sql(database, statement):
    subprocess.run(["docker", "compose", "exec", "-T", "postgres", "psql", "-U", "postgres", "-d", database,
                    "-v", "ON_ERROR_STOP=1"], input=statement, text=True, check=True, capture_output=True)


def wait_ready():
    for port, path in [(8080, "/api/auth/me"), (8081, "/api/habits"), (8082, "/api/savings/me")]:
        deadline = time.monotonic() + 180
        while True:
            try:
                call(port, "GET", path, expected=401)
                break
            except (OSError, AssertionError):
                if time.monotonic() > deadline:
                    raise
                time.sleep(2)


def fake_token(identity, expiry, secret):
    enc = lambda obj: base64.urlsafe_b64encode(json.dumps(obj, separators=(",", ":")).encode()).rstrip(b"=")
    payload = {"sub": identity, "userId": identity, "role": "USER"}
    if expiry is not None:
        payload["exp"] = expiry
    unsigned = enc({"alg": "HS256"}) + b"." + enc(payload)
    return (unsigned + b"." + base64.urlsafe_b64encode(hmac.new(secret.encode(), unsigned, hashlib.sha256).digest()).rstrip(b"=")).decode()


def main():
    wait_ready()
    email = f"{uuid.uuid4()}@example.com"
    credentials = {"email": email, "nickname": "Smoke", "password": "password123"}
    regular = call(8080, "POST", "/api/auth/register", credentials, expected=201)
    assert regular["token"] and regular["user"]["emailVerified"] is False
    call(8080, "POST", "/api/auth/login", {"email": email, "password": "password123"})
    call(8080, "POST", "/api/auth/login", {"email": email, "password": "bad"}, expected=401)
    call(8080, "POST", "/api/auth/register", credentials, expected=409)
    call(8080, "POST", "/api/auth/register", {"email": "bad", "nickname": "", "password": "x"}, expected=400)
    # SMTP is deliberately unavailable in this run. Request failure cannot break login.
    call(8080, "POST", "/api/auth/request-verification", {}, regular["token"])
    call(8080, "POST", "/api/auth/login", {"email": email, "password": "password123"})
    call(8080, "GET", "/api/auth/verify-email?token=invalid", expected=400)

    first = call(8080, "POST", "/api/auth/guest", {}, expected=201)
    second = call(8080, "POST", "/api/auth/guest", {}, expected=201)
    owner, other = first["token"], second["token"]
    owner_id = str(uuid.UUID(first["user"]["id"]))
    assert owner_id != second["user"]["id"]
    assert first["user"]["isGuest"] and first["user"]["email"] == ""
    habits = [{"title": "Smoke habit", "monthlySpend": 3000}]
    call(8081, "POST", "/api/habits", habits, owner, 201)
    owned_habits = call(8081, "GET", "/api/habits", token=owner)
    habit_id = str(uuid.UUID(owned_habits[0]["id"]))
    assert call(8081, "GET", "/api/habits", token=other) == []
    call(8081, "PUT", f"/api/habits/{habit_id}", habits[0], other, 403)
    call(8081, "PATCH", f"/api/habits/{habit_id}/archive", token=other, expected=403)
    call(8081, "POST", f"/api/habits/{habit_id}/relapse", token=other, expected=403)
    goal = call(8082, "POST", "/api/savings/goals", {"title": "Smoke goal", "targetAmount": 1000}, owner, 201)
    goal_id = str(uuid.UUID(goal["id"]))
    assert call(8082, "GET", "/api/savings/goals", token=other) == []
    call(8082, "GET", f"/api/savings/goals/{goal_id}", token=other, expected=404)
    call(8082, "PUT", f"/api/savings/goals/{goal_id}", {"title": "Stolen", "targetAmount": 50}, other, 404)
    call(8082, "DELETE", f"/api/savings/goals/{goal_id}", token=other, expected=404)
    # Seed one already-confirmed transaction; no change to accrual rules or scheduler.
    sql("savings_db", f"""INSERT INTO savings_transactions
        (id, event_id, habit_id, user_id, successful_day, monthly_spend, saved_amount, created_at)
        VALUES ('{uuid.uuid4()}', '{uuid.uuid4()}', '{habit_id}', '{owner_id}', '2020-01-01', 3000, 100, now());""")
    assert call(8082, "GET", "/api/savings/me", token=owner)["totalSaved"] == 100
    assert call(8082, "GET", "/api/savings/me", token=other)["totalSaved"] == 0

    call(8080, "POST", "/api/auth/guest/upgrade", credentials, owner, 409)
    assert call(8080, "GET", "/api/auth/me", token=owner)["isGuest"]
    credentials["email"] = f"{uuid.uuid4()}@example.com"
    upgraded = call(8080, "POST", "/api/auth/guest/upgrade", credentials, owner)
    assert upgraded["user"]["id"] == owner_id and not upgraded["user"]["isGuest"]
    registered_token = upgraded["token"]
    assert call(8081, "GET", "/api/habits", token=registered_token)[0]["id"] == habit_id
    assert call(8082, "GET", f"/api/savings/goals/{goal_id}", token=registered_token)["id"] == goal_id
    assert call(8082, "GET", "/api/savings/me", token=registered_token)["totalSaved"] == 100
    assert call(8080, "GET", "/api/auth/me", token=other)["isGuest"]
    call(8080, "POST", "/api/auth/login", {"email": credentials["email"], "password": "password123"})
    secret = os.environ["JWT_SECRET"]
    bad_tokens = ["invalid", fake_token(owner_id, int(time.time()) - 60, secret),
                  fake_token(owner_id, None, secret), fake_token("bad-uuid", int(time.time()) + 60, secret),
                  fake_token(owner_id, int(time.time()) + 60, "another-secret-value-at-least-32-bytes")]
    for invalid in bad_tokens:
        for port, path in [(8080, "/api/auth/me"), (8081, "/api/habits"), (8082, "/api/savings/me")]:
            call(port, "GET", path, token=invalid, expected=401)
    # Authenticated principal comes exclusively from the token, not request query parameters.
    assert call(8082, "GET", f"/api/savings/me?userId={owner_id}", token=other)["totalSaved"] == 0
    print("PASS: SMTP outage, registration/login, independent guests, all-service JWT rejection, ownership, upgrade with persisted data")


if __name__ == "__main__":
    main()
