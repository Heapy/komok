# komok [![Build](https://github.com/Heapy/komok/actions/workflows/build.yml/badge.svg)](https://github.com/Heapy/komok/actions/workflows/build.yml)

## About

Komok is a personal information system for plans, knowledge and memories. Tasks,
notes, photos, places and media share one connected model: a trip brings together
its dates, saved places, photographs and things to do. A stream surfaces what
needs attention; a library lets you browse, search and organise the same data.

The aim is to make this useful without programming. Local and hosted language
models help organise information, extract knowledge and create views over your
data. You control where it lives, what is available offline and what gets shared.

Komok is in development. Its long-term direction is Kotlin throughout, with
Kinetica for the frontend and reusable Kotlin libraries developed in
[Kotomi](https://github.com/Heapy/kotomi). Existing tools can deliver features while that ecosystem grows.

See the [feature catalogue](docs/features.md),
[proposed architecture](docs/architecture.md) and [documentation](docs/README.md)
for the intended scope and current implementation baseline.

## Build

Komok consumes `io.heapy.kotomi:kotomi-*` Maven artifacts. Until those artifacts
have a public release, build the pinned Kotomi revision into Maven Local first:

```sh
git clone https://github.com/Heapy/kotomi.git ../kotomi
git -C ../kotomi checkout "$(cat kotomi.ref)"
(cd ../kotomi && ./ktc-plugins sync && ./publish-local)
```

If Kotomi is already checked out, use that checkout for the last two commands.
The `kotomi.ref` commit and `libs.versions.toml` version are updated together.
CI builds the same pinned revision before building Komok.

```sh
./ktc-plugins sync
./ktc-plugins verify
./kotlin build --platform jvm
./kotlin test --platform jvm
./kotlin check koverCheck -m komok-app
```

## Run and package

```sh
./kotlin run -m komok-app
./kotlin do installDist -m komok-app
./kotlin do distTar -m komok-app
./kotlin do distZip -m komok-app
```

## Website

The public website is maintained in [Heapy/heapy-landing](https://github.com/Heapy/heapy-landing).
It serves the ecosystem story at https://heapy.io/ and Komok at https://heapy.io/komok/.
Product copy remains in [docs/landing.md](docs/landing.md); the website source,
screenshots, container image and publication workflow live in the landing repository.

## License

This project utilizes two distinct licenses for different components to reflect
our philosophy and intentions:

### Libraries

All libraries within this project are shared under the **Apache License 2.0**.
We believe these libraries provide generally useful functionality and represent
a better approach compared to existing solutions on the market.
By using the Apache License 2.0, we ensure that these libraries can be freely
used in commercial projects, encouraging broader adoption and innovation.

Reusable technology libraries are maintained in [Kotomi](https://github.com/Heapy/kotomi).

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
