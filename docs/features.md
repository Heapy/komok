# Features

Product ideas from the GitHub issues, original README and product discussion. This is the scope catalogue, not a release schedule or a list of shipped features. IDs remain stable when names or priorities change. Technical work lives in [technology.md](technology.md); storage and sync proposals live in [architecture.md](architecture.md).

## Daily planning

<a id="day-01"></a>

### DAY-01 · Stream

- One compact, information-dense feed of relevant entities, plans and reasons to act. Open a card to see the entity and its connections.
- Rank by importance, urgency, context, available time and source feedback. Allow manual corrections, read/archive actions and entity-specific actions.
- Batch low-priority sources, estimate total queue duration and keep spare-time items accessible. Send important reminders through enabled notification channels.

<a id="day-02"></a>

### DAY-02 · Dependencies

- Any entity can depend on or be blocked by another entity or condition.
- Show “Choose trip dates” first; make “Book accommodation” eligible only once dates are known. Explain what is blocking an item.

<a id="day-03"></a>

### DAY-03 · Snooze

- Postpone any entity until a date, elapsed interval or event; keep it searchable while absent from the stream.
- Support deadlines and event-relative follow-ups, such as a reminder after returning from a trip.

<a id="day-04"></a>

### DAY-04 · Tasks and calendar

- Plan tasks, events and recurring chores with task trees, duration estimates, travel time and completion history.
- Capture from web/PWA or Telegram. Any suitable entity can appear as an event. Prioritise planning over measuring time spent.

<a id="day-05"></a>

### DAY-05 · Goals

- Store goals as entities with progress and links to tasks, resources and practice.
- Let an assistant create connected next steps; edit or dismiss them using ordinary task and dependency rules.

<a id="day-06"></a>

### DAY-06 · Birthdays

- Connect recurring birthday reminders to people, contacts and family history.

<a id="day-07"></a>

### DAY-07 · Weather assistant

- Proactively warn about cold, heat, rain or wind relevant to a location and upcoming plans.
- Show forecast time and source. Windy is the quality reference and a possible integration; provider choice remains open.

<a id="day-08"></a>

### DAY-08 · Habits

- Schedule recurring activities, mark them complete, snoozed or missed, and show streaks and history.
- Surface reminders in the stream and Telegram; allow individual occurrences to move.

<a id="day-09"></a>

### DAY-09 · Morning briefing

- A ready-made view of today's date, plans, weather, currency changes and habit progress.

## Capture and knowledge

<a id="know-01"></a>

### KNOW-01 · Browser archive and bookmarks

- Firefox, Chrome and Safari extensions automatically save visited pages, content and history. Support bookmarks, browser bookmark sync and address-bar suggestions.
- Keep URL, visit time, annotations, project links, reading position and read state. Search archived text after the original page or tab is gone.
- Provide capture exclusions and pause controls for unwanted or private content.

<a id="know-02"></a>

### KNOW-02 · RSS and source monitoring

- A usable RSS reader with subscriptions, categories, OPML import and duplicate detection.
- Monitor sites without feeds and turn updates into feed items. Allow manually adding an item to a collection or reading queue.

<a id="know-03"></a>

### KNOW-03 · Second brain

- Extract meanings from consumed articles, videos and podcasts into a growing personal wiki, with links to source passages or timestamps.
- Save extracted knowledge automatically after consumption; allow manual review and corrections that survive reprocessing.
- Facts, ideas and observations are provisional categories. Extraction algorithms and prompts need experiments.

<a id="know-04"></a>

### KNOW-04 · New knowledge detection

- Compare incoming material with saved knowledge; identify new ideas, additions, qualifications and contradictions.
- Offer a summary of what is new, highlighted excerpts, a quiz or the full source. Reduce repetitive reading without treating saved information as proven understanding.

<a id="know-05"></a>

### KNOW-05 · News events

- Combine reports about the same event from RSS, Telegram and other sources into one card.
- Explain the event and its relevance, retain all sources and disagreements, and allow incorrect groups to be split. Independent of Kagi News.

<a id="know-06"></a>

### KNOW-06 · Voice capture

- Accept voice input in the app and Telegram voice messages; turn speech into editable notes, tasks or other entities.
- Reuse transcription for searchable podcasts and videos.

