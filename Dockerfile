FROM bellsoft/liberica-openjre-alpine:25
COPY /komok-app/build/distributions/komok-app /opt/backend
COPY /frontend/dist /opt/frontend
ENTRYPOINT ["/opt/backend/bin/backend"]
