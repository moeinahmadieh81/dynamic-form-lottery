CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

ALTER TABLE submissions
    ADD CONSTRAINT fk_submissions_user
    FOREIGN KEY (user_id) REFERENCES users(id);

-- Existing development submissions may have user_id = NULL from before authentication existed.
-- New submissions are always authenticated at the application layer.
CREATE UNIQUE INDEX uq_submissions_form_user
    ON submissions(form_id, user_id)
    WHERE user_id IS NOT NULL;
