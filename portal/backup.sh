#!/bin/bash
# Elysee Events Portal - Database Backup Script
# Usage: ./backup.sh
# Cron:  0 2 * * * cd /path/to/portal && ./backup.sh

set -euo pipefail

BACKUP_DIR="./backups"
DB_FILE="./portal.db"
KEEP=30

# Verify database file exists
if [ ! -f "$DB_FILE" ]; then
    echo "[ERROR] Database file not found: $DB_FILE"
    exit 1
fi

# Create backup directory if needed
if [ ! -d "$BACKUP_DIR" ]; then
    mkdir -p "$BACKUP_DIR"
    echo "[INFO] Created backup directory: $BACKUP_DIR"
fi

# Generate timestamped filename
TIMESTAMP=$(date +"%Y-%m-%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/portal_${TIMESTAMP}.db"

# Copy database
cp "$DB_FILE" "$BACKUP_FILE"
echo "[OK] Backup created: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

# Backup uploads
if [ -d "./uploads" ]; then
  tar -czf "$BACKUP_DIR/uploads_$TIMESTAMP.tar.gz" ./uploads 2>/dev/null
  echo "[OK] Uploads backed up: $BACKUP_DIR/uploads_$TIMESTAMP.tar.gz"
fi

# Remove old backups beyond retention limit
BACKUP_COUNT=$(ls -1t "$BACKUP_DIR"/portal_*.db 2>/dev/null | wc -l)
if [ "$BACKUP_COUNT" -gt "$KEEP" ]; then
    REMOVE_COUNT=$((BACKUP_COUNT - KEEP))
    ls -1t "$BACKUP_DIR"/portal_*.db | tail -n "$REMOVE_COUNT" | while read -r OLD; do
        rm "$OLD"
        echo "[CLEANUP] Removed old backup: $OLD"
    done
    echo "[INFO] Kept $KEEP backups, removed $REMOVE_COUNT"
else
    echo "[INFO] $BACKUP_COUNT backup(s) stored (limit: $KEEP)"
fi
