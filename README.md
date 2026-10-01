# AFK Salvaging

A RuneLite plugin for Sailing. Park at a shipwreck, put your crew on the hooks, and it tells you
how long until the cargo hold is full, then tells you loudly when it is.

It grew out of Cargo Hold Alert, and everything that plugin did is still here: the full-hold
alert, the banner, the early warning and the cargo counter. What is new is the timer and the
things an AFK salvager actually needs to hear about: a hook standing empty, the crew having
stopped, and the fact that your own hook does not restart itself.

## What it does

- **Countdown to a full hold.** "Hold full in ~52 min", with the clock time if you like. It is
  built from the game's published salvage chances for your wreck, hooks and level, then corrected
  by what your crew actually bring in, so it shows a number from the first tick and gets better
  as it goes. Under ten minutes it counts in seconds.
- **Knows about wrecks sinking.** A wreck lasts about a fixed time once anyone starts salvaging
  it (the wiki's figures, from one minute for a small wreck to four for a merchant). The plugin
  tracks the wrecks in reach of your hooks, shows the longest one has left at most, and when none
  is up the timer pauses and says so rather than lying.
- **Knows about salvaging worlds.** On the official salvaging worlds (596 and 597, and any you
  add) every wreck site is being worked, so a sunk wreck near you is replaced quickly. Elsewhere
  your crew can sit idle for a long time. The plugin measures how much of the time a wreck has
  been up and folds that into the estimate. If you have been waiting a while on an ordinary
  world it mentions the salvaging worlds, once.
- **Counts you too.** Salvage you hook yourself goes to your inventory first; the plugin counts
  it as bound for the hold and knows you stop when your inventory is full or the wreck sinks.
  Salvage you withdraw to sort is never counted, and sorting shows its own little countdown.
- **Hook reminders.** Step off your hook to sort and forget to hand it over, and after a short
  grace period it reminds you: "A salvaging hook is empty. Assign a crewmate to it." If nobody
  aboard can take the hook (a crewmate needs enough deckhandiness for it) and a wreck is up, it
  asks you to click the hook instead, because unlike your crew you do not restart by yourself.
- **Crew stopped.** Crew salvage on your level, boosted or not. If it drops below what the wreck
  needs they stop; the plugin says so.
- **Full-hold alert.** Sound, tray popup, screen flash, focus, whatever you set up in RuneLite,
  plus a big red banner and an optional early warning and repeat. As before.
- **Idle logout.** If the hold will take longer to fill than you have left before the game logs
  you out for idling, it shows that too. It cannot press a key for you.
- **Knows where the wrecks are.** A sidebar panel lists all 29 salvaging hotspots, filtered by
  wreck, with the Sailing level each needs and where it sits from the nearest port, and a Tips
  button at the bottom that explains the boxes, the buttons and the rest of this README in short. Every one has
  a Map button, which centres the world map on it, and a Route button, which hands it to the
  Shortest Path plugin to draw the way there. Without that plugin it tells you to install it.
- **Marks them on the world map.** Hover a marker and it tells you which salvage, what level, and
  where. Green means you have the level.
- **Boxes on the water that say where to park.** A yellow box around each wreck for where a hook
  can reach it, and a green box where a hook reaches two wrecks at once. Park so one of your hooks
  sits in the green box and it lights up and says "Parked". Each has its own switch and colour.
  Read the section below before trusting them to the tile.

Everything comes from game state your client already has. No web requests other than RuneLite's
own world list, no accounts, nothing sent anywhere, and it never clicks anything for you. It does
not need any other plugin; Shortest Path is optional and only used if you press Route.

## The overlay

A small panel at the top of the game while you are on your own boat and a wreck site is in view
(or whenever you are aboard, if you set "Show overlay" to always; alerts show either way):

```
Hold full in        ~52 min
                   at 14:32
Cargo hold          188/240
Hooks    Jenkins, Jolly Jim
Wrecks   2 up (Merchant) · last sinks in ≤ 1:20
```

The "~" means the rate still rests mostly on the published tables; it goes away after about 25
salvages have been seen. Other lines you may see:

- `open the cargo hold once to start`: it needs one real count to work from.
- `Waiting for a wreck · 1:20 so far` and `Left to salvage · 34 min`: nobody can salvage until a
  wreck rises; the second line is what remains once one does.
- `Hooks  Jenkins · 1 empty (no crewmate can use it)`: a hook is standing empty.
- `Crew stopped  Sailing level too low (needs 87)`.
- `Hold  full by the tally; open it to check`: the running tally says full, but nothing has
  confirmed it. If the crew keep bringing salvage in, the tally was high: it becomes
  `Hold  tally has drifted; open it to resync` and stays there, without alerting again, until you
  open the hold or a crewmate says it is full.
- `Timer  crew do not seem to be salvaging`: far more salvage was expected than has arrived. Check
  the hooks are really in range of the wreck. The last countdown stays up while it works this out.

## Settings

**Timer**: show timer (turning it off hides the countdown lines; the status lines, such as waiting
for a wreck, stay); show clock time; count salvage you hooked; salvaging worlds (default
`596, 597`); salvaging world tip.

**Reminders**: three RuneLite notifications you can shape separately (hook empty with a crewmate
free; your hook is idle; crew stopped because your level is too low), the grace period before the
first reminder (15 s by default; using the hold buys a little more, settling in to sort shortens
it) and how often to repeat.

**Cargo full** and **Early warning**: as before. **Banner**: as before, plus a colour for the
reminder banner. **Overlay**: when to show it (near wrecks, or always aboard), the cargo counter,
the hooks line, the wrecks line and text size.

**Salvage spots**: show the sidebar; mark spots on the world map; wreck reach boxes and their
colour (yellow); double spot boxes (off, only pairs of wrecks that are both up, or every pair of
sites in view with the sunk ones dimmer) and their colour (green); and whether to label the double
spot boxes.

## Salvage spots and double spots

The sidebar and the map markers use the same list of hotspots RuneLite's own World Map plugin
draws its salvaging icons from, so they agree with the icons already on your map. Each spot is
described from the nearest port by sailing distance, for example "Barracuda salvage, 25 tiles
south-west of Ruins of Unkah", because six Barracuda spots called "Barracuda salvage" would not
help anyone. The world map can only be moved while it is open and nothing lets a plugin open it
for you, so if you press Map with the map closed the plugin remembers the spot and jumps to it the
moment you open the map yourself. Route posts the spot to the Shortest Path plugin over RuneLite's
plugin message bus. If Shortest Path is not installed, or is installed but switched off, the sidebar
says so in red and the same line appears in your chat, with what to do about it.

The boxes are worked out, not looked up. A hook works any wreck within its reach, and reach is
measured as a square around the wreck: that square is the yellow box. Where two yellow boxes
overlap, a hook parked in the overlap works both wrecks: that overlap is the green box, labelled
with the two wrecks. The game measures reach from the hook rather than the boat (a game update
changed it to that), so the boxes are about where a hook has to sit, not the whole boat. Your own
hooks are known, so when one of them is inside a green box the box goes solid and says "Parked";
nudge the boat until it does. The honest caveat: the reach is taken as 8 tiles from other plugins'
observations and has not been confirmed from the game's own code, so a box edge may be a tile off.
If your hook sits in a box and only one wreck is being worked, tell me and I will fix the number.
With "All sites" the green boxes also appear around sunk wrecks, since the next wreck rises on the
same tile, so you can park before it does; sunk wrecks' yellow boxes are drawn dimmer for the same
reason.

## What it can and cannot know

Here is the honest version, because a countdown invites more trust than a counter.

1. **The hold count.** The game only sends the hold's contents while the hold interface is open.
   Between openings the plugin keeps a tally: one for each crewmate line *"Managed to hook some
   salvage!"*, one for each Sailing XP drop that matches a crewmate's share (a crewmate earns you
   10% of the wreck's XP per point of deckhandiness, so the size of the drop says who earned it;
   this is how Cabin Boy Jenkins, who only says "Wooo", is counted), and your own deposits and
   withdrawals from your inventory changing. The tally is remembered per boat. Opening the hold
   resyncs it instantly.
