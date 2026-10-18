# A connected place for everyday life

Komok starts with one person's real problems. Planning the day, remembering what to buy, finding a photograph, keeping a useful idea and preparing for a trip should work together. Family members can use the same archive with their own access and views.

The product should be useful without programming. Ready-made interfaces cover common activities. Language models help organise unfamiliar data and create views when a ready-made interface is not enough.

## One object, many connections

A photograph can connect to a person, a place, a day and a project. A photograph of a bicycle part can belong to “Upgrade the bicycles.” It remains the same photograph wherever it appears.

Entities have stable identities and extensible attributes. An entity can implement several interfaces: a note can also become a task, a bookmark can have reading progress, and a saved place can belong to a trip. Imported data can declare its interfaces; inferred interfaces need validation before they confer behaviour. Links are first-class entities with a source, a target, a relationship type and their own metadata.

Built-in entities cover people, pets, organisations, places, days, projects, goals, tasks, events, notes, links, photos, albums, files, messages, feeds, articles, books, podcasts and media. Domain modules add recipes, possessions, financial records and health records. This is an extensible vocabulary, not a requirement to implement every module at once.

## A stream that respects prerequisites

The stream brings forward relevant entities and reasons to act. Important items can also arrive as notifications. Opening an item leads to its entity and the related context.

A trip without dates should offer “Choose travel dates.” “Book accommodation” becomes eligible only when the dates are known. Eligibility and ranking are separate: becoming ready does not guarantee an immediate place at the top.

Any entity can be postponed until a date or an event. It stays available in search while absent from the stream. Calendar follow-ups may be relative to events such as returning from a trip. Recurring tasks and habits keep completion history; detailed time tracking is deferred in favour of planning.

Ranking can use explicit importance, urgency, available time, source feedback and context. Starred and read items can inform source scores. Low-priority sources may be grouped into a digest. Users need to understand and correct the result; the product is not optimising for time spent scrolling. The daily briefing is a useful ready-made view, not a mandatory cadence for all information.

## Find context, then choose a view

Search by words, location, date, image or related entity. On a photograph, follow the person, place, day or project; combine filters such as day + place + person. Folder organisation may be imported, but should not be necessary to navigate the product.

Tables, calendars, maps, collections and an infinite canvas are views over the same entities. Table definitions select interfaces and fields, and can filter, join or combine result sets. A canvas note links to existing cards rather than copying their contents. An automatically arranged graph exploration interface is deferred.

Mini-apps use the common model and the same access rules as the rest of Komok. “Show the known places on this island and let me choose which belong to the trip” should create a useful interface and persist its resulting links. Saving and distributing mini-app definitions is a later design decision.

## A second brain that notices what is new

The archive should become a living wiki. After a video is watched or an article is read, extracted meanings can be saved automatically and corrected manually. This is a semi-manual workflow, without a separate approval step for every extracted item.

New material is compared with the existing knowledge collection. Komok can show the new ideas, additional evidence, qualifications or contradictions, and offer a summary, a quiz, a relevant passage or the full original. “Already saved” and “actually understood” are different states; the system must not claim to know what a person remembers.

Every extracted item retains provenance and links to the relevant source fragment. Facts, ideas and observations are provisional labels. The extraction taxonomy, prompts, evaluation and novelty algorithm require experiments. A fluent summary is not evidence that an assertion is true.

Goals are entities too. An assistant can propose and create connected tasks, reading material and practice activities. Flashcards are reusable entities, with a dedicated mini-app and combinable decks. Cards may teach vocabulary or concepts from articles, books and the subtitles of something on the watch-later list.

## Keep a life, not just a work queue

The intended scope includes a family photo archive, family history, films and series, podcasts, watched videos, recipes, groceries, several homes and their possessions, finances, health, language learning and reading. Relationships make these parts useful together: a receipt belongs to a purchase, the purchase belongs to a home, and its maintenance task appears when due.

News reports from multiple feeds or channels about the same event become one card with source links and an explanation of what matters. Conflicting reports remain visible. RSS, Telegram, monitored websites and eventually other social sources are inputs; Komok itself is not becoming a social network.

The browser archive automatically captures visits and useful page content. Firefox, Chrome and Safari extensions are required. Bookmarks, address-bar suggestions, project context and reading position should let a tab be closed without losing its purpose.

Web, iOS and Apple TV have different jobs. iOS is essential for capture and location history; Apple TV and web serve the media library. Telegram through Heapy's kotbot and voice input are additional channels. Home Assistant remains responsible for controlling the home.

Fastmail, CardDAV and Apple Calendar are important integrations. Email belongs alongside its people, tasks and projects; contacts connect to birthdays and recipient suggestions; calendar events remain usable in existing calendar apps. Mail, address books and calendars use separate adapters and permissions. Export is useful, but is not a substitute for explicitly enabled two-way synchronisation. See [the integration features](features.md#mail-contacts-and-calendars).

## Ownership is part of the experience

Keep the useful small subset on a laptop for the plane, most of the archive on a NAS, and selected large material only in remote storage if desired. A catalogue entry and an available original are different things. Show what will actually work offline.

Family views can hide a photo without deleting it. Views do not grant or restrict permission by themselves; spaces and access policies do that. Synchronised replicas improve availability; independently retained backups protect history.

Publish selected data as a blog, site, feed or shared collection only after the owner approves the exact content and audience. Updates that expose new content require approval too. Linking to a private entity must not silently publish it.

Federation has no central server. Friends can exchange selected podcast collections or a musician's recordings, with portable identity and efficient delivery. A transfer protocol such as BitTorrent is a candidate, not a settled dependency. Access and permission to redistribute must remain explicit.

## A product that grows an ecosystem

The target is Kotlin throughout the product: Kinetica on the frontend, a Kotlin backend framework and reusable KMP libraries. Existing software and services can deliver a feature now; a separate technical backlog records opportunities for a Kotlin port or independent implementation.

Local models and external LLM providers are equally important. One provider-neutral interface should expose real capability differences, without making one vendor's API the product model. See [the ecosystem plan](technology.md).

## Boundaries of this edition

This is a big-picture specification, not a release promise. Mood modes, a social network, automatic graph exploration and detailed time tracking are deferred. Weather providers, the exact peer-to-peer protocol, extraction algorithms, mini-app reuse and final storage engines need separate decisions. No historical project names are carried forward.
