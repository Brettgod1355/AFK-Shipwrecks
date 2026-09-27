# Third-party notices

Cargo Full is an independent project. It is not an official RuneLite product and is
not affiliated with Jagex.

## RuneLite

The build and launcher follow the official
[example plugin](https://github.com/runelite/example-plugin). RuneLite is a
build/runtime dependency and is not bundled in the plugin JAR. Game identifiers
(item containers, objects, varbits and interfaces) are referenced from the RuneLite
API's `net.runelite.api.gameval` classes at compile time.

## Gradle wrapper

The wrapper scripts, JAR, and properties originate from the RuneLite example
plugin, commit `5370caa0f5f6a5bba4fbb42931722ca535ad3fd5`, via
[Sidebar Favorites](https://github.com/Brettgod1355/Sidebar-Favorites), commit
`4a539621363eaacdc1b82cfd3b9e9e2fc0e7c422`. Gradle is licensed under Apache 2.0; see
`licenses/Apache-2.0.txt` and the notices retained in the scripts and wrapper. This
development tooling is not included in the plugin JAR.

## Behaviour references (no code copied)

- Cargo hold capacities for each tier and boat size were checked against the
  [Old School RuneScape Wiki](https://oldschool.runescape.wiki/w/Cargo_hold). Only the
  figures are used; no wiki text or images are included.
- The [Sailing](https://github.com/LlemonDuck/sailing) plugin by LlemonDuck
  (BSD 2-Clause) was consulted to confirm which game objects and item containers
  represent a boat's cargo hold and which crewmate line reports a full hold. Cargo
  Full's detection and alert code was written independently and none of that
  project's source is included.
