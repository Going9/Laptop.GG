#!/usr/bin/env bash
set -euo pipefail
umask 077

backup_dir=${LAPTOPGG_BACKUP_DIR:-/var/backups/laptopgg}
install -d -m 700 "$backup_dir"
exec 9>"$backup_dir/.backup.lock"
flock -n 9 || { echo "LaptopGG backup is already running."; exit 0; }

partial=$(mktemp "$backup_dir/.laptopgg-backup.XXXXXX")
verify_db="laptopgg_backup_verify_$$"
cleanup() {
    result=$?
    trap - EXIT
    rm -f -- "$partial"
    if ! dropdb --if-exists "$verify_db"; then
        echo "Backup verification database cleanup failed: $verify_db" >&2
        result=1
    fi
    exit "$result"
}
trap cleanup EXIT

pg_dump --format=custom --no-owner --no-acl --file="$partial" --dbname=laptopgg
createdb --template=template0 "$verify_db"
pg_restore --exit-on-error --no-owner --no-acl --dbname="$verify_db" "$partial"
psql --no-psqlrc --set=ON_ERROR_STOP=1 --dbname="$verify_db" --tuples-only --no-align \
    --command='select count(*) as laptops from laptop; select count(*) as comments from comment; select count(*) as scores from recommendation_score;'

archive="$backup_dir/laptopgg-$(date -u +%Y%m%dT%H%M%S.%NZ).dump"
mv -- "$partial" "$archive"
find "$backup_dir" -maxdepth 1 -type f -name 'laptopgg-*.dump' -mtime +13 -delete
echo "LAPTOPGG_BACKUP_VERIFIED archive=$archive"