2. **The rate.** It starts from the wiki's success tables for your wreck, hook and Sailing level
   and learns a correction from what actually arrives, remembered per wreck between sessions.
   Expect the first countdown to be within about 20%; it tightens over the next quarter hour.
3. **Wreck timers are ceilings.** A wreck's clock starts when *any* player first salvages it,
   which your client cannot see. "Sinks in ≤ 2:40" means no later than that, counted from the
   moment your own hooks started working it.
4. **Respawns.** When a wreck sinks the next one rises somewhere in the area, not necessarily
   next to you. The plugin cannot predict when your site refills; it measures how often a wreck
   has been up and shows how long you have waited. That is why the salvaging worlds are worth it.
5. **Your own hook stops.** When the wreck you are on sinks you stop and do not restart, unlike
   your crew. The plugin tells you; it cannot click for you.
6. **Crewmate stats** come from the game's crew table, with the wiki's figures as a fallback and
   a middling guess for anyone it cannot place. A keg of whirlpool surprise is taken into account.
7. **Only your own boat.** On someone else's boat you get one notification when their crew say
   the hold is full, and nothing else: no timer, no counter, no reminders. Barracuda Trials,
   trawling and courier cargo are not tracked.
8. **Fully AFK still means touching the client** before the game's idle logout. The overlay
   warns when the hold will outlast it.
9. **Things that need confirming in game.** The player animations for salvaging and sorting, the
   crew assignment values for the two sloop hooks, and the hook's reach (taken as 8 tiles, which the
   double salvage spot boxes also rest on) come from other plugins' observations and the wiki rather
   than from the game's code. If you find a case where the plugin is consistently wrong, tell me what
   you were doing and I'll chase it.

## Install

Search for **AFK Salvaging** in the RuneLite Plugin Hub.

To run it from source, clone the repo and use `./gradlew run` for a dev client or
`./gradlew test` for the tests. In IntelliJ, run `AfkSalvagingPluginTest`.

## Found a bug? Want something?

Open an issue: <https://github.com/Brettgod1355/Cargo-Hold-Alert/issues>

## License

BSD 2-Clause. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Not affiliated with Jagex or
RuneLite.
