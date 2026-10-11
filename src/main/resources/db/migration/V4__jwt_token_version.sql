-- JWT login: each token carries the account's token_version. Logging out, changing the password or an
-- HR password reset increases it, which cancels every token issued before.
ALTER TABLE user_account ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
