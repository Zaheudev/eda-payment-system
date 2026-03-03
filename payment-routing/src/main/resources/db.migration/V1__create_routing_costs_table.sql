CREATE TABLE routing_costs (
                               id BIGSERIAL PRIMARY KEY,
                               payment_method VARCHAR(20) NOT NULL,
                               fixed_fee DECIMAL(19,4) NOT NULL,
                               percentage_fee DECIMAL(19,6) NOT NULL,
                               authorization_rate DECIMAL(3,2) NOT NULL,
                               is_token BOOLEAN NOT NULL DEFAULT false,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE(payment_method, is_token)
);