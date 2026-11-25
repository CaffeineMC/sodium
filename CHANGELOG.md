[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() improves mod compatibility, re-enables the fullscreen resolution option on Linux, and fixes several bugs.

- Optimize pitcher plant models ([#3881](https://github.com/CaffeineMC/sodium/pull/3881))
- Improve mod compatibility by correctly handling vertexIndex state
- Fix command line not being restored after NeoForge early window init ([#3803](https://github.com/CaffeineMC/sodium/pull/3803))
- Fix mixin members not being prefixed with sodium$ ([#3955](https://github.com/CaffeineMC/sodium/pull/3955))
  Note that this change may break some third-party mods. Please wait for them to update.
- Make VideoSettingsScreen trigger onClose in keyPressed to fix interaction with Iris, for example ([#3938](https://github.com/CaffeineMC/sodium/pull/3938))
- Fix render distance logic when the camera is outside the loaded chunks
- Re-enable "Fullscreen Resolution" control on Linux ([#3843](https://github.com/CaffeineMC/sodium/pull/3843))
