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

1. Copy `ops/nginx/laptopgg.conf` to `/etc/nginx/sites-available/laptopgg.conf`.
2. Link it into `sites-enabled`.
3. Run `sudo nginx -t && sudo systemctl reload nginx`.
4. Confirm public pages are reachable.
5. Confirm `/actuator/*` and `/api/crawl/*` are not publicly reachable.

## Deploy Flow

GitHub Actions deploy runs:

1. `test`
2. `:web-app:bootJar`
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
- DB preflight: before running the crawler jar against production, the workflow runs `ops/sql/crawler-identity-preflight.sql` through the SSH tunnel and stops if `laptop.product_code` or `laptop.detail_page` contains duplicate crawler identities.
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
