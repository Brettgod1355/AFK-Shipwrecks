# Third-party notices

AFK Salvaging (formerly Cargo Hold Alert) is an independent project. It is not an official
RuneLite product and is not affiliated with Jagex.

## RuneLite

The build and launcher follow the official
[example plugin](https://github.com/runelite/example-plugin). RuneLite is a
build/runtime dependency and is not bundled in the plugin JAR. Game identifiers
(item containers, objects, NPCs, animations, varbits, database tables and interfaces)
are referenced from the RuneLite API's `net.runelite.api.gameval` classes at compile time.

## Gradle wrapper

The wrapper scripts, JAR, and properties originate from the RuneLite example
plugin, commit `5370caa0f5f6a5bba4fbb42931722ca535ad3fd5`, via
[Sidebar Favorites](https://github.com/Brettgod1355/Sidebar-Favorites), commit
`4a539621363eaacdc1b82cfd3b9e9e2fc0e7c422`. Gradle is licensed under Apache 2.0; see
`licenses/Apache-2.0.txt` and the notices retained in the scripts and wrapper. This
development tooling is not included in the plugin JAR.

## Behaviour references (no code copied)

- Game figures were checked against the [Old School RuneScape Wiki](https://oldschool.runescape.wiki):
  cargo hold capacities per tier and boat size ([Cargo hold](https://oldschool.runescape.wiki/w/Cargo_hold)),
  shipwreck levels, lifetimes, salvaging and sorting XP, spawn mechanics and the crew and player roll
  cadence ([Shipwreck salvaging](https://oldschool.runescape.wiki/w/Shipwreck_salvaging) and the
  individual shipwreck pages, whose success charts give the per-hook chance out of 256 at levels 1
  and 99), hook tiers and their deckhandiness requirements
  ([Salvaging hook](https://oldschool.runescape.wiki/w/Salvaging_hook)), crewmates' deckhandiness
  and the crew size per level ([Crew Management](https://oldschool.runescape.wiki/w/Crew_Management)),
  and the official salvaging worlds ([World](https://oldschool.runescape.wiki/w/World)). Only the
  figures are used; no wiki text or images are included.
- The [Sailing](https://github.com/LlemonDuck/sailing) plugin by LlemonDuck (BSD 2-Clause) was
  consulted to confirm which game objects represent a boat's cargo hold and salvaging hooks, which
  crewmate line reports a full hold, which varbits hold the crew slots and their assignments, and
  how a crewmate's stats are read from the game's crew table. This plugin's detection and alert
  code was written independently and none of that project's source is included.
- The [Salvaging AFK Timer](https://github.com/silly-build/salvaging-afk-timer) plugin by
  silly-build (BSD 2-Clause) was consulted to confirm the player animations for salvaging and
  sorting, the shipwreck and stump objects, and the crystal extractor's XP. This plugin's
  estimation, tracking and overlay code was written independently and none of that project's
  source is included.
