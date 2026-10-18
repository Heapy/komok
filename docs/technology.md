# A Kotlin ecosystem with a reason to exist

Komok is the product. Heapy is the ecosystem of reusable Kotlin libraries that its real needs help develop. The target is Kotlin throughout the product and KMP where portability is useful: Kotlin/Native for iOS, desktop and suitable backend workloads; JavaScript and eventually Wasm for the browser; JVM where its libraries or runtime are advantageous.

This target is not a description of today's repository. The previous React frontend has been removed; its replacement will use Kinetica. A Kotlin backend framework is part of the ecosystem work. Portability must be demonstrated on actual targets rather than inferred from a `commonMain` directory.

## Deliver a feature, then grow the library

Use a suitable existing library, service or application when it makes a product feature possible. It may be written in Rust, Python, Go or another language. Keep its boundary explicit and create a separate Kotlin/KMP backlog item if replacing it has a useful purpose.

A port or an independent implementation needs a documented specification, licence/provenance review and compatibility fixtures. Preserve working integrations until the replacement meets the product's quality and operational requirements. “100% Kotlin” is a long-term direction, not a reason to block a useful feature indefinitely.

## Technical backlog

| ID | Work | First evidence of success |
| --- | --- | --- |
| TECH-01 | Provider-neutral LLM interface, including local runtimes and hosted providers | The same product workflow works with two materially different adapters, including a local model, with explicit capability negotiation. |
| TECH-02 | Shared entity schemas, commands, relations and scoped queries | A custom entity and a built-in entity work through the same access and validation boundary on two targets. |
| TECH-03 | Kinetica views and safe mini-app capabilities | A generated trip-selection view changes ordinary entity links without direct database or unrestricted network access. |
| TECH-04 | Backend framework and durable handlers | A retried source event creates one related task, with observable failure and cancellation. |
| TECH-05 | Local catalogue, sync and migration adapters | Offline edits converge according to documented domain rules; stale and revoked peers are handled explicitly. |
| TECH-06 | Object placement, backup and key-broker interfaces | An encrypted backup restores on an empty device without giving the uploader a decryption key. |
| TECH-07 | Diagram rendering adapter, then a Kotlin/KMP implementation | A stored source version has a reproducible preview and a clear privacy boundary. |
| TECH-08 | Browser extensions and capture services | Firefox, Chrome and Safari can preserve a page and its context, with exclusions and reliable retries; evaluate notification and fullscreen APIs where useful. |
| TECH-09 | iOS capture, location and health adapters | Native permissions, background behaviour, battery impact and offline operation are verified on-device. |
| TECH-10 | Media and podcast adapters, web/Apple TV playback | A real file can be imported, indexed and resumed across supported players. Kotlin replacements remain separate projects. |
| TECH-11 | Federation, portable identity and efficient transfer | Two independently operated instances exchange only approved data without a mandatory central service. |
| TECH-12 | Remote access adapter; evaluate a Kotlin tunnel separately | Authenticated remote access works without exposing broad local-service access. |
| TECH-13 | MCP server and agent capability broker | An agent completes a scoped task while denied access to unrelated entities, raw keys and unapproved publication. |
| TECH-14 | Proxy with context parameters | Continue the existing proxy-generation work using context parameters for dependency injection, caching, retries, logging, metrics and mocks; verify those capabilities in consumers. Former idea #294. |
| TECH-15 | Knowledge extraction and novelty evaluation | A small reviewed corpus measures useful new information, missed nuance, false merges and preservation of manual edits. |
| TECH-16 | Fastmail JMAP connector and reusable Kotlin/KMP client | Selected mailboxes, messages, threads and attachments map to stable Komok entities; incremental imports resume without duplication and credentials stay outside agent context. |
| TECH-17 | CardDAV/vCard connector and reusable Kotlin/KMP support | A selected Fastmail address book round-trips supported fields, with explicit write-back, conflict handling and no leakage of private Komok annotations. |
| TECH-18 | Calendar interoperability: CalDAV/iCalendar adapters and Apple Calendar support | Export/subscription and editable sync are verified separately, including Fastmail calendars, time zones, recurrence exceptions and cancellation. Test Apple Calendar clients separately from iCloud hosting; retain Google Calendar as an additional target. |
| TECH-19 | Storage, service boundaries and operations | Validate storage/query choices, transactions, sync and communication boundaries against product workloads. Decide API shape, configuration, discovery and failure isolation only where needed; cover deployment/CI, integration tests, logs and monitoring. Earlier Cassandra/PostgreSQL and microservice ideas were candidates, not commitments. |

## Required personal integrations

