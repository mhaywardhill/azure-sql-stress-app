# Azure SQL Stress App (Codespaces-ready)

A lightweight Spring Boot web app to **stress test Azure SQL** by repeatedly executing a SQL statement with configurable **iterations**, **concurrency**, **timeouts**, and **result modes**. Enter a SQL statement, choose how many times to run it (and how many workers), and view aggregated timings, successes/failures, and optional sample results in the browser.

> ⚠️ **Use responsibly.** Only run against **non-production** databases or approved test environments. This tool can generate significant load and may incur costs or trigger throttling.

## Features
- Web form to input **SQL**, **iterations**, **concurrency**, **delay between iterations**, and **timeout**
- Result modes: **No rows**, **Scalar (first column of first row)**, or **Rows (first N rows)**
- Aggregated metrics: total duration, success/error counts, avg, p50, p95, p99 latency
- Sample output rows (bounded), and error samples
- Ready to run in **GitHub Codespaces**

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
     - `DB_URL` – e.g. `jdbc:sqlserver://<server>.database.windows.net:1433;database=<db>;encrypt=true;loginTimeout=30;`
     - `DB_USER` – username (for SQL Auth **or** Entra ID user UPN)
     - `DB_PASSWORD` – password

   **Entra ID (Microsoft Entra) password auth**: append `authentication=ActiveDirectoryPassword` to the JDBC URL, e.g.
   ```
   jdbc:sqlserver://<server>.database.windows.net:1433;database=<db>;encrypt=true;loginTimeout=30;authentication=ActiveDirectoryPassword;
   ```
   > Note: If a Conditional Access policy enforces MFA, password-based flows will be blocked.

4. **Run the app**:
   ```bash
   mvn spring-boot:run
   ```
   Codespaces will forward **port 8080**. Open the forwarded URL and use the UI.

## Local Dev (optional)
- JDK 17+
- Maven 3.8+
- Set `DB_URL`, `DB_USER`, `DB_PASSWORD` in your shell (or `.env` loaded by your IDE).

## Safety & Tips
- Prefer **READONLY** accounts when testing SELECT workloads.
- For write tests, consider **idempotent** statements or use temp/test tables.
- Start small (e.g., 50 iterations, concurrency 5), then scale up.
- Monitor in Azure (Query Store, Wait Stats, Perf Insights) during tests.

## License
MIT
