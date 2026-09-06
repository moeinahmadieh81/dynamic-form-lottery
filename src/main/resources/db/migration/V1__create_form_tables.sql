CREATE TABLE forms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    current_version INTEGER NOT NULL DEFAULT 1,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_form_period CHECK (end_at IS NULL OR start_at IS NULL OR end_at > start_at)
);

CREATE TABLE form_versions (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL REFERENCES forms(id),
    version INTEGER NOT NULL,
    schema JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_form_version UNIQUE(form_id, version)
);

CREATE INDEX idx_forms_status ON forms(status);
CREATE INDEX idx_form_versions_form_id ON form_versions(form_id);
