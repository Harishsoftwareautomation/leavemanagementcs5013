-- Phase 3: decimal (half) days, half-day leave, negative balances, compensatory leave (comp-off),
-- HR balance adjustments and a ledger of every balance change.

-- Built-in "Compensatory Leave" type, identified by system_code rather than by name
ALTER TABLE leave_type ADD COLUMN system_code VARCHAR(20);
CREATE UNIQUE INDEX ux_leave_type_system_code ON leave_type (system_code);
UPDATE leave_type SET system_code = 'COMP_OFF' WHERE leave_type_name = 'Compensatory Leave';
INSERT INTO leave_type (leave_type_name, max_days_per_year, description, system_code)
SELECT 'Compensatory Leave', 0, 'Days earned by working on a weekend or holiday. Credits never expire.', 'COMP_OFF'
WHERE NOT EXISTS (SELECT 1 FROM leave_type WHERE system_code = 'COMP_OFF');

-- Balances: remaining = total + adjusted - used, calculated instead of stored.
-- adjusted_days holds comp-off credits and HR adjustments. Day values may now be halves (e.g. 11.5).
-- The comp-off balance does not belong to a year; it is stored with year = 0.
ALTER TABLE leave_balance ADD COLUMN adjusted_days NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE leave_balance DROP COLUMN remaining_days;

-- Half-day leave: FIRST_HALF or SECOND_HALF on a single-day request
ALTER TABLE leave_request ADD COLUMN half_day VARCHAR(20);

-- Approval history now covers leave and comp-off requests (table rebuilt to drop the leave_request foreign key)
CREATE TABLE leave_approval_new (
    approval_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    request_type         VARCHAR(20)  NOT NULL DEFAULT 'LEAVE',
    request_id           INTEGER      NOT NULL,
    level                VARCHAR(20)  NOT NULL,
    action               VARCHAR(20)  NOT NULL,
    approver_user_id     INTEGER REFERENCES user_account (user_id),
    approver_employee_id INTEGER REFERENCES employee (employee_id),
    approver_name        VARCHAR(255) NOT NULL,
    comments             VARCHAR(500),
    on_behalf            INTEGER      NOT NULL DEFAULT 0,
    acted_at             TIMESTAMP    NOT NULL
);
INSERT INTO leave_approval_new (approval_id, request_type, request_id, level, action, approver_user_id,
                                approver_employee_id, approver_name, comments, on_behalf, acted_at)
SELECT approval_id, 'LEAVE', leave_request_id, level, action, approver_user_id,
       approver_employee_id, approver_name, comments, on_behalf, acted_at
FROM leave_approval;
DROP TABLE leave_approval;
ALTER TABLE leave_approval_new RENAME TO leave_approval;
CREATE INDEX idx_leave_approval_request ON leave_approval (request_type, request_id);

-- Comp-off requests: whole days worked on a weekend/holiday, approved like leave (Team Lead, then HR)
CREATE TABLE comp_off_request (
    comp_off_request_id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id         INTEGER      NOT NULL REFERENCES employee (employee_id),
    worked_from         DATE         NOT NULL,
    worked_to           DATE         NOT NULL,
    days                NUMERIC      NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING_TL',
    request_date        DATE         NOT NULL
);
CREATE INDEX idx_comp_off_employee ON comp_off_request (employee_id);
CREATE INDEX idx_comp_off_status ON comp_off_request (status);

-- Ledger: one row per balance change (+ adds days, - takes days)
CREATE TABLE leave_transaction (
    transaction_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id        INTEGER      NOT NULL REFERENCES employee (employee_id),
    leave_type_id      INTEGER      NOT NULL REFERENCES leave_type (leave_type_id),
    year               INTEGER      NOT NULL,
    days               NUMERIC      NOT NULL,
    type               VARCHAR(20)  NOT NULL,
    reference_id       INTEGER,
    reason             VARCHAR(500),
    created_by_user_id INTEGER REFERENCES user_account (user_id),
    created_by_name    VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP    NOT NULL
);
CREATE INDEX idx_leave_transaction_employee ON leave_transaction (employee_id, year);

-- Opening ledger rows for existing balances, so the ledger adds up to each balance
INSERT INTO leave_transaction (employee_id, leave_type_id, year, days, type, reason, created_by_name, created_at)
SELECT employee_id, leave_type_id, year, total_days_allowed, 'ALLOCATION', 'Opening balance (migrated)',
       'System', strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')
FROM leave_balance;
INSERT INTO leave_transaction (employee_id, leave_type_id, year, days, type, reason, created_by_name, created_at)
SELECT employee_id, leave_type_id, year, -used_days, 'LEAVE_DEDUCTED', 'Days used before the ledger existed (migrated)',
       'System', strftime('%Y-%m-%d %H:%M:%f', 'now', 'localtime')
FROM leave_balance
WHERE used_days <> 0;
