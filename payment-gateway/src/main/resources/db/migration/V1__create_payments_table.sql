-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    payment_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    token_ref VARCHAR(255),
    timestamp BIGINT,
    amount numeric(38,2) NOT NULL,
    currency VARCHAR(10)
);

-- Create index on timestamp for better query performance
CREATE INDEX idx_payments_timestamp ON payments(timestamp);

-- Create index on status for filtering
CREATE INDEX idx_payments_status ON payments(status);

