-- Composite indexes for the paginated/read-heavy endpoints added in the read API phase.
CREATE INDEX idx_forms_status_updated_at
    ON forms(status, updated_at DESC);

CREATE INDEX idx_submissions_form_submitted_at
    ON submissions(form_id, submitted_at DESC);

CREATE INDEX idx_submissions_user_submitted_at
    ON submissions(user_id, submitted_at DESC)
    WHERE user_id IS NOT NULL;

CREATE INDEX idx_lotteries_form_status_created_at
    ON lotteries(form_id, status, created_at DESC);
