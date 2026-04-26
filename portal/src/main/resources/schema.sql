CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK(role IN ('ADMIN','CUSTOMER')),
    active          INTEGER NOT NULL DEFAULT 1,
    force_pw_change INTEGER NOT NULL DEFAULT 0,
    two_fa_enabled  INTEGER NOT NULL DEFAULT 1,
    two_fa_code     TEXT,
    two_fa_expires  TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now')),
    last_login      TEXT
);

CREATE TABLE IF NOT EXISTS customers (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name  TEXT NOT NULL,
    last_name   TEXT NOT NULL,
    company     TEXT,
    phone       TEXT,
    address     TEXT,
    postal_code TEXT,
    city        TEXT,
    notes       TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS bookings (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id      INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    booking_type     TEXT NOT NULL CHECK(booking_type IN ('CATERING','HOCHZEIT','CORPORATE')),
    status           TEXT NOT NULL DEFAULT 'ANFRAGE'
                     CHECK(status IN ('ANFRAGE','BESTAETIGT','IN_PLANUNG','ABGESCHLOSSEN','STORNIERT')),
    event_date       TEXT,
    event_time_slot  TEXT,
    guest_count      INTEGER,
    budget           REAL,
    menu_selection   TEXT,
    special_requests TEXT,
    admin_notes      TEXT,
    created_at       TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at       TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER,
    action      TEXT NOT NULL,
    entity_type TEXT,
    entity_id   INTEGER,
    details     TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS invoices (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id      INTEGER NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    customer_id     INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    invoice_number  TEXT NOT NULL UNIQUE,
    amount          REAL NOT NULL,
    tax_rate        REAL NOT NULL DEFAULT 19.0,
    tax_amount      REAL NOT NULL,
    total           REAL NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OFFEN'
                    CHECK(status IN ('OFFEN','BEZAHLT','STORNIERT')),
    due_date        TEXT,
    paid_date       TEXT,
    notes           TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id  INTEGER NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity    REAL NOT NULL DEFAULT 1,
    unit_price  REAL NOT NULL,
    total       REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS quotes (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id     INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    booking_id      INTEGER REFERENCES bookings(id),
    quote_number    TEXT NOT NULL UNIQUE,
    amount          REAL NOT NULL,
    tax_rate        REAL NOT NULL DEFAULT 19.0,
    tax_amount      REAL NOT NULL,
    total           REAL NOT NULL,
    status          TEXT NOT NULL DEFAULT 'OFFEN'
                    CHECK(status IN ('OFFEN','ANGENOMMEN','ABGELEHNT','ABGELAUFEN')),
    valid_until     TEXT,
    notes           TEXT,
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS quote_items (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    quote_id    INTEGER NOT NULL REFERENCES quotes(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    quantity    REAL NOT NULL DEFAULT 1,
    unit_price  REAL NOT NULL,
    total       REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS documents (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    booking_id  INTEGER REFERENCES bookings(id),
    uploaded_by TEXT NOT NULL CHECK(uploaded_by IN ('ADMIN','CUSTOMER')),
    file_name   TEXT NOT NULL,
    file_path   TEXT NOT NULL,
    file_size   INTEGER,
    file_type   TEXT,
    description TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Performance-Indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_quotes_number ON quotes(quote_number);

CREATE TABLE IF NOT EXISTS newsletter_subscribers (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT NOT NULL UNIQUE,
    name        TEXT,
    active      INTEGER NOT NULL DEFAULT 1,
    token       TEXT NOT NULL,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS weekly_menus (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    week_start  TEXT NOT NULL,
    week_end    TEXT NOT NULL,
    monday      TEXT,
    tuesday     TEXT,
    wednesday   TEXT,
    thursday    TEXT,
    friday      TEXT,
    notes       TEXT,
    sent        INTEGER NOT NULL DEFAULT 0,
    sent_at     TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_newsletter_email ON newsletter_subscribers(email);
CREATE INDEX IF NOT EXISTS idx_newsletter_token ON newsletter_subscribers(token);
CREATE INDEX IF NOT EXISTS idx_quotes_customer ON quotes(customer_id);
CREATE INDEX IF NOT EXISTS idx_quote_items_quote ON quote_items(quote_id);
CREATE INDEX IF NOT EXISTS idx_documents_customer ON documents(customer_id);
CREATE INDEX IF NOT EXISTS idx_documents_booking ON documents(booking_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoices_booking ON invoices(booking_id);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_type ON bookings(booking_type);
CREATE INDEX IF NOT EXISTS idx_bookings_event_date ON bookings(event_date);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at);

-- Wizard-Felder (Anfrage-Formular v2)
ALTER TABLE bookings ADD COLUMN delivery_address TEXT;
ALTER TABLE bookings ADD COLUMN catering_package TEXT;
ALTER TABLE bookings ADD COLUMN food_option TEXT;
ALTER TABLE bookings ADD COLUMN food_sub_option TEXT;
ALTER TABLE bookings ADD COLUMN cuisine_style TEXT;

CREATE TABLE IF NOT EXISTS kantine_reservations (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id     INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    seat_count      INTEGER NOT NULL CHECK(seat_count > 0),
    reservation_date TEXT,
    status          TEXT NOT NULL DEFAULT 'OFFEN'
                    CHECK(status IN ('OFFEN','BESTAETIGT','STORNIERT')),
    created_at      TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_kantine_reservations_customer ON kantine_reservations(customer_id);
CREATE INDEX IF NOT EXISTS idx_kantine_reservations_date ON kantine_reservations(reservation_date);

ALTER TABLE kantine_reservations ADD COLUMN reservation_time TEXT;
