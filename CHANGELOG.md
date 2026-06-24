[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
### Overview
Sodium [SodiumVersion]() is a backport of modern Sodium 0.8 to Minecraft [MCVersion]().

- Significantly improved the performance of rendering the world (up to +115%) on some computers.
- Greatly improved the rendering of transparent objects with complex models, especially when submerged in water.
- Lots and lots of improvements to the user experience in the Video Settings menu.
- Reduced latency and micro-stutter when updating chunks in the world.
- Slightly faster entity rendering, especially for transparent mobs and particles.
- Improvements for hardware and mod compatibility.
- ...And many more bug fixes and improvements...

### Using and Testing This Release
It includes the backport of our Config API and other conventions that will hopefully make it easier for mods to interact with Sodium across multiple versions. This release series doesn't get released at the same cadence as our current releases for Minecraft 26.1 and 1.21.11, and doesn't follow the same alpha/beta numbering. Mod developers can find our artifacts, such as the Config API, on [our Maven repository](https://maven.caffeinemc.net/).

Please participate in testing this release, coordinating on mod compatibility, and giving feedback on [our discord server](https://caffeinemc.net/discord). In the thread in #testing-builds we have more information. Report any issues you may have in the thread.

Known incompatibilities at the time of this release:
- Create Aeronautics works as of 1.3.0
- Sable works as of 2.0.0
- Veil works as of 4.1.2
- Iris works as of 1.8.13 (unreleased on platforms at the time of this release)
- Voxy does not work
- sodiumleafculling does not work
- EBE animations don’t work

### Alpha Series Changelog
- Fix BufferBuilderMixin conflicting with Iris' MixinBufferBuilder_SeparateAo when requireOverwriteAnnotations is enabled ([#3658](https://github.com/CaffeineMC/sodium/pull/3658))
- Potentially fix issues with publishing by making the buildscript more similar to that used in 1.21.11
- Fix the corrupted config screen to show up properly
- Revert "Update texture light coords math to match Vanilla (#3311)" as this was mistakenly applied to 1.21.1

### Beta Series Changelog
- Added the display of fps percentiles. This gives a more accurate idea of the typical frame rate and lets you identify how smooth it is.
- Fix crash when using `-Dmixin.debug=true` ([#3689](https://github.com/CaffeineMC/sodium/pull/3689))
- Added checks to prevent crashes when using the vertex writing fast path ([#3716](https://github.com/CaffeineMC/sodium/pull/3716))
- Only make environment changes if the early window will create a gl context early ([#3697](https://github.com/CaffeineMC/sodium/pull/3697))
- Improve the presentation and wording of some video options ([#3700](https://github.com/CaffeineMC/sodium/pull/3700))
- Fix crash "getResources is null" ([#3752](https://github.com/CaffeineMC/sodium/pull/3752))
