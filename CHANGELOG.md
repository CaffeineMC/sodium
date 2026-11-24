[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth, and CurseForge.
----------
Sodium [SodiumVersion]() adds the option to use spherical render distance and appropriate fog. Additionally, we've implemented a number of bug fixes and minor improvements, such as fixes for glitches on certain Intel GPUs. The release includes constraints for NeoForge to prevent launching in broken configurations. Vanilla graphics options are now automatically imported from Vanilla to ensure that default values are up to date by construction. Some refactoring to improve compatibility has also caused breakage with some other mods, which we hope can be resolved soon or has already been resolved during beta testing.

- Fix Fluid Culling For Non-Full Blocks With Unusual Shapes ([#3916](https://github.com/CaffeineMC/sodium/pull/3916))
- Re-enable "Fullscreen Resolution" control on Linux ([#3843](https://github.com/CaffeineMC/sodium/pull/3843))
- Source vanilla option defaults and ranges directly from vanilla options ([#3947](https://github.com/CaffeineMC/sodium/pull/3947))
- Allow rendering buffer debug outside F3 Debug Screen ([#3943](https://github.com/CaffeineMC/sodium/pull/3943))
- Make VideoSettingsScreen trigger onClose in keyPressed to fix interaction with Iris, for example ([#3938](https://github.com/CaffeineMC/sodium/pull/3938))
- Fix mixin members not being prefixed with sodium$ ([#3952](https://github.com/CaffeineMC/sodium/pull/3952))
  Note that this change may break some third-party mods. Please wait for them to update.
- Fix incorrect null value for unspecified vertex normals on NeoForge BakedQuads ([#3962](https://github.com/CaffeineMC/sodium/pull/3962))
- Fix disappeared block breaking animation ([#3958](https://github.com/CaffeineMC/sodium/pull/3958))
- Newly added compatibility metadata in NeoForge builds to prevent usage in broken configurations
- Implement option for spherical render distance fog
- Fix leaking buffer arena debug image by properly closing it on arena close
- Fix potential failure to schedule async culling task when the camera movement stops before a task is started
