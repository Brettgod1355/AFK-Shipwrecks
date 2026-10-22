# Notes for the helper bot

Helper-Bot (https://github.com/Brettgod1355/Helper-Bot) adds this file to Claude's instructions for the PR audit
and issue triage here. The full guide for players is the wiki: https://github.com/Brettgod1355/AFK-Shipwrecks/wiki

## What the plugin is

AFK Shipwrecks is a RuneLite plugin for Sailing's salvaging: a hold count and full-hold timer, wreck timers, hook
reminders, parking boxes, inventory sorting boxes and a sidebar of salvaging spots. It never clicks anything for
the player and sends nothing anywhere. It works only on the player's own boat.

## Known quirks players report

- **The hold count drifts.** The game only sends the hold's contents while the hold is open; between openings the
  plugin keeps a tally. Opening the cargo hold resyncs it at once. If the tally says full while the crew keep
  bringing salvage in, the overlay asks the player to open the hold.
- **The first countdown is off.** The rate starts from the wiki's tables and learns from what arrives; the first
  countdown is within about 20% and tightens over the next quarter hour.
- **Wreck timers are ceilings.** "Sinks in ≤ 2:40" means no later than that: a wreck's clock starts when anyone
  first salvages it, which the client cannot see.
- **The plugin cannot say when a wreck will rise next.** It shows how long the player has waited; the salvaging
  worlds respawn more often.
- **Nothing on someone else's boat** except one notification when their crew say the hold is full. Barracuda
  Trials, trawling and courier cargo are not tracked.
- **Two counts or two sets of wreck outlines** come from the Sailing plugin doing the same thing. Only one count:
  Sailing plugin, *Cargo Hold Tracking* → turn off **Item Count**, or AFK Shipwrecks, *Overlay* → turn off
  **Count on the hold**. Only one set of outlines: Sailing plugin, *Salvaging* → turn off **Highlight Active
  Locations** and **Highlight Inactive Locations**, or AFK Shipwrecks, *Wrecks and parking* → **Mark wrecks** to
  *Area* or *Off*.
- **Routes need Shortest Path.** The sidebar's **Route** button and **Auto** spots hand the spot to the Shortest
  Path plugin; without it everything else still works.
- **A mark comes back with two clients open.** RuneLite loads settings only when a client starts, so restart the
  other client after unmarking an item.
- **Notifications do not show while the game window is focused** unless RuneLite's own setting *Send
  notifications when focused* is on (off by default). The plugin sends them either way.
- **Idle logout.** Fully AFK still means touching the client: the overlay warns when the hold will outlast the
  game's idle timer, and a notification comes a minute before.

## What the plugin does not do, on purpose

- Anything the Plugin Hub rules or Jagex's third-party client rules forbid: clicking or acting for the player,
  menu changes that save clicks, automation of any kind. Say politely that it is not allowed, never that it is
  planned.
- Fetching anything from the internet beyond RuneLite's own world list.

## Replying

- Use the setting and section names players see in the plugin's settings and the wiki, never Java names.
- When the answer is a setting, link the wiki page that covers it when one does.
