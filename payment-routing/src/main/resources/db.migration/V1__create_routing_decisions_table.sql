CREATE TABLE routing_decisions (
                                   id BIGSERIAL PRIMARY KEY,
                                   payment_id VARCHAR(36) NOT NULL,
                                   selected_payment_method VARCHAR(20) NOT NULL,
                                   estimated_cost DECIMAL(19,4) NOT NULL,
                                   use_token BOOLEAN NOT NULL,
                                   available_networks TEXT,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   INDEX idx_payment_id (payment_id)
);