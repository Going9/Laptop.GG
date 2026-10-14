# LaptopGG 운영 Runbook

## Web App Setup

1. Copy `ops/env/laptopgg.env.example` to `/home/ubuntu/laptopgg/laptopgg.env` and fill DB credentials.
2. Copy `ops/systemd/laptopgg.service` to `/etc/systemd/system/laptopgg.service`.
3. Run `sudo systemctl daemon-reload && sudo systemctl enable laptopgg`.
4. Place the first jar under `/home/ubuntu/laptopgg/releases/<sha>/app-<sha>.jar`.
5. Link it with `ln -sfn /home/ubuntu/laptopgg/releases/<sha>/app-<sha>.jar /home/ubuntu/laptopgg/app.jar`.
6. Start with `sudo systemctl start laptopgg`.
7. Verify with `curl -fsS http://127.0.0.1:8080/actuator/health/readiness`.

`JAVA_OPTS` includes `-XX:TieredStopAtLevel=1` because the production app runs on a 1-core server and startup latency matters more than peak JIT throughput.
The web app uses Spring Boot graceful shutdown with a 20s shutdown phase timeout, while systemd keeps `TimeoutStopSec=30` to give the JVM enough time to drain in-flight requests before forced termination.

## Nginx Setup

1. Copy `ops/nginx/laptopgg-http.conf` to `/etc/nginx/conf.d/laptopgg-http.conf` and `laptopgg-{headers,proxy}.conf` to `/etc/nginx/snippets/`.
2. Copy `ops/nginx/laptopgg.conf` to `/etc/nginx/sites-available/laptopgg.conf` for initial setup. On existing TLS installations, retain Certbot's certificate/listen/redirect blocks and replace only the proxy blocks with the snippet includes.
3. Link it into `sites-enabled`.
4. Run `sudo nginx -t && sudo systemctl reload nginx`.
5. Confirm public pages are reachable.
6. Confirm `/actuator/*` and `/api/crawl/*` are not publicly reachable.

The deploy profile binds Spring to `127.0.0.1`; external requests must pass through Nginx.
Nginx accepts at most 16KiB request bodies and limits comment writes to 10/minute per visitor IP,
with a burst of 10. GET requests do not consume that limit. Only Cloudflare's published networks
may supply `CF-Connecting-IP`; other forwarding headers are overwritten or removed before proxying.
Refresh the trusted ranges against https://www.cloudflare.com/ips/ when Cloudflare changes them.
The proxy applies CSP, frame/content-type protections and a one-day HSTS policy. Run `nginx -t`
before reloading and check the public site through the CDN after changes.
Cloudflare injects its Web Analytics beacon at the edge. CSP explicitly allows its script and
collection origins from https://developers.cloudflare.com/web-analytics/faq/; local fixture pages
alone cannot expose an edge-injected script regression.

Proxy tests use a synthetic local backend and loopback listeners; they never submit to production.

## Log Management

The web application writes only to `systemd-journald`; do not add a separate Spring log file. The versioned journal policy retains logs for at most 14 days and 256MB, keeps 5GB of disk free, and rotates files daily or at 32MB.

1. Copy `ops/systemd/journald.conf.d/laptopgg.conf` to `/etc/systemd/journald.conf.d/laptopgg.conf`.
2. Run `sudo systemctl restart systemd-journald`.
3. Reload the web unit after copying `ops/systemd/laptopgg.service`: `sudo systemctl daemon-reload && sudo systemctl restart laptopgg`.
4. Nginx writes the site-specific files `/var/log/nginx/laptopgg.access.log` and `/var/log/nginx/laptopgg.error.log`. Ubuntu's `/etc/logrotate.d/nginx` already rotates every `/var/log/nginx/*.log` daily, keeps 14 copies, compresses old files, and reopens Nginx logs.

Useful checks:

```bash
journalctl -u laptopgg -f
journalctl -u laptopgg -p warning --since '1 hour ago'
journalctl --disk-usage
sudo tail -f /var/log/nginx/laptopgg.access.log
sudo tail -f /var/log/nginx/laptopgg.error.log
```

## Deploy Flow

CI runs representative UI checks (36 cases) and server/PostgreSQL tests in parallel. Extended Full (108 cases) is optional via the CI workflow_dispatch input full=true. Its deploy job needs both jobs to succeed, then calls the reusable Deploy Web workflow with the tested JAR artifact from the same run. PRs cannot deploy. Manual deploy uses `gh workflow run ci.yml --ref main`.