<a id="know-07"></a>

### KNOW-07 · External activity

- Import GitHub assigned/project issues, pull requests and review activity; connect updates to projects and possible tasks.
- Read Telegram channels, with TDLib as a candidate. Bluesky and Twitter/X remain future sources of news and ideas.
- Collect information and reasons to act first. Repository management and external actions are not required. Connector authentication is separate from Komok identity.

<a id="know-08"></a>

### KNOW-08 · Telegram bot

- Send information to Komok and receive messages through a bot using Heapy/kotbot.
- Support the same entities and permissions as the main app. Commands and interaction details remain open; other messaging channels can follow.

<a id="know-09"></a>

### KNOW-09 · Reader and reading queue

- Read articles, books and notes, queue videos, and resume from a saved position.
- Mix imported and manually added material in one queue; retain sources, annotations and reading progress.

## Learning

<a id="learn-01"></a>

### LEARN-01 · Flashcards and spaced repetition

- Store vocabulary and concept cards as entities, with a practice mini-app and combinable decks.
- Generate cards from articles, books and subtitles of films or series on the watch-later list.
- Keep editable card/deck content separate from personal review history; support portable export, sync and explicitly approved sharing. Repetition algorithms remain open.

<a id="learn-02"></a>

### LEARN-02 · Reading and language practice

- Insert a page, short passage, word exercise or course lesson into the day, linked to a learning goal.
- Track book/course progress and allocate time to finish them. Use ordinary snooze controls; no course provider is selected.

<a id="learn-03"></a>

### LEARN-03 · Progress and personal observations

- Monthly/yearly summaries of completed tasks, reading, listening, watching, topics, habits and goals.
- Keep personal achievements and imported credentials/badges distinct. Connect observations to their source records without inventing causal explanations.

## Places and memories

<a id="memory-01"></a>

### MEMORY-01 · Family photo archive

- Keep original family photos in Komok; iCloud can remain a convenient capture route.
- Suggest albums by event, person or pet, retaining dates and source information.
- Give family members their own views. Hiding a photo does not delete it or change another person's view. Ente is a reference.

<a id="memory-02"></a>

### MEMORY-02 · Photo connections

- Link a photo to people, places, dates, trips and projects; follow those links from its card.
- Filter by day + place + person. A bicycle-part photo can belong to the bicycle-upgrade project without making a copy.

<a id="memory-03"></a>

### MEMORY-03 · Places and location history

- Save map points with photos, notes and times. Record routes through an iOS app, including offline capture, or import GPS/fitness data.
- Import saved places from Google Maps and Maps.me where exports allow it.
- View visited countries, cities, roads and streets on a map, with coverage statistics and corrections. Make background capture and battery use controllable.

<a id="memory-04"></a>

### MEMORY-04 · Trips

- Connect dates, places, documents, contacts, notes and tasks to a trip.
- Ask an assistant to list known places on an island and choose which to attach. Prepare and verify an offline trip collection before departure.

<a id="memory-05"></a>

### MEMORY-05 · Family history

- A family wiki of people, relationships, biographies, events and documents connected to photos and memories.
- Let relatives browse the parts they are allowed to see. Bonsai is a reference.

## Media

<a id="media-01"></a>

### MEDIA-01 · Watch history and watch later

- Track watched and planned YouTube videos, films, series and episodes, with progress and notes.
- Put episode releases in the calendar. Send consumed videos to transcription, summaries and knowledge extraction; absorb the relevant Kotomka capabilities.

<a id="media-02"></a>

### MEDIA-02 · Media library and playback

- Store films and series, play them on web and Apple TV, and resume watching across players.
- Download torrents into the library. Keep download status, stored files and watch progress distinct.
- Use an LLM for correctable metadata. Jellyfin and Plex are references and possible reuse candidates.

<a id="media-03"></a>

### MEDIA-03 · Podcasts

- Subscribe, archive audio, track listening, transcribe, search and summarise episodes; link ideas to timestamps.
- Combine and filter feeds for another podcast player. Share selected collections through federation with explicit approval.

## Everyday life

<a id="life-01"></a>

### LIFE-01 · Recipes, meals and groceries

- Search recipes, retain sources and personal edits, plan meals and generate a reviewable grocery list.
- Combine suggested ingredients with manually added shopping items.

