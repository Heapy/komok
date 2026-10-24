# komok-dao-mg Plan

## Scope

Mongo persistence security, connection configuration, client lifecycle, database selection, and focused test coverage.

## Severity-Ranked Issues

### High

- Raw TOTP seeds and active session tokens are stored in Mongo model objects. A MongoDB read compromise can become direct account takeover.

### Medium

- Mongo URI is assembled as `mongodb://...` with interpolated credentials and no explicit TLS, `authSource`, or encoding controls.
- `MongoClient` lifecycle is not explicitly closed.
- Database selection is hard-coded to `komok`, and config has no database field.
- Config path, URI construction, and database selection have no focused tests.

### Low

- Mongo schema and collection constants lack codec round-trip tests.

## Fix Steps

1. Store session tokens as non-reversible hashes or keyed HMAC fingerprints.
2. Encrypt TOTP seeds at rest, preferably with envelope encryption/KMS-backed key material.
3. Replace ad hoc Mongo URI interpolation with typed and validated connection settings.
4. Add explicit config for TLS, `authSource`, credential encoding, and database name.
5. Introduce explicit `MongoClient` shutdown ownership.
6. Add focused tests for config, connection settings, database selection, and codec round trips.
7. Add migration handling for existing raw secret/token records if production data exists.

## Verification

- Run `./gradlew :komok-dao-mg:test`.
- Add regression tests for credential encoding, TLS/authSource config, and database selection.
- Confirm no raw session tokens are persisted.
- Confirm TOTP seed storage is encrypted or externalized.
- Confirm Mongo client closure is exercised by lifecycle tests.
