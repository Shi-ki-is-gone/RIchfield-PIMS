CREATE DATABASE IF NOT EXISTS healthfirst_pims;
USE healthfirst_pims;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('Admin','Cashier') NOT NULL,
    full_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(30),
    email VARCHAR(100),
    address VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS medicines (
    medicine_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    company VARCHAR(100),
    medicine_type VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 10,
    expiry_date DATE,
    supplier_id INT NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
);

CREATE TABLE IF NOT EXISTS sales (
    sale_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    user_id INT,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS sale_items (
    sale_item_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    medicine_id INT NOT NULL,
    quantity_sold INT NOT NULL,
    price_at_sale DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id),
    FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
);

INSERT IGNORE INTO users(username,password,role,full_name)
VALUES
('admin','admin123','Admin','System Administrator'),
('cashier','cash123','Cashier','Front Counter Cashier');

INSERT IGNORE INTO suppliers(name,contact_person,phone,email,address)
VALUES
('MediSupply SA','Thabo Mokoena','012 555 0101','info@medisupply.co.za','Pretoria, Gauteng'),
('Pharma Distributors','Lerato Dlamini','011 555 0202','sales@pharmadist.co.za','Johannesburg, Gauteng'),
('HealthMed Wholesale','Sipho Ndlovu','031 555 0303','sales@healthmed.co.za','Durban, KwaZulu-Natal');

INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id)
SELECT 'Paracetamol 500mg','HealthMed','Tablet',25.00,100,20,'2027-12-31',supplier_id FROM suppliers WHERE name='MediSupply SA' LIMIT 1;
INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id)
SELECT 'Amoxicillin 500mg','PharmaCare','Capsule',65.00,50,10,'2027-10-31',supplier_id FROM suppliers WHERE name='Pharma Distributors' LIMIT 1;
INSERT INTO medicines(name,company,medicine_type,price,quantity_in_stock,reorder_level,expiry_date,supplier_id)
SELECT 'Cough Syrup 100ml','HealthMed','Syrup',48.50,35,8,'2027-08-30',supplier_id FROM suppliers WHERE name='HealthMed Wholesale' LIMIT 1;