<a id="life-02"></a>

### LIFE-02 · Homes and possessions

- Track multiple homes/apartments, belongings and their locations, purchases, receipts, photos and documents.
- Preserve an item's history when it moves.

<a id="life-03"></a>

### LIFE-03 · Home maintenance

- Recurring tasks for filter replacement, battery charging and other upkeep, linked to the correct home and object.
- Keep completion history. Home Assistant continues to control devices.

<a id="life-04"></a>

### LIFE-04 · Finances

- Track income, expenses, receipts, budgets and historical exchange rates, with manual rate overrides.
- Show money available per day until a chosen date; recalculate after overspending. Reconcile imports without double counting.
- SMS/receipt imports are ideas; phone bridges and Apple Pay access need investigation. Records and planning do not authorise bank transactions.

<a id="life-05"></a>

### LIFE-05 · Health

- Sync Garmin and Apple Health; keep illness history, lab results, medical documents and exercise goals.
- Preserve dates, units and sources. Select and review the context given to an agent; use progress and reminders to support exercise.

## Library and common entities

<a id="shape-01"></a>

### SHAPE-01 · Extensible entities

- Built-in types plus custom types without programming. An entity can be a note, task or event at the same time, with one identity and shared access rules.
- Links, tags, annotations, private comments and duplicate handling apply across types. Coffee and tea collections are examples of custom data.

<a id="shape-02"></a>

### SHAPE-02 · Library, views and mini-apps

- Browse the same data as tables, documents, galleries, lists, maps or calendars; save filters, sorting, field selections and layouts.
- Combine collections and linked data without copying records. Organise project/topic views by type or date, with configurable type groups and collection hierarchy.
- Let an LLM build a view or mini-app, such as selecting places for a trip. It uses the common model and access rules; its changes outlive the interface.

<a id="shape-03"></a>

### SHAPE-03 · Canvas notes

- An infinite-canvas note containing cards linked to existing entities. Store layout separately from entity content.
- Automatic graph exploration remains deferred.

<a id="shape-04"></a>

### SHAPE-04 · Diagrams

- Store editable diagram source and render a preview of its exact version: flows, sequences, classes, states, data models, timelines, mind maps and charts.
- Reuse a renderer or service first, respecting private data; track a Kotlin/KMP implementation separately. Mermaid and diagrams.net are references.

<a id="shape-05"></a>

### SHAPE-05 · Files

- Store arbitrary files, preview supported formats and connect them to projects or other entities.
- Keep originals and versions independently of generated previews.

<a id="shape-06"></a>

### SHAPE-06 · Entity history

- Show a timeline of an entity's changes, revisions, corrections and their actor or source.

<a id="shape-07"></a>

### SHAPE-07 · Search and connections

- Search by keywords, location, photo, date and related entities. Follow links in both directions and combine filters.
- Apply the same permissions to results, snippets, suggestions and views.

## Storage and access

<a id="own-01"></a>

### OWN-01 · Offline use and sync

- Read and edit locally available notes, plans and selected material offline; synchronise changes later.
- Distinguish available content from catalogue-only entries; preserve meaningful concurrent edits and show unresolved conflicts.

<a id="own-02"></a>

### OWN-02 · Storage placement

- Keep a small useful subset on a laptop/phone, most data on a NAS, and selected large originals only in remote storage if desired.
- Pin, download and evict replicas independently of logical deletion. Show availability and protect the last required durable copy.

<a id="own-03"></a>

### OWN-03 · Encrypted backups and recovery

- A verifiable 3-2-1 backup policy, with encryption before upload to S3, R2 or another provider; compare storage and restore costs.
- Restore entities, links, originals, history and keys after device/NAS loss. Cover remote-only originals too; the third destination remains open.

<a id="own-04"></a>

### OWN-04 · Spaces

- Personal, family, friend and guest access with independent permissions and keys. Views cannot bypass access rules.
- Optional spaces stay out of navigation, search and notifications until explicitly unlocked with their own key. Avoid exposing their existence through ordinary UI.
- Keep identity, data-encryption and recovery keys separate; grant devices and services only the access they need.

<a id="own-05"></a>

### OWN-05 · Remote access

