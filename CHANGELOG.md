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

### Using and Building on This Release
It includes the backport of our Config API and other conventions that will hopefully make it easier for mods to interact with Sodium across multiple versions. This release series doesn't get released at the same cadence as our current releases for Minecraft 26.2 and 26.1, and doesn't follow the same alpha/beta numbering. Mod developers can find our artifacts, such as the Config API, on [our Maven repository](https://maven.caffeinemc.net/).

Mod compatibility:
- Create Aeronautics works as of 1.3.0
- Sable works as of 2.0.0
- Veil works as of 4.1.2
- Iris works as of 1.8.13
- Not compatible with any version of "Sodium Options API", which is not affiliated with us, and any mods that depend on it.

Some other mods may still be incompatible with this release. If your modpack doesn't work with this release, downgrade to the version of Sodium it was built with and choose appropriate versions of other mods. New or updated modpacks are encouraged to use the latest version of Sodium and compatible mods.