#!/usr/bin/env bash
set -euo pipefail

image="${1:-komok-landing:test}"
container="$(docker run -d "$image")"
trap 'docker rm -f "$container" >/dev/null' EXIT

for attempt in $(seq 1 30); do
    if docker exec "$container" wget -q -O /dev/null http://127.0.0.1/healthz; then
        break
    fi
    sleep 1
done

docker exec "$container" nginx -t
# Keep checks inside the container: no public or host port is needed.
docker exec "$container" sh -eu -c '
    wget -qO- http://127.0.0.1/healthz | grep -qx ok
    wget -qO- http://127.0.0.1/ | grep -q "Your plans, knowledge and memories, connected."
    wget -qO- http://127.0.0.1/site.js | grep -q selectTab
    for asset in style.css assets/komok-stream.png assets/komok-library.png; do
        wget -q -O /dev/null "http://127.0.0.1/$asset"
    done
    for path in missing/nested/page docs/architecture.md; do
        if wget -S -O /tmp/missing "http://127.0.0.1/$path" 2>/tmp/response; then
            echo "Expected 404 for $path" >&2
            exit 1
        fi
        grep -q "HTTP/1.1 404" /tmp/response
    done
'
printf 'Landing container checks passed.\n'
