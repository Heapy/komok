# Where the data lives

**Status: proposed architecture.** Product constraints come from the discussion; storage engines, protocols and operational defaults below are recommendations to validate. This document does not implement encryption or migrate the existing database.

## The short answer

Use a relational catalogue for entities, relationships, policies and durable change records; an object store for large immutable files; and rebuildable full-text and vector indexes for discovery. Start with SQLite on personal devices and a SQLite-based single-user host. Evaluate PostgreSQL when the NAS service needs concurrent household access or heavier queries. Avoid requiring a separate graph database, vector database and distributed queue from day one.

Each trusted device works with its permitted local subset. A NAS is a useful always-on peer, not the universal owner of all bytes. An S3-compatible service can hold encrypted live replicas and separately retained backups. These have different lifecycles and credentials.

## Implementation baseline

The repository currently uses MongoDB for users, sessions and content documents. Content contains title, text, date, read status and user ID; it is not yet the multi-interface graph described in this guide. The previous React/TypeScript frontend has been removed; its replacement will use Kinetica. Password hashing, TOTP and sessions exist; that does not constitute archive encryption.

Relevant source: [MongoV1](../komok-dao-mg/src/io/heapy/komok/dao/mg/MongoV1.kt), [MongoEntityDao](../komok-app/src/io/heapy/komok/business/entity/MongoEntityDao.kt), [EntityModule](../komok-app/src/io/heapy/komok/business/entity/EntityModule.kt).

The entity read/update routes currently construct a fixed user context, while insertion uses authenticated context. Access enforcement must be audited before extending this prototype to family data or agents. No implemented sync engine, general agent API or application-level archive encryption was found in the inspected paths.

The relational proposal is a new direction to benchmark, not an assertion that PostgreSQL is already in use. Preserve the Mongo prototype until a migration can export, validate and restore every supported record.

## Boundaries and data flow

<figure class="architecture-map">
  <div class="architecture-row"><span>Web · iOS · desktop · Apple TV</span><span>Telegram · browser extensions · MCP</span></div>
  <div class="architecture-arrow">↓ authenticated, scoped operations ↓</div>
  <div class="architecture-core"><strong>Komok domain service</strong><span>schema validation · access policy · approval · event transaction</span></div>
  <div class="architecture-row"><span><strong>Catalogue</strong>entities · links · changes</span><span><strong>Object store</strong>originals · manifests</span><span><strong>Derived indexes</strong>text · vectors · previews</span></div>
  <div class="architecture-arrow">↕ authenticated peers / selective replication</div>
  <div class="architecture-row"><span>Personal devices</span><span>Home NAS</span><span>Encrypted remote storage</span></div>
  <figcaption>Logical boundaries. These can start in one process; the diagram does not prescribe microservices. Backups cross a separate retention and credential boundary.</figcaption>
</figure>

## A logical model independent of engines

| Record | Responsibility |
| --- | --- |
| Entity | Stable ID, space ID, lifecycle state, schema version and attributes. IDs are generated offline; file paths and hostnames are not identities. |
| Interface | Versioned schema describing a capability such as task, photo, bookmark or reading progress. One entity can implement several. |
| Relation | A first-class entity with source, target, relation type, provenance and attributes. Indexed in both directions. |
| Change | Unique operation ID, actor/device, causal dependencies, entity ID, schema version and operation payload. Sensitive contents are encrypted when replicated to untrusted storage. |
| Blob manifest | Original/version ID, encrypted chunk references, lengths, integrity information and key-envelope references. Names, EXIF and private checksums stay inside encrypted metadata. |
| Replica | Which peer or provider has which verified object version, whether it is pinned and when it was checked. |
| View | Query, filters, selected fields and layout. A view never expands permission. |
| Policy and grant | Principals, spaces, capabilities and epochs; changes follow an explicit authority model. |
| Derivation | Input versions, parser/model identity, parameters and resulting entities or indexes. |

Use typed columns for identity, ownership, lifecycle and frequently queried fields; versioned JSON-like attributes for extensibility; and dedicated tables where constraints demand them. Do not turn every byte into an untyped EAV triple. Preserve the flexibility of the original entity/attribute idea without requiring its proposed index permutations as the physical schema. PostgreSQL's `jsonb` is one possible implementation of extensible attributes. [PostgreSQL JSON types](https://www.postgresql.org/docs/current/datatype-json.html).

