[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() for Minecraft [MCVersion]() updates to Minecraft 26.2, and adds asynchronous occlusion culling. 

This is the first version to *experimentally* support Vulkan. To access it, use the Graphics API option in video settings.

- Updated to Minecraft 26.2
- Moved all rendering to Mojang's Blaze3D API
- Added Asynchronous Graph Culling and Frame-Independent Task Scheduling ([#2887](https://github.com/CaffeineMC/sodium/pull/2887))
- Improve the presentation and wording of some video options ([#3700](https://github.com/CaffeineMC/sodium/pull/3700))
