CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL REFERENCES forms(id),
    form_version_id BIGINT NOT NULL REFERENCES form_versions(id),
    user_id BIGINT,
    answers JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_submissions_form_id ON submissions(form_id);
CREATE INDEX idx_submissions_form_version_id ON submissions(form_version_id);
CREATE INDEX idx_submissions_user_id ON submissions(user_id);
CREATE INDEX idx_submissions_status ON submissions(status);
