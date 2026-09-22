CREATE TABLE roles
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE
);

CREATE TABLE permissions
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(256)       NULL
);

CREATE TABLE role_permissions
(
    role_id       BIGINT REFERENCES roles (id),
    permission_id BIGINT REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE users
(
    id         SERIAL PRIMARY KEY,
    email      VARCHAR(100) UNIQUE                                             NOT NULL,
    password   VARCHAR(256)                                                    NOT NULL,
    role_id    BIGINT REFERENCES roles (id)                                    NOT NULL,
    fname      VARCHAR(100)                                                    NOT NULL,
    lname      VARCHAR(100)                                                    NOT NULL,
    gender     VARCHAR(10)                                                     NOT NULL,
    status     VARCHAR(50)                 DEFAULT 'ACTIVE'::CHARACTER VARYING NOT NULL,
    created_at TIMESTAMP(0) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX idx_users_email ON users (email);

CREATE TABLE tests
(
    id                   SERIAL PRIMARY KEY,
    title                VARCHAR(256)                NOT NULL,
    description          TEXT                        NULL,
    duration             INTEGER                     NOT NULL,
    no_of_questions      INTEGER                     NOT NULL,
    passing_percentage   INTEGER                     NOT NULL,
    should_shuffle       BOOLEAN DEFAULT TRUE        NOT NULL,
    should_randomly_pick BOOLEAN DEFAULT TRUE        NOT NULL,
    deleted_at           TIMESTAMP(0) WITH TIME ZONE NULL
);

CREATE TABLE test_events
(
    id               SERIAL PRIMARY KEY,
    test_taker_id    BIGINT REFERENCES users (id)                                       NOT NULL,
    test_id          BIGINT                                                             REFERENCES tests (id) ON DELETE SET NULL,
    event_datetime   TIMESTAMP(0) WITH TIME ZONE                                        NOT NULL,
    status           VARCHAR(50)                 DEFAULT 'SCHEDULED'::CHARACTER VARYING NOT NULL,
    score_points     REAL                                                               NULL,
    score_percentage INTEGER                                                            NULL,
    is_passed        BOOLEAN                                                            NULL,
    started_at       TIMESTAMP(0) WITH TIME ZONE                                        NULL,
    finished_at      TIMESTAMP(0) WITH TIME ZONE                                        NULL,
    created_at       TIMESTAMP(0) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    test_attempt     jsonb                                                              NULL
);

CREATE TABLE question_types
(
    id   SERIAL PRIMARY KEY,
    type VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE questions
(
    id               SERIAL PRIMARY KEY,
    test_id          BIGINT REFERENCES tests (id) ON DELETE CASCADE,
    question_type_id BIGINT REFERENCES question_types (id),
    title            VARCHAR(256) NOT NULL,
    description      TEXT         NULL,
    image            VARCHAR(256) NULL,
    point            REAL         NOT NULL
);

CREATE TABLE options
(
    id          SERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES questions (id) ON DELETE CASCADE,
    option      VARCHAR(256) NOT NULL,
    is_answer   BOOLEAN      NOT NULL
);

-- Views --
CREATE VIEW test_events_view AS
SELECT te.id         AS test_event_id,
       event_datetime,
       te.status     AS test_event_status,
       score_points,
       score_percentage,
       is_passed,
       started_at,
       finished_at,
       te.created_at AS test_event_created_at,
       test_attempt,
       test_taker_id,
       email,
       password,
       role_id,
       roles.name    AS role_name,
       fname,
       lname,
       gender,
       u.status      AS user_status,
       u.created_at  AS user_created_at,
       te.test_id    AS test_id,
       title,
       description,
       duration,
       no_of_questions,
       passing_percentage,
       should_shuffle,
       should_randomly_pick,
       deleted_at
FROM test_events te
         LEFT JOIN users u ON u.id = te.test_taker_id
         LEFT JOIN roles ON roles.id = u.role_id
         LEFT JOIN tests ON tests.id = te.test_id;
