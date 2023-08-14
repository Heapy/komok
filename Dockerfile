FROM bellsoft/liberica-openjre-alpine:17
COPY /build/install/backend /backend
COPY /frontend/dist /frontend
ENTRYPOINT /backend/bin/backend
