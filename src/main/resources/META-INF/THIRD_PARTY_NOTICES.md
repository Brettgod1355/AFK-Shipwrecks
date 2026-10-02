# Third-party notices

AFK Salvaging (formerly Cargo Hold Alert) is an independent project. It is not an official
RuneLite product and is not affiliated with Jagex.

## RuneLite

The build and launcher follow the official
[example plugin](https://github.com/runelite/example-plugin). RuneLite is a
build/runtime dependency and is not bundled in the plugin JAR. Game identifiers
(item containers, objects, NPCs, animations, varbits, database tables and interfaces)
are referenced from the RuneLite API's `net.runelite.api.gameval` classes at compile time.

## Data taken from RuneLite's World Map plugin (BSD 2-Clause)

The salvaging hotspot list in `SalvagingSpot.java` (29 world points and the eight salvage names)
is taken from RuneLite's own World Map plugin, and the mooring list in `Mooring.java` (61 dock
names and world points, also used to describe where each spot is) is taken from the same plugin.
Both are in
[runelite/runelite](https://github.com/runelite/runelite), `runelite-client`, version 1.13.1,
`net/runelite/client/plugins/worldmap/`:

- `SalvagingSpotLocation.java`: Copyright (c) 2026, Sam Szotkowski <https://github.com/samszotkowski>.
  All rights reserved.
- `MooringLocation.java`: Copyright (c) 2025, coopermor <https://github.com/coopermor>.
  All rights reserved.

Both files are licensed under the BSD 2-Clause License, the same text as this project's
[LICENSE](LICENSE), which requires that these copyright notices be reproduced; they are, here and
in the copy of this file shipped inside the plugin JAR. The data was transcribed into this
project's own enum, with the "where" descriptions computed from it; no source code was copied.

## Gradle wrapper

The wrapper scripts, JAR, and properties originate from the RuneLite example
plugin, commit `5370caa0f5f6a5bba4fbb42931722ca535ad3fd5`, via
[Sidebar Favorites](https://github.com/Brettgod1355/Sidebar-Favorites), commit
`4a539621363eaacdc1b82cfd3b9e9e2fc0e7c422`. Gradle is licensed under Apache 2.0; see
`licenses/Apache-2.0.txt` and the notices retained in the scripts and wrapper. This
development tooling is not included in the plugin JAR.

## Behaviour references (no code copied)

- Game figures were checked against the [Old School RuneScape Wiki](https://oldschool.runescape.wiki),
  whose content is licensed [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).
  This plugin uses only the numbers, which are facts about the game rather than the wiki's
  writing; the wiki is credited as their source all the same:
  cargo hold capacities per tier and boat size, and the list of items the hold accepts that
  `HoldWhitelist` starts from ([Cargo hold](https://oldschool.runescape.wiki/w/Cargo_hold), with
  [Cannonball](https://oldschool.runescape.wiki/w/Cannonball) and
  [Repair kits](https://oldschool.runescape.wiki/w/Repair_kits) for the ship cannonballs and kits it links
  to, item ids from the wiki's item data, read 2026-10-02; the dragon chainshot and incendiary
  cannonballs, which that table lacks, were added from RuneLite's own item list),
  shipwreck levels, lifetimes, salvaging and sorting XP, spawn mechanics and the crew and player roll
  cadence ([Shipwreck salvaging](https://oldschool.runescape.wiki/w/Shipwreck_salvaging) and the
  individual shipwreck pages, whose success charts give the per-hook chance out of 256 at levels 1
  and 99), hook tiers and their deckhandiness requirements
  ([Salvaging hook](https://oldschool.runescape.wiki/w/Salvaging_hook)), crewmates' deckhandiness
  and the crew size per level ([Crew Management](https://oldschool.runescape.wiki/w/Crew_Management)),
  the official salvaging worlds ([World](https://oldschool.runescape.wiki/w/World)), and the Sailing
  level and quest each dock needs ([Mooring point](https://oldschool.runescape.wiki/w/Mooring_point)
  and [Last Light](https://oldschool.runescape.wiki/w/Last_Light), read 2026-10-02). The same
  salvaging page's update history is where "double salvage spots" is named as a game feature. Only
  the figures are used; no wiki text or images are included.
- The [Sailing](https://github.com/LlemonDuck/sailing) plugin by LlemonDuck (BSD 2-Clause) was
  consulted to confirm which game objects represent a boat's cargo hold and salvaging hooks, which
  crewmate line reports a full hold, which varbits hold the crew slots and their assignments, and
  how a crewmate's stats are read from the game's crew table, and its wreck highlight was looked at
  when the double salvage spot boxes were designed. This plugin's detection, alert and double-spot
  code was written independently and none of that project's source is included.
- The [Shortest Path](https://github.com/Skretzo/shortest-path) plugin (BSD 2-Clause) was read, at
  its commit `038a190` of 2026-10-01, to learn the plugin messages it listens for: namespace
  `shortestpath`, names `path` and `clear`, and the `target` key. `ShortestPathMessages.java` builds
  those messages; nothing of that project is included and it is not a dependency.
- The [Salvaging AFK Timer](https://github.com/silly-build/salvaging-afk-timer) plugin by
  silly-build (BSD 2-Clause) was consulted to confirm the player animations for salvaging and
  sorting, the shipwreck and stump objects, and the crystal extractor's XP. This plugin's
  estimation, tracking and overlay code was written independently and none of that project's
  source is included.
