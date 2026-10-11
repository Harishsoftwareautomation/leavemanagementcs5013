-- Initial schema: employees, user accounts, leave types, balances and requests.
-- Unique constraints are declared inline because SQLite cannot add them to an existing table.

CREATE TABLE employee (
    employee_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20),
    department        VARCHAR(100) NOT NULL,
    designation       VARCHAR(100) NOT NULL,
    date_of_joining   DATE         NOT NULL,
    salary            REAL         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    reporting_lead_id INTEGER REFERENCES employee (employee_id)
);

CREATE TABLE user_account (
    user_id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username             VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(100) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    employee_id          INTEGER UNIQUE REFERENCES employee (employee_id),
    enabled              INTEGER      NOT NULL DEFAULT 1,
    must_change_password INTEGER      NOT NULL DEFAULT 1
);

CREATE TABLE leave_type (
    leave_type_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    leave_type_name   VARCHAR(100) NOT NULL UNIQUE,
    max_days_per_year INTEGER      NOT NULL,
    description       VARCHAR(500)
);

CREATE TABLE leave_balance (
    leave_balance_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id        INTEGER NOT NULL REFERENCES employee (employee_id),
    leave_type_id      INTEGER NOT NULL REFERENCES leave_type (leave_type_id),
    total_days_allowed INTEGER NOT NULL,
    used_days          INTEGER NOT NULL DEFAULT 0,
    remaining_days     INTEGER NOT NULL,
    year               INTEGER NOT NULL,
    UNIQUE (employee_id, leave_type_id, year)
);

CREATE TABLE leave_request (
    leave_request_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id       INTEGER      NOT NULL REFERENCES employee (employee_id),
    leave_type_id     INTEGER      NOT NULL REFERENCES leave_type (leave_type_id),
    start_date        DATE         NOT NULL,
    end_date          DATE         NOT NULL,
    reason            VARCHAR(500) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    request_date      DATE         NOT NULL,
    approval_date     DATE,
    approver_comments VARCHAR(500),
    approver_id       INTEGER REFERENCES employee (employee_id)
);

CREATE INDEX idx_employee_reporting_lead ON employee (reporting_lead_id);
CREATE INDEX idx_leave_request_employee ON leave_request (employee_id);
CREATE INDEX idx_leave_request_status ON leave_request (status);
