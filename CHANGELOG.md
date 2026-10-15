[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() implements a number of bug fixes and minor improvements.

- Show macOS fullscreen menu visibility option on the Sodium options screen
- Fix Fluid Culling For Non-Full Blocks With Unusual Shapes ([#3916](https://github.com/CaffeineMC/sodium/pull/3916))
- Use SDL message box instead of TFD ([#3923](https://github.com/CaffeineMC/sodium/pull/3923))
- Optimization for shelf mushroom models ([#3927](https://github.com/CaffeineMC/sodium/pull/3927))
- Fix compatibility with Neoforge 26.3.0.65-beta and remove platform-specific LevelExtractorMixin ([#3948](https://github.com/CaffeineMC/sodium/pull/3948))
- Source vanilla option defaults and ranges directly from vanilla options ([#3947](https://github.com/CaffeineMC/sodium/pull/3947))
- Allow rendering buffer debug outside F3 Debug Screen ([#3943](https://github.com/CaffeineMC/sodium/pull/3943))
- Make VideoSettingsScreen trigger onClose in keyPressed to fix interaction with Iris, for example ([#3938](https://github.com/CaffeineMC/sodium/pull/3938))