1. CI runs `test` and builds `:web-app:bootJar` once
2. Deploy downloads `web-release-<sha>`; it does not repeat tests or builds
3. upload the web jar to `/home/ubuntu/laptopgg/releases/<sha>/`
4. switch `/home/ubuntu/laptopgg/app.jar` symlink
5. restart `laptopgg`
6. check `/actuator/health/readiness`
7. rollback symlink to the previous jar if health fails
8. after a successful health check, prune old release directories while preserving the active jar, the previous rollback target, and the latest 5 releases

Deploy workflow runs are serialized. Do not cancel an in-progress deploy unless you are ready to check the remote symlink and service status manually.

Manual rollback:

```bash
cd /home/ubuntu/laptopgg
ls -lt releases
ln -sfn /home/ubuntu/laptopgg/releases/<previous-sha>/app-<previous-sha>.jar app.jar
sudo systemctl restart laptopgg
curl -fsS http://127.0.0.1:8080/actuator/health/readiness
```

Release retention:

```bash
cd /home/ubuntu/laptopgg
readlink -f app.jar
ls -lt releases | head
```

The deploy workflow keeps the current release, the previous rollback target, and the newest 5 release directories. If pruning fails, the deployed release stays active and cleanup can be repeated manually after checking the symlink target.

## Crawler Job

Production crawling is GitHub Actions only.

- Workflow profile: `postgres,crawler`
- DB access: SSH tunnel to PostgreSQL
- Duplicate prevention: GitHub Actions `concurrency` plus PostgreSQL advisory lock
- Audit table: `crawler_run`
- Build artifact: `:crawler-job:bootJar`
- Tunable knobs: `CRAWLER_LIMIT`, `CRAWLER_START_PAGE`, `CRAWLER_MAX_LIST_PAGES`, and `CRAWLER_DETAIL_FETCH_CONCURRENCY` must be positive integers when provided. Detail fetch concurrency is capped at 12.
- Startup validation: crawler numeric configuration is validated before advisory lock acquisition, crawling, or `crawler_run` creation.
- Safety check: the workflow runs PostgreSQL integration tests against a local PostgreSQL service before opening the production write path.
- DB preflight: before running the crawler jar against production, the workflow runs `ops/sql/crawler-identity-preflight.sql` through the SSH tunnel and stops if normalized `laptop.product_code` or `laptop.detail_page` contains duplicate crawler identities.
- Env isolation: production datasource variables are scoped to DB tunnel verification and actual crawler execution, so preflight tests do not accidentally connect through the production tunnel.
- Flyway migrations are owned by the web deploy path. The crawler runtime does not include migration resources, sets `SPRING_FLYWAY_ENABLED=false`, and should fail fast on JPA schema validation if deploy has not prepared the schema.

Useful query:

```sql
select id, status, filter_profile, started_at, ended_at,
       processed_count, created_count, updated_count,
       detail_refresh_count, price_only_updated_count,
       degraded_count, failed_count
from crawler_run
order by started_at desc
limit 20;
```

Crawler identity preflight failure:

1. Do not rerun the crawler immediately. The application crawler persistence layer intentionally fails when a crawler identity maps to multiple laptop rows.
2. Connect to the DB through the same SSH tunnel used by the workflow.
3. Run the diagnostic SQL and inspect duplicate groups:

```bash
psql -h 127.0.0.1 -p 5433 -U "$CRAWLER_DB_USERNAME" -d "$CRAWLER_DB_NAME" \
  -v ON_ERROR_STOP=1 \
  -f ops/sql/crawler-identity-diagnostics.sql
```

4. Decide the canonical row for each duplicate group from `laptop_ids`, `detail_pages`, `product_codes`, and recent price/profile/comment ownership.
5. Back up the DB before changing rows.
6. Merge or retire duplicate rows manually, then rerun:

```bash
psql -h 127.0.0.1 -p 5433 -U "$CRAWLER_DB_USERNAME" -d "$CRAWLER_DB_NAME" \
  -v ON_ERROR_STOP=1 \
  -f ops/sql/crawler-identity-preflight.sql
```

## DB Observability

Recommended 1GB PostgreSQL baseline is versioned at `ops/postgres/laptopgg-postgresql.conf`:

```conf
max_connections = 30
shared_buffers = 256MB
effective_cache_size = 768MB
work_mem = 4MB
maintenance_work_mem = 64MB
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.track = all
track_io_timing = on
log_min_duration_statement = 1000
```

Apply PostgreSQL setting changes separately from app deploys and only after a fresh backup.

Example apply flow on the DB server:

