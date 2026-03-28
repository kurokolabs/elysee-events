-- MySQL Schema für Elysee Event GmbH Portal

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    active          TINYINT(1) NOT NULL DEFAULT 1,
    force_pw_change TINYINT(1) NOT NULL DEFAULT 0,
    two_fa_enabled  TINYINT(1) NOT NULL DEFAULT 1,
    two_fa_code     VARCHAR(72),
    two_fa_expires  VARCHAR(30),
    two_fa_attempts INT NOT NULL DEFAULT 0,
    email_verification_token VARCHAR(64),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login      DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NULL UNIQUE,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    company     VARCHAR(200),
    phone       VARCHAR(30),
    address     VARCHAR(300),
    postal_code VARCHAR(10),
    city        VARCHAR(100),
    notes       TEXT,
    email       VARCHAR(255),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bookings (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT NOT NULL,
    booking_type     VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ANFRAGE',
    event_date       DATE,
    event_time_slot  VARCHAR(50),
    guest_count      INT,
    budget           DECIMAL(12,2),
    menu_selection   TEXT,
    special_requests TEXT,
    admin_notes      TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(30),
    entity_id   BIGINT,
    details     TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id      BIGINT NOT NULL,
    customer_id     BIGINT NOT NULL,
    invoice_number  VARCHAR(20) NOT NULL UNIQUE,
    amount          DECIMAL(12,2) NOT NULL,
    tax_rate        DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    tax_amount      DECIMAL(12,2) NOT NULL,
    total           DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OFFEN',
    due_date        DATE,
    paid_date       DATE,
    notes           TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoice_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id  BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity    DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2) NOT NULL,
    total       DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quotes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    booking_id      BIGINT,
    quote_number    VARCHAR(20) NOT NULL UNIQUE,
    amount          DECIMAL(12,2) NOT NULL,
    tax_rate        DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    tax_amount      DECIMAL(12,2) NOT NULL,
    total           DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'OFFEN',
    valid_until     DATE,
    notes           TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quote_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_id    BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity    DECIMAL(10,2) NOT NULL DEFAULT 1,
    unit_price  DECIMAL(12,2) NOT NULL,
    total       DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS documents (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    booking_id  BIGINT,
    uploaded_by VARCHAR(20) NOT NULL,
    file_name   VARCHAR(500) NOT NULL,
    file_path   VARCHAR(1000) NOT NULL,
    file_size   BIGINT,
    file_type   VARCHAR(20),
    description TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(200),
    active      TINYINT(1) NOT NULL DEFAULT 1,
    token       VARCHAR(64) NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS weekly_menus (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    week_start  DATE NOT NULL,
    week_end    DATE NOT NULL,
    monday      TEXT,
    tuesday     TEXT,
    wednesday   TEXT,
    thursday    TEXT,
    friday      TEXT,
    notes       TEXT,
    sent        TINYINT(1) NOT NULL DEFAULT 0,
    sent_at     DATETIME,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rechnungs-Erweiterungen: Leistungszeitraum, Intro-Text, gemischte Steuersätze
ALTER TABLE invoices ADD COLUMN service_period_from DATE;
ALTER TABLE invoices ADD COLUMN service_period_to DATE;
ALTER TABLE invoices ADD COLUMN intro_text TEXT;
ALTER TABLE invoices ADD COLUMN tax_amount_7 DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN tax_amount_19 DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE invoice_items ADD COLUMN tax_type VARCHAR(20) NOT NULL DEFAULT 'GETRAENKE';

-- Unabhängige Rechnungen (ohne Portalkundenbindung)
ALTER TABLE invoices MODIFY COLUMN customer_id BIGINT NULL;
ALTER TABLE invoices MODIFY COLUMN booking_id BIGINT NULL;
ALTER TABLE invoices ADD COLUMN recipient_name VARCHAR(200);
ALTER TABLE invoices ADD COLUMN recipient_company VARCHAR(200);
ALTER TABLE invoices ADD COLUMN recipient_address VARCHAR(300);
ALTER TABLE invoices ADD COLUMN recipient_postal_code VARCHAR(10);
ALTER TABLE invoices ADD COLUMN recipient_city VARCHAR(100);
ALTER TABLE invoices ADD COLUMN recipient_email VARCHAR(255);

-- Angebote flexibel: Leistungszeitraum, Intro-Text, gemischte Steuersätze, Standalone
ALTER TABLE quotes ADD COLUMN service_period_from DATE;
ALTER TABLE quotes ADD COLUMN service_period_to DATE;
ALTER TABLE quotes ADD COLUMN intro_text TEXT;
ALTER TABLE quotes ADD COLUMN tax_amount_7 DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE quotes ADD COLUMN tax_amount_19 DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE quotes MODIFY COLUMN customer_id BIGINT NULL;
ALTER TABLE quotes ADD COLUMN recipient_name VARCHAR(200);
ALTER TABLE quotes ADD COLUMN recipient_company VARCHAR(200);
ALTER TABLE quotes ADD COLUMN recipient_address VARCHAR(300);
ALTER TABLE quotes ADD COLUMN recipient_postal_code VARCHAR(10);
ALTER TABLE quotes ADD COLUMN recipient_city VARCHAR(100);
ALTER TABLE quotes ADD COLUMN recipient_email VARCHAR(255);
ALTER TABLE quote_items ADD COLUMN tax_type VARCHAR(20) NOT NULL DEFAULT 'GETRAENKE';

-- Performance-Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_quotes_number ON quotes(quote_number);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_documents_customer ON documents(customer_id);

-- Wizard-Felder (Anfrage-Formular v2)
ALTER TABLE bookings ADD COLUMN delivery_address TEXT;
ALTER TABLE bookings ADD COLUMN catering_package VARCHAR(20);
ALTER TABLE bookings ADD COLUMN food_option VARCHAR(20);
ALTER TABLE bookings ADD COLUMN food_sub_option VARCHAR(20);
ALTER TABLE bookings ADD COLUMN cuisine_style VARCHAR(30);
