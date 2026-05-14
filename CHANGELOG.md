[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() fixes a number of bugs, adds some new features, and improves memory usage. It also includes multiple improvements to lighting.

The release includes a new display of FPS percentiles which more accurately represent what you actually want to know about the FPS.
- The median (labeled p50) shows the typical frame rate ignoring the slow frames
- The 98th and 99.5th percentiles show the FPS of slow and very slow frames.
- The average, which is what Minecraft displays by default, can get pulled down by few slow frames even though otherwise the frame rate is pretty high. This leads to the average FPS being not very representative of the actual frame rate you're seeing most of the time.

The new option to sort entities quads by their closest point allows correct rendering of similar translucent entities that have specific types of otherwise wrongly sorted geometry. This is disabled by default as it incurs a performance cost.

The bug fixes include multiple crash fixes and changes to avoid retaining large memory allocations in specific scenarios.

- Fix the animation when fluid sprites are obtained directly via FluidModel ([#3630](https://github.com/CaffeineMC/sodium/pull/3630))
- Avoid retaining large allocations created when heavy quad splitting takes place
- Fix buffer overflow caused by BSP Node reuse when quads were split
- Fix wrong applicability test in BaseBiForest that would cause wrong rendering at render distances >32
- Fix post launch checks to actually run and fix duplicate module dialog ([#3624](https://github.com/CaffeineMC/sodium/pull/3624))
- Implement optional percentile fps display ([#3633](https://github.com/CaffeineMC/sodium/pull/3633), [#3642](https://github.com/CaffeineMC/sodium/pull/3642))
- Fix flat fluid lighting being too dark ([#3620](https://github.com/CaffeineMC/sodium/pull/3620))
- Significantly reduce memory consumption on the direct sorting fallback path
- Fix issues with fluid shaping in both "improved" and regular mode
- Fix #3612 by performing uniform binding on particle rendering ([#3637](https://github.com/CaffeineMC/sodium/pull/3637))
- Fixed #3603 (incorrect AO and skylight level around light sources) ([#3631](https://github.com/CaffeineMC/sodium/pull/3631))
- Add option to sort Entity Quads by Closest Point ([#3635](https://github.com/CaffeineMC/sodium/pull/3635))
- Fix Minecraft's CompactVectorArray#getZ returning the wrong value which broke some mods' rendering ([#3643](https://github.com/CaffeineMC/sodium/pull/3643))
- Change fullbright blocks to only emit block light and no sky light ([#3640](https://github.com/CaffeineMC/sodium/pull/3640))
- Fix clouds being invisible behind leaves with specific resource packs
- Refactor `ImprovedItemModelBuilder` for better mod compatibility and fixed crash from beta 3 ([#3648](https://github.com/CaffeineMC/sodium/pull/3648))

Please participate in testing this release and giving feedback on [our discord server](https://caffeinemc.net/discord).