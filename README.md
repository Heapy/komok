# komok [![Build](https://github.com/Heapy/komok/actions/workflows/build.yml/badge.svg)](https://github.com/Heapy/komok/actions/workflows/build.yml)

## Local-First challenges

1. Represent every change in the system like event
2. Build event log
3. Use CRDT to merge events from different sources
4. Sync events between devices

## Project goals

- Help to collect information in a single place and connect it;
- Prioritize and schedule tasks (task can be anything, from learning and coding to resting and playing games);
- Help to focus by removing multiple sources of distraction, and consolidate them in single system that care about user, not profit.

## Features

* [Local-first](https://www.inkandswitch.com/local-first.html)
* Bookmarks
  - Firefox extension
    - Add bookmarks directly to the system
    - Suggest urls from the system
    - Save all visited pages and content automatically
      - Example: [fetching.io](http://fetching.io/)
  - Example projects
    - [https://saved.io/](https://saved.io/)
    - [https://booky.io/](https://booky.io/)
    - [https://raindrop.io/](https://raindrop.io/)
    - [https://historio.us/](https://historio.us/)
    - [del.icio.us](https://en.wikipedia.org/wiki/Delicious_(website))
      - Social Bookmarks
    - [pinboard.in](https://en.wikipedia.org/wiki/Pinboard_(website))
      - Social Bookmarks
    - [Instapaper](https://en.wikipedia.org/wiki/Instapaper)
      - Save content
      - Annotate content
    - [Pocket](https://en.wikipedia.org/wiki/Pocket_(service))
      - Save content
      - Annotate content
    - [Diigo](https://en.wikipedia.org/wiki/Diigo)
      - Annotate content
* Contacts
  - CardDAV
  - Firefox extension
    - Suggestions for **to** field in fastmail/gmail/etc
  - Add birthdays to Unified Stream
* Calendar
  - Put anything as event to calendar
  - Notifications to Unified Stream
  - Export to other systems (Google Calendar, Fastmail, Apple Calendar, etc)
  - Event types:
    - Notification - don't forget to do smth at particular time
    - Event - you'll watch cinema for 2 hours at that location
      - Add "Travel time"
* Tasks
  - Create tasks in Web UI, telegram bot, PWA
  - Task tree structure (Checkvist-like)
  - Track time (in-progress/stop buttons, notify if more than one in progress)
  - Repeated tasks (habits)
    - Display "streak" under current reminder in Unified Stream
      - Usually represent as number of green/red circles
* File store
  - Store and preview files
  - Edit files (distant future)
* Project/Topic
  - Attach different entities to project/topic
  - View and edit them in single place
    - Sort by type
      - Allow arranging types
    - Sort by date
* Expense tracker (Receipt Keeper)
  - Connect with a phone through ifttt (SMS)
  - Parse SMS and add to stream of expenses
  - Currency support (history of exchange rates)
  - Connect to Apple Pay?
* JMAP Client
  - Connect to Fastmail
* Twitter Client
* RSS Client
  - [RSSHub](https://github.com/DIYgod/RSSHub) - utilize as microservice for certain feeds
    - Eventually build own modules for common social networks, forums, websites without RSS, etc
* Episodes (Serials, shows) Calendar
* Entities tracking
  - Cinema, Films
  - Books
    - It would be cool to grab few pages or maybe chapter and display as event in the stream, so Unified Stream will "ask" you to read daily dose of book. This way it's possible to estimate books/year rare, or! allocate enough time for reading.
  - Courses
    - Similar to books, integration with coursera/etc and allocate time for making progress in course
  - Coffee (Different kinds)
  - Tea (Different kinds)
* Document collaboration tools
  - This is for far future
  - Basically CRDT-based google docs/sheets/etc
* Location tracker
  - Track user location (phone send location)
  - Import location bookmarks from Google and Maps.me
* Podcast tracker
  - Gather multiple RSS, filter and use it in any podcast App
  - Speech-to-text + Search
* Blog/Site CMS
  - See ideas I want to implement in ObjectStyle CMS
* TL-DR
  - Add notes to video
  - Speech-to-text + Search (Or just Search by subtitles, youtube-api for subtitles?)
* Timeline with all modifications for every entity (Just result of CRDT)
* Monthly/Yearly reports on read/listen/view, finished tasks, popular topics, etc
* **Unified Stream** - Core concept
  - All entities displayed in single stream of events
  - Mark important/not important accounts (twitter, instagram, youtube, etc) (id)
  - Batch all not important account updates once per day/week/month
  - Mark garbage events, add score to event (ML), filter garbage
    - User can update automatically assigned score
  - Estimate Unified Stream Event processing time (cost)
    - Video, Podcast, etc - exact time
    - Article - approximation of character count
    - Event - time + travel time
    - Task - user task estimate
  - Estimate and show total processing time of current queue
    - Use score to drop unimportant, but costly events
      - Create separate stream of **drop** events to process in free-time
  - Any event can be shared with group. Group is just abstraction on one or more Users
    - A shared event will appear in other users streams
  - Any event can have tags, tags can be set automatically by event source or ML
  - Any event can have comments (for single user it's just note)
  - Deduplication of events
    - Try to combine similar events and show them together
    - Try to find similar events in past and show them in detailed view

## Examples of similar systems

* [Jetbrains OMEA](https://www.jetbrains.com/omea/)
  - email
  - contacts
  - documents
  - files
  - blogs
  - transcripts
  - newsgroups
  - RSS feeds
  - pics
  - webpage bookmarks
  - tasks
  - instant messages
* [FriendFeed](https://en.wikipedia.org/wiki/FriendFeed)
  - Own protocol [SUP](https://en.wikipedia.org/wiki/Simple_Update_Protocol)
  - Supported Services - Check later for good one
* [Zotero](https://www.zotero.org/)
  - Save/find references
  - Organize research

## Decentralized systems

* [YaCy](https://en.wikipedia.org/wiki/YaCy)
  - Distributed search engine
* [Hubzilla](https://zotlabs.org/page/hubzilla/hubzilla-project)
  - Nomadic identity - **Research**

## License

This project utilizes two distinct licenses for different components to reflect
our philosophy and intentions:

### Libraries

All libraries within this project are shared under the **Apache License 2.0**.
We believe these libraries provide generally useful functionality and represent
a better approach compared to existing solutions on the market.
By using the Apache License 2.0, we ensure that these libraries can be freely
used in commercial projects, encouraging broader adoption and innovation.

List of libraries:

- `io.heapy.komol:komok-tech-di`
- `io.heapy.komol:komok-tech-di-lib`

### Application Code

The application code of this project is licensed under the **GNU Affero General
Public License v3.0 (AGPL-3.0)**. This choice reflects our commitment to
fostering an open and collaborative community around the core application.
By using the AGPL-3.0, we ensure that any company using this code must
contribute back to the community, promoting transparency and continuous
improvement in the project.

<picture>
  <source media="(prefers-color-scheme: dark)" srcset=".readme/dark-mode-AGPLv3_Logo.svg">
  <source media="(prefers-color-scheme: light)" srcset=".readme/light-mode-AGPLv3_Logo.svg">
  <img width="150" alt="AGPLv3 Logo" src=".readme/fallback-mode-AGPLv3_Logo.svg">
</picture>

For more detailed information on the terms and conditions of each license, please refer to the
following links:

- [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [GNU Affero General Public License v3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)

By contributing to or using this project, you agree to adhere to the terms of
the respective licenses.

