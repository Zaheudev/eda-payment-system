INSERT INTO routing_costs (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES
    ('VISA', 0.10, 0.015, 0.82, false),
    ('MASTERCARD', 0.12, 0.014, 0.83, false),
    ('AMEX', 0.15, 0.022, 0.80, false),
    -- Token-based costs (cheaper)
    ('VISA', 0.09, 0.014, 0.85, true),
    ('MASTERCARD', 0.11, 0.013, 0.86, true);