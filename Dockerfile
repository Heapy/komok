FROM bellsoft/liberica-openjre-alpine:25
COPY /komok-app/build/distributions/komok-app /opt/backend
ENTRYPOINT ["/opt/backend/bin/backend"]
