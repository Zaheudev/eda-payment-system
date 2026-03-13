CREATE TABLE routing_cost (
                               id BIGSERIAL PRIMARY KEY,
                               payment_method VARCHAR(20) NOT NULL,
                               fixed_fee NUMERIC(38,2) NOT NULL,
                               percentage_fee NUMERIC(38,2) NOT NULL,
                               authorization_rate DOUBLE PRECISION NOT NULL,
                               is_token BOOLEAN NOT NULL DEFAULT false,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE(payment_method, is_token)
);