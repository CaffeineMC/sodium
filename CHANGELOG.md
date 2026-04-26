[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() fixes bugs and makes some miscellaneous improvements.  It also updates to the latest NeoForge version.

- Fixed vanilla performance regression in item models when using high resolution texture packs ([PR](https://github.com/CaffeineMC/sodium/pull/3551) and [port to 1.21.11](https://github.com/CaffeineMC/sodium/pull/3581))
- Better error messages for option override/overlay conflicts
- Only show amd workarounds message on windows ([PR](https://github.com/CaffeineMC/sodium/pull/3553))
- Improved down-facing inner fluid face heuristic ([PR](https://github.com/CaffeineMC/sodium/pull/3552))
