#!/bin/bash
set -euo pipefail

# MySQL Backup Script — Elysee Event GmbH Portal
# Verwendung: ./backup-mysql.sh
# Cron:       0 2 * * * cd /opt/elysee-portal && ./backup-mysql.sh >> /var/log/elysee-backup.log 2>&1

BACKUP_DIR="./backups"
KEEP=30
TIMESTAMP=$(date +%Y-%m-%d_%H%M%S)

# Lade .env wenn vorhanden
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-elysee_portal}"
DB_USER="${DB_USER:-elysee_portal}"
DB_PASSWORD="${DB_PASSWORD:-}"

mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starte Backup..."

# MySQL Dump
DUMP_FILE="$BACKUP_DIR/db_${TIMESTAMP}.sql.gz"
mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" \
  --single-transaction --routines --triggers \
  "$DB_NAME" | gzip > "$DUMP_FILE"
echo "  Datenbank: $DUMP_FILE ($(du -h "$DUMP_FILE" | cut -f1))"

# Uploads sichern
if [ -d "./uploads" ] && [ "$(ls -A ./uploads 2>/dev/null)" ]; then
  UPLOAD_FILE="$BACKUP_DIR/uploads_${TIMESTAMP}.tar.gz"
  tar -czf "$UPLOAD_FILE" ./uploads 2>/dev/null
  echo "  Uploads: $UPLOAD_FILE ($(du -h "$UPLOAD_FILE" | cut -f1))"
fi

# Alte Backups aufräumen (behalte die letzten $KEEP)
cd "$BACKUP_DIR"
ls -t db_*.sql.gz 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm --
ls -t uploads_*.tar.gz 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm --
cd ..

echo "[$(date)] Backup abgeschlossen."
