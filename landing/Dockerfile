FROM nginx:stable-alpine

LABEL org.opencontainers.image.source="https://github.com/Heapy/komok" \
      org.opencontainers.image.title="Komok landing"

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY build/site/ /usr/share/nginx/html/

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
