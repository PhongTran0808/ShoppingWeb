CREATE DATABASE IF NOT EXISTS catalog_db;
CREATE DATABASE IF NOT EXISTS cart_db;
CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS keycloak_db;

GRANT ALL PRIVILEGES ON catalog_db.* TO 'ecomuser'@'%';
GRANT ALL PRIVILEGES ON cart_db.* TO 'ecomuser'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'ecomuser'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'ecomuser'@'%';
GRANT ALL PRIVILEGES ON keycloak_db.* TO 'ecomuser'@'%';

FLUSH PRIVILEGES;
