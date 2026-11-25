[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() re-enables the fullscreen resolution option on Linux, fixes several bugs, and includes additional optimized models.

- Optimize pitcher plant models ([#3881](https://github.com/CaffeineMC/sodium/pull/3881))
- Improve mod compatibility by correctly handling vertexIndex state
- Fix Fluid Culling For Non-Full Blocks With Unusual Shapes ([#3916](https://github.com/CaffeineMC/sodium/pull/3916))
- Make VideoSettingsScreen trigger onClose in keyPressed to fix interaction with Iris, for example ([#3938](https://github.com/CaffeineMC/sodium/pull/3938))
- Optimize the dried ghast model ([#3815](https://github.com/CaffeineMC/sodium/pull/3815))
- Fix incorrect null value for unspecified vertex normals on NeoForge BakedQuads ([#3962](https://github.com/CaffeineMC/sodium/pull/3962))
- Re-enable "Fullscreen Resolution" control on Linux ([#3843](https://github.com/CaffeineMC/sodium/pull/3843))
