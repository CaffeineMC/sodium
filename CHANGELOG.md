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
- Fix crash "centroid is null," "allQuads is null," and "geometryPlanes is null" ([#3757](https://github.com/CaffeineMC/sodium/pull/3757), [#3805](https://github.com/CaffeineMC/sodium/pull/3805))
- Fix crashes resulting from unsafe concurrency in async culling "ArrayIndexOutOfBoundsException" ([#3756](https://github.com/CaffeineMC/sodium/pull/3756))
- Fix incorrect GlyphVertex
- Improved mod compatibility by using occlusion culling from camera render state ([#3764](https://github.com/CaffeineMC/sodium/pull/3764))
- Optimize checks for immediate presentation in RSM ([#3768](https://github.com/CaffeineMC/sodium/pull/3768))
- Cache max draw size in MultiDrawBatch instead of scanning every frame ([#3773](https://github.com/CaffeineMC/sodium/pull/3773))
- Reimplement enhanced entity sorting. The option was ineffective as of 26.2, but should now work again.
- Fix sections getting stuck fully "faded" as the color of the sky, often after explosions ([#3785](https://github.com/CaffeineMC/sodium/pull/3785))
- Fix crash "sorter is null" ([#3787](https://github.com/CaffeineMC/sodium/pull/3787))
- Internal code quality improvements and cleanup
- Fix non-terrain block lighting ([#3800](https://github.com/CaffeineMC/sodium/pull/3800), [#3795](https://github.com/CaffeineMC/sodium/pull/3795))
- Remove extra ABGR conversion to fix incorrect falling block coloration ([#3798](https://github.com/CaffeineMC/sodium/pull/3798))
- Fix crash when rendering very many sections on Vulkan by making the indirect context ring buffer dynamically sized
- Fix command line not being restored after NeoForge early window init ([#3803](https://github.com/CaffeineMC/sodium/pull/3803))
