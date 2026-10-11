-- Phase 2: two-level approval (Team Lead, then HR).
-- Approval decisions move from columns on leave_request into a leave_approval history table,
-- and status PENDING becomes PENDING_TL (waiting for the Team Lead).

CREATE TABLE leave_approval (
    approval_id          INTEGER PRIMARY KEY AUTOINCREMENT,
    leave_request_id     INTEGER      NOT NULL REFERENCES leave_request (leave_request_id),
    level                VARCHAR(20)  NOT NULL,
    action               VARCHAR(20)  NOT NULL,
    approver_user_id     INTEGER REFERENCES user_account (user_id),
    approver_employee_id INTEGER REFERENCES employee (employee_id),
    approver_name        VARCHAR(255) NOT NULL,
    comments             VARCHAR(500),
    on_behalf            INTEGER      NOT NULL DEFAULT 0,
    acted_at             TIMESTAMP    NOT NULL
);

-- Existing single-step decisions are recorded as HR-level actions
INSERT INTO leave_approval (leave_request_id, level, action, approver_user_id, approver_employee_id,
                            approver_name, comments, on_behalf, acted_at)
SELECT lr.leave_request_id,
       'HR',
       CASE lr.status WHEN 'REJECTED' THEN 'REJECTED' ELSE 'APPROVED' END,
       ua.user_id,
       lr.approver_id,
       COALESCE(e.first_name || ' ' || e.last_name, 'Unknown'),
       lr.approver_comments,
       0,
       COALESCE(lr.approval_date, lr.request_date)
FROM leave_request lr
LEFT JOIN employee e ON e.employee_id = lr.approver_id
LEFT JOIN user_account ua ON ua.employee_id = lr.approver_id
WHERE lr.status IN ('APPROVED', 'REJECTED')
   OR (lr.status = 'CANCELLED' AND lr.approval_date IS NOT NULL);

-- SQLite cannot drop columns that carry a foreign key, so the table is rebuilt
CREATE TABLE leave_request_new (
    leave_request_id INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id      INTEGER      NOT NULL REFERENCES employee (employee_id),
    leave_type_id    INTEGER      NOT NULL REFERENCES leave_type (leave_type_id),
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING_TL',
    request_date     DATE         NOT NULL
);

INSERT INTO leave_request_new (leave_request_id, employee_id, leave_type_id, start_date, end_date, reason, status, request_date)
SELECT leave_request_id, employee_id, leave_type_id, start_date, end_date, reason,
       CASE status WHEN 'PENDING' THEN 'PENDING_TL' ELSE status END,
       request_date
FROM leave_request;

DROP TABLE leave_request;
ALTER TABLE leave_request_new RENAME TO leave_request;

CREATE INDEX idx_leave_request_employee ON leave_request (employee_id);
CREATE INDEX idx_leave_request_status ON leave_request (status);
CREATE INDEX idx_leave_approval_request ON leave_approval (leave_request_id);
