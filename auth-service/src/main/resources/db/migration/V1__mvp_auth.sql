-- Supports a clean database and the existing Hibernate-managed schema.
-- Existing users retain IDs, passwords, roles and true verification flags.
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY,
    email varchar(255) UNIQUE,
    nickname varchar(255) NOT NULL,
    password_hash varchar(255),
    role varchar(255) NOT NULL,
    email_verified boolean
);
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_guest boolean NOT NULL DEFAULT false;
UPDATE users SET email_verified = false WHERE email_verified IS NULL;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT false;

-- Abort rather than merge existing identities differing only by case/whitespace.
DO $$
BEGIN
    IF EXISTS (SELECT lower(btrim(email)) FROM users WHERE email IS NOT NULL
               GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Case-insensitive email collision: resolve account ownership before migration';
    END IF;
END $$;
UPDATE users SET email = lower(btrim(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS users_email_normalized_unique ON users (lower(btrim(email)));
ALTER TABLE users ADD CONSTRAINT users_guest_credentials_check CHECK (
    (is_guest AND email IS NULL AND password_hash IS NULL AND email_verified = false)
    OR (NOT is_guest AND email IS NOT NULL AND password_hash IS NOT NULL)
);
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone
);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON email_verification_tokens (user_id);