```bash
sudo install -d -m 0750 -o postgres -g postgres /var/backups/laptopgg
sudo install -m 0644 ops/postgres/laptopgg-postgresql.conf /etc/postgresql/16/main/conf.d/laptopgg.conf
sudo -u postgres pg_dump -Fc -d laptopgg -f /var/backups/laptopgg/laptopgg-$(date +%Y%m%d-%H%M%S).dump
sudo systemctl restart postgresql
sudo -u postgres psql -d laptopgg -c 'create extension if not exists pg_stat_statements;'
sudo -u postgres psql -d laptopgg -c "select name, setting from pg_settings where name in ('max_connections', 'effective_cache_size', 'track_io_timing', 'shared_preload_libraries');"
```

## DB Backup

Prepare the existing web VM as the second backup location before enabling the DB job:

- Create a system user/group `laptopgg-backup` with home `/archives`, no password and shell `/usr/sbin/nologin`.
- Create `/var/backups/laptopgg-replica` as root:root 0755, then its `archives` child as laptopgg-backup:laptopgg-backup 0700.
- Install `ops/ssh/laptopgg-backup.conf` as `/etc/ssh/laptopgg-backup.conf` and include it at the **end** of `sshd_config`. Check `sshd -t` before reloading `ssh`.
- Generate a dedicated Ed25519 key as `postgres` on the DB VM at `/var/lib/postgresql/.ssh/laptopgg_backup_ed25519`. Keep the private key there with mode 0600.
- Put only its public key in `/etc/ssh/authorized_keys/laptopgg-backup` on the web VM, prefixed with `restrict,from="<db-private-ip>"`. Keep this file root-owned and readable, not writable, by the backup user.
- Configure the DB user's SSH alias from `ops/postgres/backup-ssh.conf.example`. Pin the web VM's Ed25519 host public key obtained through the existing trusted admin connection. Never disable host-key checking.
- Install `prune-backup-replica.sh` under `/usr/local/lib/laptopgg/` on the web VM with its service/timer from `ops/systemd/`, then enable `laptopgg-backup-replica-prune.timer`.

The receiver permits SFTP upload/rename/metadata operations inside the chroot, with no shell,
forwarding, file reads or remove requests. Uploads use a hidden partial name followed by an
atomic rename. The web VM retains 30 days and always keeps the newest complete archive,
even during a longer source outage; abandoned partial uploads expire after two days.

Install the verified daily backup on the DB VM:

```bash
sudo install -d -m 0755 /usr/local/lib/laptopgg
sudo install -d -m 0700 -o postgres -g postgres /var/backups/laptopgg
sudo install -m 0755 ops/postgres/backup.sh /usr/local/lib/laptopgg/backup.sh
sudo install -m 0644 ops/systemd/laptopgg-backup.{service,timer} /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now laptopgg-backup.timer
sudo systemctl start laptopgg-backup.service
sudo journalctl -u laptopgg-backup.service --no-pager
```

Each run makes a private custom-format dump, restores it into a separate temporary database,
checks the core tables, and only then publishes and replicates the archive. After successful
replication it removes local archives older than 14 days.
Failure leaves existing backups intact. Temporary files and the verification database are cleaned up.
The timer runs at 18:05 UTC with up to five minutes of jitter; missed runs resume on boot.
The job uses local PostgreSQL peer authentication as `postgres` and does not store passwords.
The journal records `LAPTOPGG_BACKUP_VERIFIED` and restored table counts, not comment contents.
Monitor both service results. Transfers are encrypted and both copies are private at rest.
The second VM protects against loss of the DB VM/disk. Both VMs are in the same cloud account/region;
protection against loss of that account or region still requires an independently managed destination.

Create backup from the DB server:

```bash
pg_dump -Fc -d laptopgg -U laptopgg -f /var/backups/laptopgg/laptopgg-$(date +%Y%m%d-%H%M%S).dump
```

Restore into a new database:

```bash
createdb -U laptopgg laptopgg_restore
pg_restore -U laptopgg -d laptopgg_restore --clean --if-exists /var/backups/laptopgg/<backup>.dump
```

## Incident Checks

- Web not responding: `sudo systemctl status laptopgg --no-pager`, then `journalctl -u laptopgg -n 200 --no-pager`.
- Health failing after deploy: verify symlink target, rollback, then inspect logs.
- DB connection failures: check env file, DB security list, PostgreSQL listen address, and SSH tunnel for crawler.
- Crawler stuck: check GitHub Actions run, then `crawler_run` latest row. A `SKIPPED_LOCKED` row means another run held the DB lock.
