[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() is a release for Minecraft 26.3-rc-2. It's based on the recently released Sodium 0.9.2.

Iris is not yet compatible. If you want to use Iris, don't update Sodium until Iris receives an update.

- Port to 26.3 ([#3902](https://github.com/CaffeineMC/sodium/pull/3902))
- Re-enable "Fullscreen Resolution" control on Linux ([#3843](https://github.com/CaffeineMC/sodium/pull/3843))
- Fix a memory leak caused by recreating resources in the F3 display
- Fix issues with falling blocks
