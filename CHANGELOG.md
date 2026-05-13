[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
### Overview
Sodium [SodiumVersion]() is a backport of modern Sodium 0.8 to Minecraft [MCVersion](). This experiment has graduated to alpha status. The degree to which we commit to supporting this branch will be determined by testing and by how well it works and integrates with the ecosystem. The quality of this version is expected to further improve it gets used, we discover quirks, and receive feedback.

- Significantly improved the performance of rendering the world (up to +115%) on some computers.
- Greatly improved the rendering of transparent objects with complex models, especially when submerged in water.
- Lots and lots of improvements to the user experience in the Video Settings menu.
- Reduced latency and micro-stutter when updating chunks in the world.
- Slightly faster entity rendering, especially for transparent mobs and particles.
- Improvements for hardware and mod compatibility.
- ...And many more bug fixes and improvements...

### Using and Testing This Release
It includes the backport of our Config API and other conventions that will hopefully make it easier for mods to interact with Sodium across multiple versions. This release series doesn't get released at the same cadence as our current releases for Minecraft 26.1 and 1.21.11, and doesn't follow the same alpha/beta numbering. Mod developers can find our artifacts, such as the Config API, on [our Maven repository](https://maven.caffeinemc.net/).

Please participate in testing this release, coordinating on mod compatibility, and giving feedback on [our discord server](https://caffeinemc.net/discord).

Known incompatibilities at the time of release:
- Iris
- Voxy
- More culling, sodiumleafculling
- Create Aeronautics, Sable

### Alpha Series Changelog
- Fix BufferBuilderMixin conflicting with Iris' MixinBufferBuilder_SeparateAo when requireOverwriteAnnotations is enabled ([#3658](https://github.com/CaffeineMC/sodium/pull/3658))
- Potentially fix issues with publishing by making the buildscript more similar to that used in 1.21.11
