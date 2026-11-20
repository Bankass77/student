CREATE TABLE CUSTOMERS
(

    customer_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name          VARCHAR(255),
    company_email         VARCHAR(255),
    tax_id                VARCHAR(20),
    billing_address_line  TEXT,
    billing_postal_code   VARCHAR(10),
    billing_city          VARCHAR(100),

    shipping_address_line TEXT,
    shipping_postal_code  VARCHAR(10),
    shipping_city         VARCHAR(100)
);