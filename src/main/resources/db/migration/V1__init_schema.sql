CREATE TABLE marketplace_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE businesses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    owner_user_id BIGINT NOT NULL REFERENCES marketplace_users(id)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    business_id BIGINT NOT NULL REFERENCES businesses(id),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_user_id BIGINT NOT NULL REFERENCES marketplace_users(id),
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12, 2) NOT NULL
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    simulated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES marketplace_users(id),
    type VARCHAR(80) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_products_business_id ON products(business_id);
CREATE INDEX idx_orders_customer_user_id ON orders(customer_user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
