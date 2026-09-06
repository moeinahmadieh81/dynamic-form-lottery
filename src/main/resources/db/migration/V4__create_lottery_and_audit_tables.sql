CREATE TABLE lotteries (
    id BIGSERIAL PRIMARY KEY,
    form_id BIGINT NOT NULL REFERENCES forms(id),
    status VARCHAR(30) NOT NULL,
    winner_count INTEGER NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_lotteries_status
        CHECK (status IN ('READY', 'RUNNING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_lotteries_winner_count
        CHECK (winner_count > 0)
);

CREATE INDEX idx_lotteries_form_id ON lotteries(form_id);
CREATE INDEX idx_lotteries_status ON lotteries(status);

-- MVP rule: a form can have at most one non-cancelled lottery.
CREATE UNIQUE INDEX uq_lotteries_form_active
    ON lotteries(form_id)
    WHERE status IN ('READY', 'RUNNING', 'COMPLETED');

CREATE TABLE lottery_entries (
    id BIGSERIAL PRIMARY KEY,
    lottery_id BIGINT NOT NULL REFERENCES lotteries(id),
    submission_id BIGINT NOT NULL REFERENCES submissions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_lottery_entries_submission UNIQUE (lottery_id, submission_id),
    CONSTRAINT uq_lottery_entries_user UNIQUE (lottery_id, user_id)
);

CREATE INDEX idx_lottery_entries_lottery_id ON lottery_entries(lottery_id);

CREATE TABLE lottery_winners (
    id BIGSERIAL PRIMARY KEY,
    lottery_id BIGINT NOT NULL REFERENCES lotteries(id),
    lottery_entry_id BIGINT NOT NULL REFERENCES lottery_entries(id),
    position INTEGER NOT NULL,
    selected_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_lottery_winners_position CHECK (position > 0),
    CONSTRAINT uq_lottery_winners_position UNIQUE (lottery_id, position),
    CONSTRAINT uq_lottery_winners_entry UNIQUE (lottery_id, lottery_entry_id)
);

CREATE INDEX idx_lottery_winners_lottery_id ON lottery_winners(lottery_id);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
