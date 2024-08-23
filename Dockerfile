FROM bellsoft/liberica-openjre-alpine:21.0.4
COPY /build/install/backend /opt/backend
COPY /frontend/dist /opt/frontend
ENTRYPOINT /opt/backend/bin/backend