Financial amounts use exact representations and currency metadata. Health observations preserve source units and timestamps. Dates distinguish local calendar dates, instants and time zones. Relationships and user corrections are durable records, not disposable LLM output.

## Which storage is needed?

| Layer | Initial proposal | Why / when to add more |
| --- | --- | --- |
| Local catalogue | SQLite per trusted device | Transactions and offline queries with a small operational footprint. Browser persistence and the KMP drivers need platform-specific validation. |
| Always-on catalogue | SQLite initially; PostgreSQL for concurrency and larger queries | A household server may benefit from PostgreSQL. Keep SQL dialects behind tested adapters; shared domain behaviour does not imply identical SQL. |
| Files and media | Filesystem blob directory on devices/NAS; object-storage adapter for S3/R2/etc. | Keep video, photos, audio and source archives out of ordinary entity rows. Immutable object versions simplify replication. |
| Full-text search | SQLite FTS5 locally; PostgreSQL text search if that host uses PostgreSQL | Exact words, titles and transcripts should work without embeddings. FTS5 is an existing SQLite extension. [SQLite FTS5](https://sqlite.org/fts5.html). |
| Semantic/image similarity | Rebuildable, scoped embeddings; evaluate an embedded index or pgvector on a PostgreSQL host | Add a separate vector service only after measured scale or operational need. pgvector supports vector search within PostgreSQL. [pgvector](https://github.com/pgvector/pgvector). |
| Jobs | Durable transactional outbox and local worker queue | Start without Kafka or another independently operated broker. Record attempts, leases and idempotency keys. |

The logical graph is stored as entities and indexed relations. A graph database is optional later if real traversal workloads justify its cost. Full-text indexes, embeddings, thumbnails and disposable preview summaries are derivatives. **Saved knowledge entities, source annotations and manual corrections are durable records**, even when they were originally created automatically. Preserve transcripts used as the source of a saved annotation so that its provenance remains meaningful.

Search enforces access before returning results, snippets, counts or suggestions. Shared vector indexes must not reveal inaccessible neighbours through scoring or metadata. A trusted host must decrypt authorised data to index it; conventional SQL/vector queries cannot operate on arbitrary encrypted blobs. Keep indexes on trusted, encrypted-at-rest devices and rebuild them after revocation or loss.

## Placement, availability and eviction

Placement policy applies per collection or object version. It can ask for local metadata, a preview, an original, or a downloaded trip collection. It also specifies required durable copies separately from cache preferences.

| Example | Laptop / phone | NAS | Remote |
| --- | --- | --- | --- |
| Today's plans and notes | Editable local data | Synced permitted copy | Encrypted backup |
| Upcoming trip | Selected notes, maps, documents and photos pinned | Usually originals | Backup; optional replica |
| Family photos | Selected originals and previews | Most or all originals | Independently retained encrypted backup |
| Large rarely used video | Catalogue entry or temporary cache | Optional | May be the live original; still needs independent backup coverage |

A verified upload does not by itself authorise deleting the local original. Eviction checks required-copy policy and never removes the last required durable copy. Pinning defeats automatic eviction. Logical deletion, hiding in a personal view, removing a replica and expiring a backup are separate operations.

Display explicit states: available here, downloading, remote only, waiting for a peer, failed verification or missing. Before a flight, verify that the selected collection's required objects are actually readable offline. A cached catalogue entry alone is not success.

## Synchronisation without a central owner

Start with authenticated device-to-device or device-to-NAS sessions, store-and-forward delivery and immutable blobs. Peer discovery and rendezvous can have optional adapters; no central account server is required by the protocol. A transport cannot grant access by forwarding bytes.

1. A local command validates schema and policy, then atomically changes the local projection and appends an operation to the outbox.
2. Peers exchange permitted change summaries and request missing operations. Operation IDs and causal dependencies make delivery idempotent; wall-clock timestamps alone do not order edits.
3. A receiver checks actor authorisation, scope, schema and causal context before applying a change. Missing prerequisites are queued, not guessed.
4. Blob transfer follows placement policy, separately from small metadata changes. Transfers resume and verify authenticated content before a replica is marked healthy.
5. Acknowledgements permit safe compaction only after the retention and stale-device policy is met. A long-absent device may need a fresh snapshot rather than replaying a pruned history.

The catalogue projection, event history and outbox must have a documented consistency boundary. Backups capture a consistent snapshot plus the changes after its checkpoint, not an arbitrary copy of an open database file.

### Merging is a domain decision

| Data | Proposed treatment |
| --- | --- |
| Note text / collaborative ordered content | Evaluate a mature CRDT implementation; retain meaningful concurrent variants. |
| Collection membership and independent links | Set operations with explicit remove semantics and causal metadata. |
| Scalar edits such as a title | Preserve conflicting values or offer a deterministic default with visible history. |
| Financial postings and other invariants | Idempotent append/correction records; validate constraints. Do not automatically add two independently edited balances. |
| Access policy, key epochs and publication | Authorised, serialised decisions per space/publication; never ordinary last-write-wins merges. |

Automerge and Loro are evaluation candidates. CRDT convergence alone does not establish domain correctness or authorisation. [Automerge conflict documentation](https://automerge.org/docs/reference/documents/conflicts/), [Loro project](https://github.com/loro-dev/loro).

For a first version, a designated owner device or service serialises policy changes per space. Offline devices may record normal edits under their last known grant; peers quarantine disputed edits after a revocation instead of trusting a claimed creation timestamp. Immediate revocation on a disconnected device cannot be promised. An offline agent cannot publish based on a stale approval.

Deletion propagates as a tombstone. Garbage collection waits for the retention rule and active replicas; stale replicas cannot resurrect deleted records on reconnect. Large blobs are collected only when no retained manifest, version or backup policy needs them.

## Mail, contacts and calendar interoperability

Fastmail, CardDAV and Apple Calendar are first-class integration requirements; see [CONNECT-01–03](features.md#mail-contacts-and-calendars). Keep provider synchronisation separate from Komok's peer sync protocol. External systems have their own identities, revisions, permissions and conflict semantics.

| Adapter | Proposed responsibility | Boundary to validate |
| --- | --- | --- |
| Fastmail / JMAP | Import selected mailboxes, messages, threads and attachments; synchronise supported state | Account-scoped IDs, incremental checkpoints, offline availability and separately granted write operations. |
| CardDAV / vCard | Map selected address books to contact entities; enable two-way sync by policy | Field ownership, duplicate contacts, remote revisions, deletion and preservation of unsupported fields. |
| CalDAV / iCalendar | Connect calendars, including Fastmail; support Apple Calendar interoperability | Export/subscription versus editable sync, recurring series and exceptions, time zones, invitations and cancellation. |

These protocol routes follow [Fastmail's documented interfaces](https://www.fastmail.com/dev/). The [Apple Calendar account model](https://support.apple.com/en-ca/guide/calendar/-icl4308d6701/mac) means a calendar hosted by Fastmail and viewed in Apple Calendar is one remote collection, not two independent copies to import. An iCloud-hosted calendar is a separate account/backend case. The exact route—provider adapter, a Komok CalDAV endpoint or native platform bridge—remains a design decision, with capabilities tested per route.

Store connector account ID, collection ID, external object ID, remote revision/checkpoint, local mapping and last successful sync separately from domain attributes. Apply received data and advance its checkpoint transactionally. Persist an idempotent outbound queue, suppress feedback loops and recheck grants before replaying offline writes. A temporary missing collection or authentication failure must not be interpreted as deletion of its contents.

Define field mappings and conflict behaviour explicitly. Preserve original payloads or unsupported fields for lossless round trips; do not export a person's private notes merely because the person also implements the contact interface. For calendars, maintain series identity and recurrence exceptions rather than flattening everything into unrelated events. Reconnecting must not duplicate contacts, episodes or birthdays already mapped to local entities.

Separate read, update, delete, send/invite and publish capabilities. Credentials belong to the connector service, not to LLM context. Shared-calendar writes, attendee invitations and feeds can disclose data; apply the approval boundary before those actions. A connection to an account is not blanket permission to publish the connected graph.

## Encryption before remote storage

The remote provider receives ciphertext, including encrypted manifests and catalogue snapshots. Provider-side encryption is an additional layer; it does not replace encryption on the trusted device before upload.

Use envelope encryption through a reviewed library and a versioned format. Each object version (or bounded backup pack) gets a random data-encryption key. A separate key-encryption key wraps that data key. The encrypted payload can travel with its wrapped key; raw keys do not go in provider metadata. This is the envelope pattern, not a commitment to AWS KMS. [AWS explanation of client-side envelope encryption](https://docs.aws.amazon.com/kms/latest/cryptographic-details/client-side-encryption.html).

| Key or credential | Scope and custody |
| --- | --- |
| Device identity/signing key | One device; OS-backed custody where available. Separate from data decryption. |
| Space key-encryption key | One access domain and epoch. Different spaces have independent keys and grants. |
| Object-version data key | One immutable original/version or bounded encrypted pack, wrapped for its authorised domain. |
| Backup key set | Separate per backup repository/trust domain; avoid making one compromised live-space key a key to every historical backup. |
| Recovery material | Offline recovery for the relevant key sets, with an inventory and tested restore instructions. Not available to routine agents. |
| Provider credentials | Per service and destination, limited to necessary object operations. Not encryption keys. |

An authorised local key broker unwraps only the keys needed for a scoped operation. Store encrypted key envelopes with the data and protect the broker's roots with platform facilities plus a user recovery mechanism. A login password is not the one key for the archive. Do not invent a new cipher or derive all independent domains from one agent-accessible secret.

Use authenticated encryption and library-managed nonce rules. Bind the object ID, version, space, format version and chunk position to authenticated metadata. Detect substitution, truncation and reordering. Streaming formats such as libsodium's `secretstream` are candidates for sequential files; seekable movie playback may require an established authenticated random-access format instead. Validate the format rather than adapting a sequential stream by guessing offsets. [libsodium file encryption](https://libsodium.gitbook.io/doc/secret-key_cryptography/secretstream).

Deduplicate within an authorised domain using protected identifiers; avoid public plaintext hashes, convergent encryption or global cross-user deduplication that reveals content equality. Random object names and encrypted manifests hide names and relationships, though sizes and traffic timing can still leak information.

### Rotation, loss and recovery

Routine wrapping-key rotation can rewrap data keys. After a key or recipient is compromised, rewrapping alone is insufficient if old envelopes were copied: new versions need fresh data keys, and retained versions may need re-encryption. Revocation stops future access through the service; it cannot erase plaintext already obtained by another device or person.

Keep retired decryption keys only while retained data requires them. Deleting a key can make a backup permanently unreadable, so key retirement and backup retention must be coordinated. Recovery is tested on an empty device, with the NAS unavailable and without an agent holding a universal secret.

Authenticated ciphertext does not prove that a provider returned the newest valid version. Keep verifiable checkpoints and key/policy epochs on trusted peers and in recovery records. A rollback to an older snapshot is an explicit restore decision; it must not silently reactivate revoked grants.

## Agent and mini-app boundary

Agents call domain tools through the same policy service as normal clients. A capability names the subject, space or entity set, allowed operations, data limits and expiry. An LLM's text is not an access-control decision.

| Actor | May receive | Must not receive |
| --- | --- | --- |
| Summarisation agent | Selected source fragments; permission to write linked drafts | Space roots, raw database access, unrelated history |
| Backup uploader | Already encrypted objects; destination-scoped upload capability | Decryption keys or backup deletion authority |
| Restore service | Temporary access to a selected repository and authorised recovery operation | Permanent access to all spaces by default |
| Publication worker | An approved export snapshot and destination-specific credentials | Permission to follow arbitrary private links or publish a newer revision |
| Generated mini-app | Validated data APIs for the user's authorised scope | Arbitrary filesystem, network or key-broker access |

Run key services and credentials outside the agent's process, shell environment, tool-visible filesystem and logs. An agent with unrestricted host-shell access under the same account is outside this containment model; capability scoping in an API does not protect against that host access. Separate OS identities/sandboxes and a broker are part of making the boundary real.

Hosted model calls are data exports: the chosen provider and permitted context must satisfy the user's policy. No silent fallback sends private data to another provider. Retrieved documents, Telegram messages and webpage instructions cannot grant tool permissions. Scope tool results and logs as carefully as inputs. MCP is an adapter to this boundary, not an exception. [MCP security guidance](https://modelcontextprotocol.io/specification/latest/basic/security_best_practices).

### Publication approval

Prepare a concrete export, including its transitive files and metadata. Bind approval to its digest, versions, audience, destination and permitted action. A changed export needs a new approval. Recheck access and approval at execution time. Produce an audit record; support withdrawing the served copy while acknowledging that recipients may retain previous downloads.

This applies to blogs, standalone sites, RSS/Atom output, shared collections and access expansion. Ordinary private extraction or edits can be automated within existing permissions; they are not all publication actions.

## Backups that can actually be restored

The 3-2-1 target means three copies, two types of storage/media and at least one offsite copy. Proposed layout: a live collection, an independent encrypted backup on different storage, and an offsite copy. A disconnected rotated disk plus a remote encrypted repository is one candidate alongside a NAS. The user has not chosen the third destination. Different buckets in one account are not automatically independent failure domains.

For a remote-only original, the remote live object is copy one. It still needs two additional independent copies under the chosen policy; “the NAS backs up everything” would be false for that object. A second provider and an offline disk can satisfy that coverage without a permanent NAS replica.

The backup includes catalogue checkpoints, subsequent change records, original blobs, manifests, permissions, schemas, user corrections and recoverable key envelopes. Derived indexes and previews may be regenerated, provided their loss does not lose manual work. Restore tools and format documentation must be available without the original installation.

Separate upload, retention/deletion and restore credentials. Use immutable or locked versions where the provider supports the required semantics. Do not assume every S3-compatible endpoint implements Object Lock; verify support and behaviour. Retention locks need deliberate configuration because they can prevent deletion for their term. [Amazon S3 Object Lock](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html).

Schedule inventory checks and actual restores, not just successful upload counters. A candidate policy is daily incremental backup, longer weekly/monthly retention and periodic restore drills; choose real recovery-point and recovery-time targets before making promises. Sync can replicate an accidental deletion immediately; backup retention must survive that deletion.

## Paying for storage and for recovery

Use warm object storage for interactive remote originals; consider cold storage for a separate disaster-recovery copy. Pack small immutable backup records to avoid excessive requests, but keep pack sizes bounded so a single-file restore does not fetch the entire archive. Estimate ciphertext size, retained versions and change rate, not just today's originals.

Illustrative published rates checked on **18 October 2026**, in USD, before taxes and account-specific terms:

| Candidate | Storage-only illustration for 1,000 GB | Restore and operational tradeoff |
| --- | --- | --- |
| R2 Standard | $15/month before free allowances | Direct R2 egress is free; operation charges still apply. Suitable to evaluate for actively fetched remote objects. |
| R2 Infrequent Access | $10/month | $0.01/GB retrieval, 30-day minimum storage duration and different operation rates. Reading the entire 1,000 GB adds $10 in retrieval alone. |
| S3 Glacier classes | Quote for the actual AWS region and class | Evaluate separately for cold backups; Deep Archive retrieval is measured in hours, with minimum-duration, request and transfer costs to include. |

The R2 arithmetic deliberately excludes free allowances: 1,000 × $0.015 and 1,000 × $0.01. It is a comparison of the published storage component, not a total bill. Consult the [R2 price table](https://developers.cloudflare.com/r2/pricing/), [S3 pricing](https://aws.amazon.com/s3/pricing/) and [Glacier retrieval choices](https://aws.amazon.com/s3/storage-classes/glacier/) before selecting a provider.

Budget = retained encrypted GB × storage rate + writes/lists/reads + retrieval + transfer + any minimum-duration charges + local hardware/energy. Model both an ordinary month and a complete disaster restore. “Cheap to keep” is not necessarily “cheap or fast to recover.”

## Federation and portable identity

Keep identity independent of a hostname. A migration publishes a verifiable endpoint update to existing peers; trust bootstrapping, recovery and compromised-identity handling need a protocol design. Separate federation identity keys from device keys and archive keys.

Share explicitly approved snapshots or bounded collections with recipient grants. A peer may distribute ciphertext without gaining decryption rights. A public torrent swarm is not automatically suitable for private material: peer addresses, manifests and participation can leak information. Evaluate encrypted transfer, discovery privacy, resumability and redistribution rights before choosing a protocol.

Tailscale, Cloudflare Tunnel or a future tunnel can carry connections. They are deployment choices, not the identity authority or an obligatory central federation server.

## The next decisions to prove

1. Benchmark an offline catalogue on iOS, desktop and browser, including migrations and large relation sets. Choose the first supported engines from evidence.
2. Build one synchronised note/task slice. Test concurrent edits, duplicate delivery, lost acknowledgements, stale replicas, deletion and policy revocation.
3. Choose established encryption and backup formats; validate Kotlin/JVM and Native interoperability with cross-platform fixtures and independent review.
4. Restore on a blank device after simulated NAS loss, corrupt chunks and expired credentials. Include a remote-only original and a retired key epoch.
5. Exercise agent isolation and publication approval against overbroad queries, modified snapshots, hostile imported text and revoked grants.
6. Export existing Mongo records, verify owner mapping and counts, import into the chosen catalogue, and compare round trips before changing the running prototype.

These are design gates for implementation. The site and this document make no claim that those checks have already passed.
