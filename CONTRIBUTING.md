# Contributing

Thanks for helping with Create Aeronautics: Landing Legs. This page explains how to build the mod and how to send changes.

## Before you start

- **Small fix?** Open a pull request.
- **Big feature or behaviour change?** Open an issue first, so we can agree on the design before you write code.
- Read the [glossary](docs/CONTEXT.md). Code, comments, lang text and issues use its terms (Ship, Leg, Leg Mount, Limb, Reach, Ride Height, Gear Up, Leveling, ...).

## Ground rules

- Be kind. The [Code of Conduct](CODE_OF_CONDUCT.md) applies to issues, pull requests and discussions.
- Everything is in English: code, comments, commits, issues.
- One topic per issue or pull request. Unrelated refactors, reformatting or dependency bumps go in their own pull request, or not at all.
- No new required dependencies, and no bundled mods or libraries, without an issue agreed first.
- Keep the mod's style: Create-like look, glossary names, no features that only work on a client or only on a server.
- Code made with AI tools is fine, but you must understand it and test it in game before you send it. Untested pull requests are closed.
- Do not open pull requests only to change versions, wording or formatting.
- Security problems go through [SECURITY.md](SECURITY.md), not a public issue.

## Build and run

You need **JDK 21 or newer**. Gradle uses a Java 21 toolchain.

```sh
./gradlew build        # builds build/libs/legworks-<version>.jar
./gradlew runClient    # dev client
./gradlew runServer    # dev server (run/server)
./gradlew runData      # regenerates src/generated (lang, blockstates, recipes)
```

`runClient` and `runServer` also load the `dev` source set (dev-only commands and test tools). They are not in the release jar.

Lang files, blockstates and recipes in `src/generated/` are generated. Do not edit the JSON by hand: change `LegworksLang` or the classes in `dev.legworks.data`, then run `./gradlew runData` and commit the result.

## Code style

- Java 21. Match the style of the code around your change.
- No unnecessary comments. Write a comment only when it tells the reader something the code can't.
- Use the glossary terms in names and player-facing text.
- Keep changes focused. One topic per pull request.

## Frozen IDs (save compatibility)

Worlds store some strings. **Never rename or remove them**, or existing worlds lose blocks, items or settings:

- the mod id `legworks`
- block, item, block entity, data component and display source registry names
- block state properties
- NBT keys and data component field names
- config keys

The full list is in [docs/save-format.md](docs/save-format.md). Code constants live in `src/main/java/dev/legworks/LegworksIds.java`. Adding new ids is fine. If you really must change a saved format, add a migration and say so clearly in the pull request.

## Testing

There is a scripted test harness (headless server suites, client screenshot scripts). See [tools/README.md](tools/README.md).

At minimum, before a pull request:

1. `./gradlew build` passes.
2. You tested your change in game (client, and dedicated server if it touches networking or sync).
3. For physics changes, run the regression suites from the harness and compare with the results in `tools/results/`.

## Pull requests

1. Fork the repo and create a branch from `main`.
2. Keep the pull request small and about one thing.
3. Describe **what** changed, **why**, and **how you tested it** (which scenes, client or server, screenshots for visual changes).
4. Add a line to `CHANGELOG.md` under `[Unreleased]`.
5. Be ready for review comments.

## Review and merge

- Only the maintainer (@SenDeliver) reviews and merges. `main` accepts pull requests only; squash merge; release tags are made by the maintainer.
- Expect a first answer within about a week. Pull requests with no reply to review comments for 30 days may be closed, you can reopen later.
- Accepted changes land in the next release and get a line in `CHANGELOG.md` with your GitHub name.
- The maintainer may decline a change that does not fit the mod's goals, even if the code is fine. Open an issue first for anything big to avoid wasted work.

By contributing, you agree that your work is released under the [MIT License](LICENSE).
