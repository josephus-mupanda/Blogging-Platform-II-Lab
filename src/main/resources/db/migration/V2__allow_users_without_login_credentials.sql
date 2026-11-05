-- Keep the Platform I columns and existing values while allowing profile-only users.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'READER';
