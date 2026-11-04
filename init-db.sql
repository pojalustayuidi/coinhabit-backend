-- psql script: safe to rerun on existing volumes, without deleting data.
SELECT 'CREATE DATABASE habit_db' WHERE NOT EXISTS
    (SELECT FROM pg_database WHERE datname = 'habit_db')\gexec
SELECT 'CREATE DATABASE savings_db' WHERE NOT EXISTS
    (SELECT FROM pg_database WHERE datname = 'savings_db')\gexec
