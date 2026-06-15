[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() for Minecraft [MCVersion]() introduces asynchronous occlusion culling alongside some other small improvements and bug fixes.

This generally improves performance and avoids the frame rate dropping when the camera is moved, especially at high render distances. This feature has been in the works for a long time now, and together with the release for Minecraft 26.2, we've decided to release it. More work is planned, including improvements to the task scheduling system and optimizations to improve responsiveness.

- Implementation of Asynchronous Graph Culling and Frame-Independent Task Scheduling ([#2887](https://github.com/CaffeineMC/sodium/pull/2887))
- Fix rare issues with chunk fading
- Fix crash when using `-Dmixin.debug=true` ([#3689](https://github.com/CaffeineMC/sodium/pull/3689))
- Only make environment changes if the early window will create a gl context early ([#3697](https://github.com/CaffeineMC/sodium/pull/3697))
- Improve the presentation and wording of some video options ([#3700](https://github.com/CaffeineMC/sodium/pull/3700))
- Fix water color handling
- Fix broken block tinting by not incorrectly converting to the wrong color format
- Fix fluid color overrides not being applied ([#3729](https://github.com/CaffeineMC/sodium/pull/3729))