# Landing direction

Komok is the main product; Heapy is the Kotlin ecosystem around it.
Use the approved Stream and Library prototype in ../prototypes/stream as the
visual reference. The owner prefers compact, information-rich TTRSS/Wikipedia
styling and wants the high-level product idea to be immediately understandable.
Use actual prototype screenshots, restrained typography, thin rules and blue
links. Keep implementation details out of the public page; visible copy is English.

Preserve the Kotlin Markdown-to-HTML build. Edit docs/landing.md for narrative
copy and keep technical/product planning documents in the repository.
Refresh screenshots deliberately from the approved prototype. Clearly label the
product as in development and screenshot content as sample data.

Build and check using landing/build.main.kts and landing/check.main.kts from the
repository root, then verify desktop/mobile rendering and tabs in the browser.
Local builds remain local. The Landing CI publishes main to GHCR; infrastructure
follows that image tag. Publishing or deploying work requires owner authorization.
