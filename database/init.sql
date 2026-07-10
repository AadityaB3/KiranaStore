-- Smart Kirana Store - PostgreSQL Schema Initialization Script

-- Drop tables if they exist to allow clean re-runs
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS delivery_assignments CASCADE;
DROP TABLE IF EXISTS store_settings CASCADE;
DROP TABLE IF EXISTS otp_verifications CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS wishlist CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS cart CASCADE;
DROP TABLE IF EXISTS inventory CASCADE;
DROP TABLE IF EXISTS product_images CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS delivery_partners CASCADE;
DROP TABLE IF EXISTS admins CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS coupons CASCADE;

-- 1. Roles Table
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

-- 2. Users Table (Core Auth Table)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. User Roles Mapping
CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Customers Extension Table
CREATE TABLE customers (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    wallet_balance DECIMAL(10, 2) DEFAULT 0.00,
    preferred_language VARCHAR(10) DEFAULT 'en'
);

-- 5. Admins Extension Table
CREATE TABLE admins (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    can_manage_inventory BOOLEAN DEFAULT TRUE,
    can_manage_users BOOLEAN DEFAULT TRUE
);

-- 6. Delivery Partners Extension Table
CREATE TABLE delivery_partners (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    vehicle_number VARCHAR(20) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    rating DECIMAL(2, 1) DEFAULT 5.0
);

-- 7. Addresses Table
CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    street_address TEXT NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Categories Table
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    icon_url TEXT
);

-- 9. Products Table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id INT REFERENCES categories(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL, -- e.g. "1 kg", "500 ml"
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 10. Product Images Table
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE
);

-- 11. Inventory Table
CREATE TABLE inventory (
    product_id BIGINT PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    stock_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. Cart Table
CREATE TABLE cart (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 13. Cart Items Table
CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT REFERENCES cart(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

-- 14. Wishlist Table
CREATE TABLE wishlist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, product_id)
);

-- 15. Coupons Table
CREATE TABLE coupons (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    discount_percentage INT NOT NULL CHECK (discount_percentage BETWEEN 1 AND 100),
    max_discount_amount DECIMAL(10,2),
    min_order_amount DECIMAL(10,2) DEFAULT 0.00,
    expiry_date TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- 16. Orders Table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    address_id BIGINT REFERENCES addresses(id) ON DELETE SET NULL,
    coupon_id INT REFERENCES coupons(id) ON DELETE SET NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    payable_amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL, -- COD, WALLET
    status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, ACCEPTED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 17. Order Items Table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    total_price DECIMAL(10, 2) NOT NULL
);

-- 18. Payments Table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    payment_status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, REFUNDED, FAILED
    transaction_id VARCHAR(100),
    amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 19. Notifications Table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 20. OTP Verifications Table
CREATE TABLE otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    otp_code VARCHAR(4) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL
);

-- 21. Store Settings Table
CREATE TABLE store_settings (
    id SERIAL PRIMARY KEY,
    is_open BOOLEAN DEFAULT TRUE,
    store_announcement TEXT,
    minimum_order_amount DECIMAL(10, 2) DEFAULT 0.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 22. Delivery Assignments Table
CREATE TABLE delivery_assignments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE REFERENCES orders(id) ON DELETE CASCADE,
    rider_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 23. Reviews Table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 24. Audit Logs Table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== SAMPLE SEED DATA ====================

-- Insert Roles
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER'), ('ROLE_OWNER'), ('ROLE_DELIVERY');

-- Insert Sample Store Owner
INSERT INTO users (username, email, password_hash, phone, name)
VALUES ('admin', 'admin@kirana.com', '$2a$12$6Nl897Bw2Qx9vUuTqT98SOnCgEcl3YmPjZqY8jRcoW1p8k7C3FmSu', '9999999999', 'Anand Kumar'); -- admin123
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);
INSERT INTO admins (user_id) VALUES (1);

-- Insert Sample Customer
INSERT INTO users (username, email, password_hash, phone, name)
VALUES ('customer', 'customer@gmail.com', '$2a$12$6Nl897Bw2Qx9vUuTqT98SOnCgEcl3YmPjZqY8jRcoW1p8k7C3FmSu', '9876543210', 'Rahul Sharma');
INSERT INTO user_roles (user_id, role_id) VALUES (2, 1);
INSERT INTO customers (user_id, wallet_balance) VALUES (2, 1000.00);

-- Insert Sample Delivery Partner
INSERT INTO users (username, email, password_hash, phone, name)
VALUES ('rider', 'rider@delivery.com', '$2a$12$6Nl897Bw2Qx9vUuTqT98SOnCgEcl3YmPjZqY8jRcoW1p8k7C3FmSu', '8888888888', 'Vijay Yadav');
INSERT INTO user_roles (user_id, role_id) VALUES (3, 3);
INSERT INTO delivery_partners (user_id, vehicle_number) VALUES (3, 'DL-3C-AB-1234');

-- Insert Sample Address
INSERT INTO addresses (user_id, street_address, city, state, pincode, is_default)
VALUES (2, 'Flat 405, Green Meadows, Sector 15', 'Noida', 'Uttar Pradesh', '201301', true);

-- Insert Categories
INSERT INTO categories (name, description, icon_url) VALUES 
('Groceries', 'Daily grocery staples, rice, dal, and flour', 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=100'),
('Vegetables', 'Fresh seasonal vegetables directly from farms', 'https://images.unsplash.com/photo-1597362925123-77861d3fbac7?auto=format&fit=crop&q=80&w=100'),
('Dairy & Eggs', 'Milk, butter, paneer, and farm fresh eggs', 'https://images.unsplash.com/photo-1528498033373-3c6c08e93d79?auto=format&fit=crop&q=80&w=100'),
('Snacks', 'Chocolates, chips, biscuits, and soft drinks', 'https://images.unsplash.com/photo-1599490659213-e2b9527bb087?auto=format&fit=crop&q=80&w=100');

-- Insert Products
INSERT INTO products (category_id, name, description, price, unit) VALUES
(1, 'Premium Basmati Rice', 'Long grain basmati rice with aromatic flavor', 110.00, '1 kg'),
(1, 'Ashirvaad Shudh Chakki Atta', '100% whole wheat flour with dietary fiber', 450.00, '10 kg'),
(2, 'Fresh Potatoes', 'Farm-fresh organic potatoes', 30.00, '1 kg'),
(2, 'Hybrid Tomatoes', 'Juicy red tomatoes rich in nutrients', 40.00, '1 kg'),
(3, 'Amul Taaza Toned Milk', 'Homogenized toned milk', 33.00, '500 ml'),
(3, 'Fresh Paneer', 'Soft and creamy block cottage cheese', 90.00, '200 g'),
(4, 'Lay''s Classic Salted Chips', 'Crunchy potato chips seasoned with salt', 20.00, '50 g');

-- Insert Inventory
INSERT INTO inventory (product_id, stock_quantity, low_stock_threshold) VALUES
(1, 50, 5),
(2, 30, 3),
(3, 100, 10),
(4, 4, 10), -- Low stock alert product
(5, 60, 8),
(6, 15, 4),
(7, 80, 10);

-- Insert Store Settings
INSERT INTO store_settings (is_open, store_announcement, minimum_order_amount)
VALUES (true, 'Welcome to Smart Kirana! Monsoon special offers are now active!', 100.00);

-- Create Indexes for Query Optimization
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_addresses_user ON addresses(user_id);
CREATE INDEX idx_delivery_assignments_rider ON delivery_assignments(rider_id);
