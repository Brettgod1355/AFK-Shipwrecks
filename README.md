# Cargo Hold Alert

A RuneLite plugin for Sailing. It tells you when your cargo hold is full, loudly,
so you stop wasting salvage.

If you've ever parked at a shipwreck, let the crew get on with hooking salvage,
tabbed out for a bit and come back to find they'd been shrugging at a full hold
for ten minutes, this is for you.

## What it does

- Fires a notification the moment the hold fills up. Sound, tray popup, screen
  flash, pull the client to the front, whatever you've got set up in RuneLite.
  Custom sound files work too.
- Puts a big red CARGO HOLD FULL banner over the game and keeps it there until
  you make room, or for as long as you tell it to.
- Shows a small `Cargo hold 154/160` counter while you're aboard, going from
  green to red as it fills, so you can see it coming.
- Optional early warning when you're down to your last few slots. It has its own
  sound switch, so it can nag quietly and save the loud one for actually full.
- Can repeat the alert every so often while the hold stays full, for the
  properly AFK.

Everything comes from game state your client already has. No web requests, no
accounts, nothing sent anywhere, and it never clicks anything for you.

## Settings

**Cargo full**

- Notification. The RuneLite notification for a full hold. Hit the gear to pick
  sound, tray, flash and focus.
- Repeat every. Re-send the alert every so many seconds while the hold stays
  full. 0 means once per fill.

**Early warning**

- Warn with slots left. Alert when this many free slots remain. 0 turns it off.
- Play sound. Untick to keep the early warning silent. The banner, tray popup
  and flash still happen.

**Banner**

- Show banner and Flash banner do what they say.
- Banner size, 50% to 300%.
- Hide banner after, in seconds. 0 keeps it up until the hold has space again.
- Banner colour.

**Counter**

- Show cargo counter.
- Counter size, 50% to 300%. It's a small box that hugs its text and only grows
  while an alert is up.

## How it counts

Here's the honest version, because it matters.

The game only sends your client the hold's contents while the cargo hold
interface is open. Close it and the client hears nothing about what your crew
are stuffing in there. So the plugin does two things:

1. Whenever the hold is open, or the game sends it for any other reason, it takes
   the real count. That's the truth and it always wins.
2. In between, it keeps a running tally. Every time a crewmate says *"Managed to
   hook some salvage! I'll put it in the cargo hold."* that's one more. Cabin Boy
   Jenkins only ever says "Wooo", so he's counted from the Sailing XP he hands
   you. Your own deposits and withdrawals are picked up from your inventory
   changing, including the quick Deposit on the hold itself that never opens the
   interface.

The capacity comes from the cargo hold built on your boat. Each tier holds a
different amount on a raft, skiff and sloop: a basic hold on a raft takes 20, a
rosewood hold on a sloop takes 240. Opening the hold once lets the plugin read
the game's own number, which it then trusts over its table.

On top of all that, if a crewmate says *"The cargo hold is full"* you get the
alert no matter what the tally thinks. That's also what fires when you're crewing
on someone else's boat and can't count their hold at all.

The tally is remembered per boat, so it survives logging out and restarting the
client.

## Things worth knowing

- First run: the counter needs one real look at the hold to start from. Open the
  cargo hold once and it'll follow from there.
- Only cargo holds. The trawling net has its own storage and isn't tracked, yet.
- Stackables take one slot for the whole stack, same as in the game.
- The tally can drift if something goes in or out that the plugin can't see.
  Opening the hold fixes it instantly. If you find a case where it's
  consistently off, tell me what you were doing and I'll chase it.

## Install

Search for **Cargo Hold Alert** in the RuneLite Plugin Hub.

To run it from source, clone the repo and use `./gradlew run` for a dev client
or `./gradlew test` for the tests. In IntelliJ, run `CargoFullPluginTest`.

## Found a bug? Want something?

Open an issue: <https://github.com/Brettgod1355/Cargo-Hold-Alert/issues>

## License

BSD 2-Clause. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Not affiliated with Jagex or
RuneLite.
