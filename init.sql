CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS employees (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    salary DOUBLE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    last_modified_by VARCHAR(255),
    last_modified_at VARCHAR(64)
);

-- Insert default admin user (password: 'password')
-- Hash generated via BCrypt
-- Insert default admin user (password: 'password123')
INSERT IGNORE INTO users (username, password_hash) VALUES ('admin', '$2a$10$0XNh9hN/WBa56oEAauPGee/zse/Cm60i.GUeA36cgTcj91TXb8D1y');

-- Insert sample employee
INSERT IGNORE INTO employees (id, name, department, salary, active, last_modified_by, last_modified_at) 
VALUES ('123e4567-e89b-12d3-a456-426614174000', 'John Doe', 'Engineering', 75000, 1, 'system', '2023-01-01T12:00:00');