- Reach an instance without relying on public inbound ports or DDNS: Tailscale, Cloudflare Tunnel or a future tunnel are candidates.
- Keep the transport independent of identity and federation.

## Sharing

<a id="share-01"></a>

### SHARE-01 · Sites, blogs and feeds

- Publish selected Komok data as a blog, standalone site, RSS/Atom feed or collection shared with selected people, including items in their streams.
- Require owner approval of exact content and audience before publishing or expanding access. Private linked records and metadata stay private.

<a id="share-02"></a>

### SHARE-02 · Federation

- Exchange selected collections directly between independent instances, with no central server.
- Listen to a friend's podcast archive or recordings shared by a musician. Evaluate efficient BitTorrent-like delivery; redistribution requires its own permission.

<a id="share-03"></a>

### SHARE-03 · Portable identity

- Move hosts without losing identity, relationships or authorised subscriptions.
- Let peers verify the new endpoint; revoke old devices/hosts without widening access.

## Assistants and automation

<a id="ai-01"></a>

### AI-01 · Local and hosted models

- Support local models and all provider integrations through a common interface, with capabilities, cost and privacy controls.
- Choose a model per task. Missing capabilities need a clear fallback/error; never silently send local-only context to a hosted model.
- Develop the reusable LLM interface separately; study Spring AI, LangChain4j and other approaches.

<a id="ai-02"></a>

### AI-02 · Agents and MCP

- Expose scoped queries and actions through MCP and internal tools, using selected context and ordinary permissions.
- Agents receive neither a master decryption key nor raw database/cloud credentials. Enforce publication approval in the service.

<a id="ai-03"></a>

### AI-03 · Follow-up automation

- React to entity creation, changes, interfaces and external updates by suggesting or creating related records.
- Example: an update notice creates a linked maintenance task. Retried events must not create duplicates; imported text grants no tool permissions.

## Mail, contacts and calendars

<a id="connect-01"></a>

### CONNECT-01 · Fastmail

- Connect selected Fastmail mailboxes through JMAP; link messages, threads and attachments to people, trips, projects and tasks.
- Search downloaded mail offline and open the original in Fastmail. Preserve remote identity and avoid duplicate imports.
- Make state sync and write-back explicit; connecting an account does not authorise sending/deleting mail or whole-mailbox agent access.

<a id="connect-02"></a>

### CONNECT-02 · CardDAV contacts

- Import and explicitly enable two-way sync for selected CardDAV address books, including Fastmail contacts.
- Connect contacts to people, organisations and birthdays; review duplicates and conflicts. Keep private Komok notes out of exported contacts.
- Suggest authorised recipients in Fastmail/Gmail through the browser extension; filling a recipient does not send mail.

<a id="connect-03"></a>

### CONNECT-03 · Apple Calendar and calendar interoperability

- Use Komok events in Apple Calendar and connect Fastmail calendars; retain Google Calendar as another target.
- Support read-only subscriptions/export and explicitly enabled two-way sync as separate capabilities.
- Preserve identity, time zones, all-day dates, recurrence exceptions, reminders and cancellations. Distinguish Apple Calendar the app from iCloud hosting; require approval for invitations and publication.

## Deferred ideas

These ideas are retained but outside the current plan.

<a id="later-01"></a>

### LATER-01 · Social learning groups

Match people by technology interests, location or language to explore a subject together. Social discovery, ranking and moderation remain out of scope.

<a id="later-02"></a>

### LATER-02 · Mood modes

Choose a feed mode such as Fun, Focus/Tech or Calm/Support, with optional session signals adjusting ranking.

<a id="later-03"></a>

### LATER-03 · Automatic graph exploration

Explore an automatically laid-out relationship graph, with colour or heat maps showing distance between entities. Ordinary entity links and filters remain in scope.

<a id="later-04"></a>

### LATER-04 · Detailed time tracking

Start/stop timers, classify time spent, report/export timesheets and warn about simultaneous active tasks. Planning remains the priority.

<a id="later-05"></a>

### LATER-05 · Collaborative editors

Collaborative document/spreadsheet editing and general file editing. Ordinary offline sync does not imply these editors.

<a id="later-06"></a>

### LATER-06 · Reusable mini-app definitions

Save, reuse and possibly distribute generated mini-apps. Persistence of their data is already required; the definition format and distribution model remain undecided.
