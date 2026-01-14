# Azure SQL Stress App

A lightweight Spring Boot web app to **stress test Azure SQL** by repeatedly executing a SQL statement with configurable **iterations**, **concurrency**, **timeouts**, and **result modes**. Enter a SQL statement, choose how many times to run it (and how many workers), and view aggregated timings, successes/failures, and optional sample results in the browser.

> ⚠️ **Use responsibly.** Only run against **non-production** databases or approved test environments. This tool can generate significant load and may incur costs or trigger throttling.

## Features
- Web form to input **SQL**, **iterations**, **concurrency**, **delay between iterations**, and **timeout**
- Result modes: **No rows**, **Scalar (first column of first row)**, or **Rows (first N rows)**
- Aggregated metrics: total duration, success/error counts, avg, p50, p95, p99 latency
- Sample output rows (bounded), and error samples
- **Configurable Hikari connection pool settings** via environment variables (pool size, timeouts, etc.)
- Ready to run in **GitHub Codespaces** or **Linux VM**

## Quick Start (GitHub Codespaces)
1. **Create a new GitHub repository** and add these files (or push this folder). If you’re viewing this locally, push it:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Azure SQL Stress App"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. **Open in Codespaces**: In your GitHub repo → **Code** → **Codespaces** → **Create codespace on main**.
3. **Configure connection settings** using Codespaces **Secrets** or a local `.env` file.
   - Required environment variables:
     - `DB_URL` – e.g. `jdbc:sqlserver://<server>.database.windows.net:1433;database=<db>;encrypt=true;loginTimeout=30;applicationName=AzureSqlStressApp;`
     - `DB_USER` – username (for SQL Auth **or** Entra ID user UPN)
     - `DB_PASSWORD` – password

   **Entra ID (Microsoft Entra) password auth**: append `authentication=ActiveDirectoryPassword` to the JDBC URL, e.g.
   ```
   jdbc:sqlserver://<server>.database.windows.net:1433;database=<db>;encrypt=true;loginTimeout=30;applicationName=AzureSqlStressApp;authentication=ActiveDirectoryPassword;
   ```
   > Note: If a Conditional Access policy enforces MFA, password-based flows will be blocked.

4. **Run the app**:
   ```bash
   mvn spring-boot:run
   ```
   Codespaces will forward **port 8080**. Open the forwarded URL and use the UI.

## Running on a Linux VM

### Option 1: Direct Access (requires firewall configuration)
1. **SSH into your Linux VM** and clone/copy the project
2. **Set environment variables**:
   ```bash
   export DB_URL="jdbc:sqlserver://your-server.database.windows.net:1433;database=your-db;encrypt=true;authentication=ActiveDirectoryPassword"
   export DB_USER="username@yourdomain.com"
   export DB_PASSWORD="username-password"
   ```
3. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```
4. **Configure firewall** to allow inbound traffic on port 8080:
   - Azure: Add NSG rule for port 8080
   - Linux: `sudo ufw allow 8080` (if using UFW)
5. **Access from your browser**: `http://<VM-IP-ADDRESS>:8080`

### Option 2: SSH Port Forwarding (recommended for secure access)
1. **On your laptop**, create an SSH tunnel to the VM:
   ```bash
   ssh -i /path/to/your-key.pem -L 8080:localhost:8080 user@<VM-IP>
   ```
2. **In the SSH session**, run the application:
   ```bash
   mvn spring-boot:run
   ```
3. **On your laptop's browser**, navigate to: `http://localhost:8080`

The SSH tunnel securely forwards port 8080 from the VM to your local machine without exposing it to the internet.

## Local Dev (optional)
- JDK 17+
- Maven 3.8+
- Set `DB_URL`, `DB_USER`, `DB_PASSWORD` in your shell (or `.env` loaded by your IDE).

## Safety & Tips
- Prefer **READONLY** accounts when testing SELECT workloads.
- For write tests, consider **idempotent** statements or use temp/test tables.
- Start small (e.g., 50 iterations, concurrency 5), then scale up.
- Monitor in Azure (Query Store, Wait Stats, Perf Insights) during tests.

## Troubleshooting Connection Issues

### Connection Failed Errors

If you see `total=0, active=0, idle=0` in the connection pool errors, the app cannot establish any database connections.

**Common Issues:**

1. **Missing Environment Variables**
   ```bash
   # Check if variables are set
   echo $DB_URL
   echo $DB_USER
   # DB_PASSWORD should be set but don't echo it
   
   # Set them if missing
   export DB_URL="jdbc:sqlserver://your-server.database.windows.net:1433;database=your-db;encrypt=true;authentication=ActiveDirectoryPassword"
   export DB_USER="youusernamername@yourdomain.com"
   export DB_PASSWORD="username-password"
   ```

2. **Azure SQL Firewall Rules**
   ```bash
   # Get your Codespaces IP
   curl -s https://api.ipify.org
   
   # Add to Azure SQL firewall via Azure CLI
   az sql server firewall-rule create \
     --resource-group YOUR_RG \
     --server YOUR_SERVER \
     --name AllowCodespaces \
     --start-ip-address YOUR_IP \
     --end-ip-address YOUR_IP
   
   # Or enable "Allow Azure Services"
   az sql server firewall-rule create \
     --resource-group YOUR_RG \
     --server YOUR_SERVER \
     --name AllowAzureServices \
     --start-ip-address 0.0.0.0 \
     --end-ip-address 0.0.0.0
   ```

3. **Authentication Issues**
   - **SQL Auth**: Use `DB_USER=sqluser` and omit `authentication=` from JDBC URL
   - **Azure AD**: Use `DB_USER=username@domain.com` and add `authentication=ActiveDirectoryPassword` to JDBC URL
   - **MFA Required**: Password auth won't work with MFA. Use service principal or managed identity instead.

4. **Test Connectivity**
   ```bash
   # Test TCP connection to Azure SQL Gateway service
   nc -zv your-server.database.windows.net 1433
   
   # Or with telnet
   telnet your-server.database.windows.net 1433
   ```
   
   Learn more about [Azure SQL Database connectivity architecture](https://learn.microsoft.com/en-us/azure/azure-sql/database/connectivity-architecture?view=azuresql).

### Debug Logging

The app now includes detailed debug logging. Check the console output when starting the app for:
- HikariCP pool initialization messages
- JDBC driver connection attempts
- Detailed error messages with SQL State codes

Look for lines like:
```
HikariPool-1 - Starting...
HikariPool-1 - Exception during pool initialization
Login failed for user 'xxx'
```

### View Connection Status in UI

The web interface now shows real-time connection status:
- ✅ **Green box**: Successfully connected with server/database details
- ❌ **Red box**: Connection failed with error details and troubleshooting hints
- Click **"🔄 Retry Connection"** button to test again after fixing issues

## License
MIT
