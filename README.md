# Cargo Full

**Never sail on with a full hold again.**

Cargo Full watches your boat's cargo hold while you sail and tells you the moment
it fills up: a RuneLite notification (sound, tray popup, screen flash), a big
**CARGO HOLD FULL** banner over the game, and a live counter of used slots.

## What you get

- **Cargo alert.** A RuneLite notification when the hold is full. Click the gear
  next to the setting to pick the sound (including your own custom sound file),
  tray popup, screen flash, and whether to pull the client into focus.
- **Early warning.** Optionally alert when a chosen number of free slots remain,
  so you can head for port before the last salvage is wasted.
- **Repeat.** Optionally send the alert again every so often while the hold stays
  full, for the times you missed the first one.
- **Banner.** A flashing banner at the top of the game. It stays until you make
  room, or hides after a timer of your choosing. Size, colour and flashing are yours to
  change.
- **Counter.** `Cargo hold 37/40` while you are aboard, coloured from green to red
  as it fills.

## How it works

- The used count comes straight from the hold's item container, which the game
  updates whenever cargo moves in or out.
- The capacity comes from the cargo hold built on your boat: each tier holds a
  different amount on a raft, skiff, or sloop. Opening the cargo hold once lets the
  plugin read the game's own numbers, which then take priority.
- A crewmate saying *"The cargo hold is full"* and the matching game message also
  trigger the alert, so you are still told even when the plugin cannot count for
  you (for example when you are crewing on someone else's boat).

Everything is read from the game state already sent to your client. The plugin
makes no network requests and never acts on your behalf.

## Good to know

- Only cargo holds are tracked. The trawling net has its own storage and is not
  covered yet.
- The counter only shows while you are aboard a boat. Alerts fire wherever you are
  when the hold fills.
- Stackable items take one slot for the whole stack, exactly as in the game.

## Development

```bash
./gradlew test
```

```bash
./gradlew run
```

`run` starts a RuneLite developer client with the plugin loaded. In IntelliJ, run
`CargoFullPluginTest` from `src/test`.

## Feedback

Problems or ideas? Open an issue at
[github.com/Brettgod1355/Cargo-Full/issues](https://github.com/Brettgod1355/Cargo-Full/issues).

## License

BSD 2-Clause. See [LICENSE](LICENSE) and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
