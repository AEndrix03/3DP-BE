-- ENUM LOGICHE ESSENZIALI
CREATE TABLE printer_status
(
    code        TEXT PRIMARY KEY,
    description TEXT
);

CREATE TABLE job_status
(
    code        TEXT PRIMARY KEY,
    description TEXT
);

-- TABELLE PRINCIPALI

CREATE TABLE printer
(
    id        UUID PRIMARY KEY,
    name      TEXT NOT NULL,
    driver_id TEXT,
    status    TEXT REFERENCES printer_status (code),
    last_seen TIMESTAMP
);

CREATE TABLE driver
(
    id         TEXT PRIMARY KEY,
    printer_id UUID REFERENCES printer (id),
    last_auth  TIMESTAMP,
    public_key TEXT
);

CREATE TABLE file_resource
(
    id          UUID PRIMARY KEY,
    file_name   TEXT  NOT NULL,
    content     BYTEA NOT NULL,
    uploaded_at TIMESTAMP,
    checksum    TEXT
);

CREATE TABLE slicing_result
(
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_file_id    UUID NOT NULL REFERENCES file_resource (id),
    generated_file_id UUID REFERENCES file_resource (id),
    parameters        JSONB,
    logs              TEXT,
    created_at        TIMESTAMP        DEFAULT now()
);

CREATE TABLE job
(
    id                UUID PRIMARY KEY,
    printer_id        UUID REFERENCES printer (id),
    file_id           UUID REFERENCES file_resource (id),
    status            TEXT REFERENCES job_status (code),
    progress          INTEGER,
    start_offset_line INTEGER,
    created_at        TIMESTAMP,
    started_at        TIMESTAMP,
    finished_at       TIMESTAMP
);

CREATE TABLE job_history
(
    id          UUID PRIMARY KEY,
    job_id      UUID REFERENCES job (id),
    from_status TEXT REFERENCES job_status (code),
    to_status   TEXT REFERENCES job_status (code),
    changed_at  TIMESTAMP,
    reason      TEXT
);

CREATE TABLE event
(
    id         UUID PRIMARY KEY,
    printer_id UUID REFERENCES printer (id),
    type       TEXT,
    payload    JSONB,
    timestamp  TIMESTAMP
);

CREATE TABLE command_request
(
    id           UUID PRIMARY KEY,
    printer_id   UUID REFERENCES printer (id),
    content      JSONB,
    submitted_at TIMESTAMP,
    executed     BOOLEAN
);

CREATE TABLE revoked_token
(
    jti        TEXT PRIMARY KEY,
    user_ref   TEXT,
    revoked_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabella dei livelli di log
CREATE TABLE log_level
(
    code        VARCHAR(20) PRIMARY KEY,
    description TEXT
);

-- Tabella dei log con relazione a log_level
CREATE TABLE log_entry
(
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp TIMESTAMP,
    level     VARCHAR(20),
    logger    TEXT,
    message   TEXT,
    context   JSONB,
    CONSTRAINT fk_log_level FOREIGN KEY (level) REFERENCES log_level (code)
);

CREATE TABLE printer_assignment
(
    id         UUID PRIMARY KEY,
    user_ref   TEXT NOT NULL,
    printer_id UUID REFERENCES printer (id),
    permission TEXT,
    UNIQUE (user_ref, printer_id)
);

CREATE TABLE printer_configuration
(
    id         UUID PRIMARY KEY,
    printer_id UUID REFERENCES printer (id),
    config     JSONB,
    created_at TIMESTAMP
);

CREATE TABLE firmware_version
(
    id          UUID PRIMARY KEY,
    version     TEXT,
    notes       TEXT,
    released_at TIMESTAMP
);

CREATE TABLE printer_firmware
(
    id           UUID PRIMARY KEY,
    printer_id   UUID REFERENCES printer (id),
    firmware_id  UUID REFERENCES firmware_version (id),
    installed_at TIMESTAMP
);

CREATE TABLE notification
(
    id         UUID PRIMARY KEY,
    user_ref   TEXT,
    title      TEXT,
    body       TEXT,
    severity   TEXT,
    created_at TIMESTAMP,
    read       BOOLEAN
);

-- abilita la generazione di UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- tabella dei tipi di evento di audit
CREATE TABLE audit_event_type
(
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    description VARCHAR(50) NOT NULL
);

-- tabella dei log di audit
CREATE TABLE audit_logs
(
    id              UUID PRIMARY KEY                     DEFAULT uuid_generate_v4(),
    "timestamp"     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    assegnatario_id VARCHAR(100)                NOT NULL,
    event_type      UUID                        NOT NULL,
    description     TEXT,
    metadata        JSONB,
    CONSTRAINT fk_audit_event_type
        FOREIGN KEY (event_type)
            REFERENCES audit_event_type (id)
);

