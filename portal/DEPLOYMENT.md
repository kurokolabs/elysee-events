# Deployment Guide — Elysee Event GmbH Portal

## Voraussetzungen

- Java 21 (Eclipse Temurin)
- MySQL 8.0+ oder SQLite (Entwicklung)
- Apache 2.4+ mit mod_proxy (optional, für Reverse Proxy)
- Docker + Docker Compose (optional)

---

## 1. Schnellstart (Entwicklung, SQLite)

```bash
cd portal
cp .env.example .env
# .env anpassen (ADMIN_PASSWORD ändern!)
./mvnw clean package -DskipTests
java -jar target/portal-1.0.0.jar
# → http://localhost:8080
```

---

## 2. Produktion mit Docker Compose (empfohlen)

### 2.1 Vorbereitung
```bash
cd /opt/elysee-portal
cp portal/.env.example portal/.env
```

### 2.2 .env bearbeiten
```env
ADMIN_PASSWORD=<sicheres-passwort>
DB_PASSWORD=<sicheres-db-passwort>
DB_ROOT_PASSWORD=<root-passwort>
COMPANY_NAME=Elysee Event GmbH
COMPANY_IBAN=<echte-IBAN>
# ... alle Firmendaten eintragen
```

### 2.3 JAR bauen und starten
```bash
cd portal && ./mvnw clean package -DskipTests && cd ..
docker-compose up -d
docker-compose logs -f portal
```

### 2.4 Status prüfen
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## 3. Produktion manuell (Hostinger VPS)

### 3.1 MySQL einrichten
```bash
sudo apt install mysql-server
sudo mysql_secure_installation

sudo mysql -e "
CREATE DATABASE elysee_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'elysee_portal'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON elysee_portal.* TO 'elysee_portal'@'localhost';
FLUSH PRIVILEGES;
"
```

### 3.2 Java 21 installieren
```bash
sudo apt install -y wget
wget https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1+12/OpenJDK21U-jre_x64_linux_hotspot_21.0.1_12.tar.gz
sudo tar -xzf OpenJDK21U-*.tar.gz -C /opt/
sudo ln -s /opt/jdk-21.0.1+12-jre/bin/java /usr/local/bin/java
```

### 3.3 Anwendung deployen
```bash
sudo mkdir -p /opt/elysee-portal/{uploads,logs,backups}
sudo useradd -r -s /bin/false elysee
sudo cp portal/target/portal-1.0.0.jar /opt/elysee-portal/
sudo cp portal/.env /opt/elysee-portal/
sudo cp -r elysee-events /opt/elysee-portal/
sudo chown -R elysee:elysee /opt/elysee-portal
```

### 3.4 Systemd Service
```bash
sudo tee /etc/systemd/system/elysee-portal.service << 'EOF'
[Unit]
Description=Elysee Events Portal
After=network.target mysql.service

[Service]
User=elysee
WorkingDirectory=/opt/elysee-portal
EnvironmentFile=/opt/elysee-portal/.env
ExecStart=/usr/local/bin/java -Xmx512m -Xms256m -jar portal-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable elysee-portal
sudo systemctl start elysee-portal
```

---

## 4. Apache Reverse Proxy + SSL

```bash
sudo a2enmod proxy proxy_http ssl rewrite headers
```

```apache
# /etc/apache2/sites-available/elysee-events.conf
<VirtualHost *:80>
    ServerName www.elysee-events.de
    RewriteEngine On
    RewriteRule ^(.*)$ https://%{HTTP_HOST}$1 [R=301,L]
</VirtualHost>

<VirtualHost *:443>
    ServerName www.elysee-events.de
    DocumentRoot /opt/elysee-portal/elysee-events

    SSLEngine on
    SSLCertificateFile /etc/letsencrypt/live/www.elysee-events.de/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/www.elysee-events.de/privkey.pem

    <Directory /opt/elysee-portal/elysee-events>
        AllowOverride All
        Require all granted
    </Directory>

    ProxyPreserveHost On
    ProxyPass /portal http://localhost:8080/portal
    ProxyPassReverse /portal http://localhost:8080/portal
    ProxyPass /actuator http://localhost:8080/actuator
    ProxyPassReverse /actuator http://localhost:8080/actuator

    Header always set X-Frame-Options "DENY"
    Header always set X-Content-Type-Options "nosniff"
    Header always set Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
</VirtualHost>
```

```bash
sudo certbot --apache -d www.elysee-events.de
```

---

## 5. Backup

```bash
# MySQL (Produktion)
chmod +x backup-mysql.sh
crontab -e
0 2 * * * cd /opt/elysee-portal && ./backup-mysql.sh >> /var/log/elysee-backup.log 2>&1
```

---

## 6. Monitoring

```bash
# Health Check (alle 5 Min, Restart bei Fehler)
*/5 * * * * curl -sf http://localhost:8080/actuator/health > /dev/null || systemctl restart elysee-portal

# Logs
tail -f /opt/elysee-portal/logs/portal.log
grep ERROR /opt/elysee-portal/logs/portal.log | tail -20
```

---

## 7. Update / Rollback

```bash
# Update
sudo cp /opt/elysee-portal/portal-1.0.0.jar /opt/elysee-portal/portal-1.0.0.jar.bak
sudo cp target/portal-1.0.0.jar /opt/elysee-portal/
sudo systemctl restart elysee-portal

# Rollback
sudo cp /opt/elysee-portal/portal-1.0.0.jar.bak /opt/elysee-portal/portal-1.0.0.jar
sudo systemctl restart elysee-portal
```

---

## 8. Firewall

```bash
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw deny 3306/tcp   # MySQL nur lokal
sudo ufw deny 8080/tcp   # Portal nur über Apache
sudo ufw enable
```

---

## Checkliste vor Go-Live

- [ ] .env mit echten Werten (Passwörter, IBAN, USt-IdNr.)
- [ ] MySQL-Datenbank erstellt und Schema importiert
- [ ] Admin-Passwort geändert (nicht Admin2024!)
- [ ] SSL-Zertifikat (Let's Encrypt)
- [ ] Firewall (nur 22, 80, 443 offen)
- [ ] Backup-Cronjob eingerichtet
- [ ] Health-Check: `/actuator/health` → `{"status":"UP"}`
- [ ] Logs: `/opt/elysee-portal/logs/portal.log`
- [ ] Upload-Ordner mit Berechtigungen
- [ ] Systemd startet bei Reboot
- [ ] https://www.elysee-events.de erreichbar
- [ ] https://www.elysee-events.de/portal/login erreichbar
