# heapy.io deployment

Published on 18 October 2026 from the owner-approved working tree.
The first release was built and published manually from local sources before
those sources were committed. Subsequent main-branch releases use
.github/workflows/landing.yml.

- URL: https://heapy.io/
- Image: ghcr.io/heapy/komok:landing-main
- Release tag: ghcr.io/heapy/komok:landing-release-20261018
- Multi-platform digest: sha256:42399f977da5e8a21155fd138531d5532750e98bf7396f1158d7a7a6db8beecd
- Platforms: linux/amd64 and linux/arm64
- Host: schwifty
- Compose directory: /home/pi/heapy-configuration/schwifty-server
- Service/container: heapy_io_root / heapy.io
- Server configuration backup: compose.yaml.before-komok-landing.MoUC2u

Only the root website container was recreated. The existing ingress and tunnel
remain in place, including the kotlin_jobs and kinetica upstreams. The same
image reference was updated in the local heapy-configuration checkout, preserving
its pre-existing edits to other services.

## Verification

- Kotlin build and existing static-site checks passed.
- actionlint passed for the new workflow; Compose validation passed.
- Docker smoke checks passed on the amd64 build host and the arm64 production
  host, before switching traffic. They cover nginx syntax, health, page content,
  JS/CSS/images and real 404s for nested paths and private documentation paths.
- The service reached Docker healthy status after recreation.
- Public HTTPS returned 200 for /, /healthz, /style.css, /site.js and both
  prototype images; a missing nested path returned the themed 404.
- The public homepage content matches the reviewed landing and Chrome loaded
  its new title, copy and Stream panel. A subsequent screenshot/tab interaction
  attempt lost the browser connection; no fresh production screenshot is claimed.
  The same interaction was verified in the local design QA before packaging.

GHCR uses the existing public Heapy/komok package, already connected to this
repository, under landing-specific tags. No backend tags were changed and no
registry credentials were saved on either server. The initially created private
komok-landing package was removed after switching to the existing public package.

## Rollback

Change only heapy_io_root.image back to ghcr.io/heapy/heapy.io:main in the server
Compose file, then run:

```sh
docker compose up -d --no-deps --pull never heapy_io_root
```

The previous image was retained on the host. Its image ID at rollout was
sha256:13cfdedde31dfb426dbbaafb01073bd4716bb861c352c935c873e1ecd4e63d53.
The backup is an additional reference; avoid restoring the entire Compose file
over unrelated changes made after the deployment.
