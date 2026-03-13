-- SIGNATURE NETWORKS (credit card networks)
-- Caracteristici: fees mai mari, auth rates mai mari

-- VISA - PAN (Primary Account Number - cardul direct)
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('VISA', 0.10, 0.015, 0.82, false);

-- VISA - TOKEN (card tokenizat)
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('VISA', 0.09, 0.014, 0.85, true);

-- MASTERCARD - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('MASTERCARD', 0.12, 0.014, 0.83, false);

-- MASTERCARD - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('MASTERCARD', 0.11, 0.013, 0.86, true);

-- AMEX - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('AMEX', 0.15, 0.022, 0.80, false);

-- AMEX - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('AMEX', 0.14, 0.021, 0.83, true);

-- DISCOVER - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('DISCOVER', 0.10, 0.016, 0.85, false);

-- DISCOVER - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('DISCOVER', 0.09, 0.015, 0.87, true);


-- DEBIT NETWORKS (US PIN debit networks)
-- Caracteristici: fees mai mici, auth rates mai mici

-- ACCEL - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('ACCEL', 0.05, 0.005, 0.80, false);

-- ACCEL - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('ACCEL', 0.04, 0.0045, 0.82, true);

-- STAR - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('STAR', 0.04, 0.004, 0.79, false);

-- STAR - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('STAR', 0.03, 0.0035, 0.81, true);

-- NYCE - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('NYCE', 0.03, 0.003, 0.77, false);

-- NYCE - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('NYCE', 0.025, 0.0025, 0.79, true);

-- PULSE - PAN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('PULSE', 0.03, 0.0035, 0.84, false);

-- PULSE - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('PULSE', 0.025, 0.003, 0.86, true);

-- MAESTRO - PAN (international debit)
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('MAESTRO', 0.08, 0.01, 0.83, false);

-- MAESTRO - TOKEN
INSERT INTO routing_cost (payment_method, fixed_fee, percentage_fee, authorization_rate, is_token)
VALUES ('MAESTRO', 0.07, 0.009, 0.85, true);