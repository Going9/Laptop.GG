#!/usr/bin/env bash
set -euo pipefail
shopt -s nullglob

replica_dir=${LAPTOPGG_BACKUP_REPLICA_DIR:-/var/backups/laptopgg-replica/archives}
archives=("$replica_dir"/laptopgg-*.dump)
if ((${#archives[@]})); then
    latest=${archives[-1]}
    find "$replica_dir" -maxdepth 1 -type f -name 'laptopgg-*.dump' -mtime +29 ! -path "$latest" -delete
fi
find "$replica_dir" -maxdepth 1 -type f -name '.laptopgg-*.dump.partial' -mtime +1 -delete
