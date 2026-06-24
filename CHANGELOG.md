[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() for Minecraft [MCVersion]() improves stability by fixing a number of crashes and other bugs.

- Fix hand rendering glitches that happened in specific cases ([#3751](https://github.com/CaffeineMC/sodium/pull/3751))
- Fix crash "getResources is null" ([#3752](https://github.com/CaffeineMC/sodium/pull/3752))
- Fix panorama screenshots crashing ([#3761](https://github.com/CaffeineMC/sodium/pull/3761))
- Fix crash "centroid is null," "allQuads is null," and "geometryPlanes is null" ([#3757](https://github.com/CaffeineMC/sodium/pull/3757))
- Fix crashes resulting from unsafe concurrency in async culling "ArrayIndexOutOfBoundsException" ([#3756](https://github.com/CaffeineMC/sodium/pull/3756))
- Fix incorrect GlyphVertex
