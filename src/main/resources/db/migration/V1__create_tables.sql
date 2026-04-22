CREATE TABLE rate_limit_rules (
                                  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                  service_id  VARCHAR(50) NOT NULL,
                                  endpoint    VARCHAR(100),
                                  capacity    INT         NOT NULL,
                                  refill_rate INT         NOT NULL,
                                  active      BOOLEAN     DEFAULT TRUE,
                                  created_at  TIMESTAMP   DEFAULT NOW(),
                                  updated_at  TIMESTAMP   DEFAULT NOW(),
                                  CONSTRAINT uq_service_endpoint UNIQUE (service_id, endpoint)
);

CREATE TABLE rate_limit_events (
                                   id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                   service_id  VARCHAR(50),
                                   user_id     VARCHAR(100),
                                   allowed     BOOLEAN,
                                   remaining   INT,
                                   created_at  TIMESTAMP   DEFAULT NOW()
);

CREATE INDEX idx_events_service_user
    ON rate_limit_events (service_id, user_id, created_at DESC);

INSERT INTO rate_limit_rules (service_id, capacity, refill_rate)
VALUES ('banking', 20, 5);