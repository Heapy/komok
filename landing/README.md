# The Heapy / Komok website

A static landing page built from docs/landing.md. Komok is the main product;
Heapy is the Kotlin ecosystem around it. The page explains plans, knowledge,
memories and their connections through the approved Stream and Library prototype.
Product features and architecture stay in the repository.

## Build and preview

Run from the repository root:

```sh
kotlinr landing/build.main.kts
kotlinr landing/check.main.kts
kotlinr landing/serve.main.kts
```

Open http://127.0.0.1:4174/. Port 4173 is used by the separate interactive
prototype. To use another port, run `kotlinr landing/serve.main.kts 4176`.
The preview server uses the JDK 18+ static file server without external dependencies,
binds only to 127.0.0.1 and stops with Ctrl+C. It serves rebuilt files without restarting;
missing paths use the JDK's default 404 response. Docker uses the site's custom 404 page.
Kotlin 2.4.10+ uses kotlinr for scripts, separately from the
repository's Kotlin Toolchain launcher. The first build resolves the pinned
CommonMark dependency; subsequent builds can use the dependency cache.
If the default script cache is not writable, set
KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR to a writable temporary directory.

Edit copy in docs/landing.md, layout in landing/landing.html, styling in
landing/style.css and the screenshot tabs in landing/site.js. landing/build/site is generated
and ignored by Git. Each build recreates this directory with the landing, a 404 page
and local assets. Raw HTML input is trusted repository content.

## Design and screenshots

The landing inherits the prototype's restrained blue links, serif headings,
Arial interface text, thin rules and compact controls. Screenshots show the
actual prototype, with fictional sample data, rather than a separate mock UI.
The two views switch using accessible tabs with arrow, Home and End keys.
Without JavaScript, both views remain visible. Images open at full size;
on narrow screens their containers scroll horizontally to preserve legibility.

The checked-in assets/komok-stream.png and assets/komok-library.png were captured
from the current prototype at 1488 × 1058 CSS pixels, device scale 1. Refresh
these images deliberately when the approved prototype changes. The landing
has no runtime dependency on the prototype's server, framework or backend.
It needs no external fonts or services.

## Docker and CI

The Docker build uses the landing directory as its context. Generate and check
its static output first; no JVM or Kotlin compiler is included in the image:

```sh
kotlinr landing/build.main.kts
kotlinr landing/check.main.kts
docker build -t komok-landing:test landing
bash landing/smoke-test.sh komok-landing:test
docker run --rm -p 127.0.0.1:4175:80 komok-landing:test
```

The container serves on port 80 with /healthz, a real 404 response for missing
paths, and revalidation of static resources. Only generated public files and
nginx configuration enter the Docker build context; source documents are excluded
by .dockerignore.

The Landing workflow validates pull requests. On main it also publishes
linux/amd64 and linux/arm64 images to the existing public ghcr.io/heapy/komok package with landing-main and
landing-sha-<commit> tags. A manual workflow run publishes only when run against main.
The website build is independent of the Komok backend and prototype builds.

In heapy-configuration/schwifty-server, heapy_io_root uses this image behind
the existing heapy_io ingress. No DNS/tunnel, port or subpath migration is needed.
The deployment commands and rollback procedure live in that repository's README.

## Validation and publication

The check script verifies the public page set, local links, anchors, assets and
unresolved template values, plus unique feature IDs.
When changing the page, check desktop/mobile layouts, screenshot tabs and links
in the browser.

Local builds do not publish. Merging landing changes to main publishes the image;
the existing infrastructure updater follows the landing-main tag. Review the rendered
page before merging a revision intended for publication.

The first production rollout and rollback reference are recorded in
[DEPLOYMENT.md](DEPLOYMENT.md).