[CONNECT-01–03](features.md#mail-contacts-and-calendars) are product requirements. Implementation routes still need validation: use existing protocol libraries first if useful, and track reusable Kotlin/KMP implementations independently. Fastmail documents JMAP for mail, CardDAV or JMAP for contacts, and CalDAV for calendars; one generic “JMAP integration” should not stand in for all three. [Fastmail developer documentation](https://www.fastmail.com/dev/).

Apple Calendar can use calendar accounts and subscriptions. A subscription is a viewing route; editable account integration needs a separate round-trip check. Do not equate the Apple Calendar application with iCloud as the storage provider. [Apple calendar accounts](https://support.apple.com/en-ca/guide/calendar/-icl4308d6701/mac), [Apple calendar subscriptions](https://support.apple.com/guide/calendar/subscribe-to-calendars-icl1022/mac).

## The LLM interface deserves its own project

Compare [Spring AI](https://docs.spring.io/spring-ai/reference/) and [LangChain4j](https://docs.langchain4j.dev/) as references when designing the abstraction. Neither is selected as the foundation by this document. Evaluate other libraries and direct provider protocols too.

The interface should cover text and structured responses, streaming, cancellation, tool requests, embeddings and multimodal input where supported. Providers advertise capabilities; a common API must not pretend that every model can do everything. Distinguish a model asking to call a tool from the domain service authorising and executing that tool.

Local and hosted models have equal standing in product flows and compatibility checks. Routing can consider task, capability, data policy, latency and budget. Failure must not silently export local-only context to a hosted provider. Record the model, prompt/schema version and source versions needed to understand a derivation without logging sensitive prompts indiscriminately.

## Integration candidates, not commitments

| Area | Useful references | Decision still needed |
| --- | --- | --- |
| Browser archive | [ArchiveBox](https://github.com/ArchiveBox/ArchiveBox), [Memex](https://github.com/WorldBrain/Memex) | Capture format, browser permissions, exclusions and replay fidelity. |
| Family memories | [Bonsai](https://github.com/impworks/bonsai), [Ente](https://ente.io/) | Photo ingestion, access, recognition and personal views. |
| Media | [Jellyfin](https://jellyfin.org/), [Plex](https://www.plex.tv/) | Reuse boundaries, playback, codecs, metadata and clients. |
| Weather | [Windy Point API](https://api.windy.com/point-forecast/pricing), [Windy maps](https://api.windy.com/map-forecast/pricing), [Open-Meteo](https://open-meteo.com/en/docs) | Forecast quality for relevant places, models, licence, cost and embedding versus data access. No provider selected. |
| Learning and habits | [Anki](https://apps.ankiweb.net/), [Loop](https://github.com/iSoron/uhabits) | Card representation, repetition, import/export and useful reminders. |
| News | [Kagi News](https://blog.kagi.com/kagi-news) | Event clustering and source explanations, independent of a daily digest format. |
| Location | [CityStrides](https://citystrides.com/), [FxLifeSheet](https://github.com/KrauseFx/FxLifeSheet) | Capture, imports, visited-area semantics and privacy. |
| Personal data | [HPI](https://github.com/karlicoss/HPI), [a year in data](https://samplesize.one/blog/posts/my_year_in_data/) | Import contracts, provenance and meaningful personal observations. |
| Existing channels | [Heapy/kotbot](https://github.com/Heapy/kotbot), [Home Assistant](https://www.home-assistant.io/) | Telegram as a channel; Home Assistant remains responsible for home control. |

| Canvas and collections | [Milanote](https://milanote.com/), [Notion](https://www.notion.so/), [Airtable](https://airtable.com/) | Dense Library views and a canvas over shared entities. |
| Source adapters | [RSSHub](https://github.com/DIYgod/RSSHub), [FiveFilters](https://github.com/fivefilters) | Monitoring sources that have no native feed. |
| Recipes and credentials | [Yuml recipes](https://github.com/256-recipes/yuml), [Open Badges](https://openbadges.org/) | Portable source formats and imports. |
| Diagrams | [Mermaid](https://mermaid.live/), [diagrams.net](https://app.diagrams.net/) | Source formats, rendering and Kotlin replacement boundaries. |
| Connected information | [OMEA](https://www.jetbrains.com/omea/), [Zotero](https://www.zotero.org/) | Organising sources and linking personal records. |

These are research leads preserved from the discussion and issue comments, not endorsements or claims of compatibility. Before adopting a component, check the primary documentation, its licence, target support and current maintenance.

## Keep the backlog honest

Product features describe user outcomes. Technical items measure reusable capabilities. A component counts as complete only when the product uses it successfully and its claimed KMP targets are verified. There are no release dates implied by this document.
