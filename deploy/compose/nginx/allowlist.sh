#!/bin/sh
# "10.0.0.5,192.168.1.0/24" -> lines "10.0.0.5 0;" "192.168.1.0/24 0;" (DR-55)
: > /etc/nginx/allowlist.conf
for ip in $(echo "${RATE_LIMIT_ALLOWLIST:-}" | tr ',' ' '); do echo "$ip 0;" >> /etc/nginx/allowlist.conf; done
