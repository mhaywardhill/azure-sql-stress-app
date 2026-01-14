#!/bin/bash
# Environment variable configuration for Azure SQL Stress App
# 
# Usage: 
#   1. Edit this file with your values
#   2. Run: source set-env.sh
#   3. Then run: mvn spring-boot:run

# REQUIRED: Replace these with your actual values
export DB_URL="jdbc:sqlserver://YOUR-SERVER.database.windows.net:1433;database=YOUR-DB;encrypt=true;trustServerCertificate=false;loginTimeout=30;applicationName=AzureSqlStressApp;authentication=ActiveDirectoryPassword"
export DB_USER="your-username@yourdomain.com"
export DB_PASSWORD="your-password"

# Verify settings
echo "✅ Environment variables set:"
echo "   DB_URL: ${DB_URL:0:50}..."
echo "   DB_USER: $DB_USER"
echo "   DB_PASSWORD: $([ -n "$DB_PASSWORD" ] && echo '***' || echo '(not set)')"
echo ""
echo "Now run: mvn spring-boot:run"
