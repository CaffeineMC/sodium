[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() for Minecraft [MCVersion]() introduces asynchronous occlusion culling alongside other small improvements and fixes for crashes to improve stability.

This generally improves performance and avoids the frame rate dropping when the camera is moved, especially at high render distances. This feature has been in the works for a long time now, and together with the release for Minecraft 26.2, we've decided to release it. More work is planned, including improvements to the task scheduling system and optimizations to improve responsiveness.

- Implementation of Asynchronous Graph Culling and Frame-Independent Task Scheduling ([#2887](https://github.com/CaffeineMC/sodium/pull/2887))
- Fix rare issues with chunk fading
- Fix crash when using `-Dmixin.debug=true` ([#3689](https://github.com/CaffeineMC/sodium/pull/3689))
- Only make environment changes if the early window will create a gl context early ([#3697](https://github.com/CaffeineMC/sodium/pull/3697))
- Improve the presentation and wording of some video options ([#3700](https://github.com/CaffeineMC/sodium/pull/3700))
- Fix water color handling
- Fix broken block tinting by not incorrectly converting to the wrong color format
- Fix fluid color overrides not being applied ([#3729](https://github.com/CaffeineMC/sodium/pull/3729))
- Fix crash "getResources is null" ([#3752](https://github.com/CaffeineMC/sodium/pull/3752))
- Fix crash "centroid is null," "allQuads is null," and "geometryPlanes is null" ([#3757](https://github.com/CaffeineMC/sodium/pull/3757))
- Fix crashes resulting from unsafe concurrency in async culling "ArrayIndexOutOfBoundsException" ([#3756](https://github.com/CaffeineMC/sodium/pull/3756))
- Improved mod compatibility by using occlusion culling from camera render state ([#3764](https://github.com/CaffeineMC/sodium/pull/3764))
- Optimize checks for immediate presentation in RSM ([#3768](https://github.com/CaffeineMC/sodium/pull/3768))
- Cache max draw size in MultiDrawBatch instead of scanning every frame ([#3773](https://github.com/CaffeineMC/sodium/pull/3773))
- Fix crash "sorter is null" ([#3787](https://github.com/CaffeineMC/sodium/pull/3787))
- Internal code quality improvements and cleanup
