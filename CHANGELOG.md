[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() includes a new terrain buffer allocation system designed to reduce the number of buffers that get allocated and resized as chunks are loaded and unloaded. Please help us by testing this version in various scenarios, comparing frame rate stability, and reporting any issues that may arise. Feedback and testing results are welcome in the testing channel on [our discord server](https://caffeinemc.net/discord).

Other changes include improvements to our item meshing, optimizations for item rendering, and numerous bug fixes. This release also adds support for FRAPI on NeoForge.

- Prevent misalignedment in item models by removing UV_SHRINK where necessary ([#3831](https://github.com/CaffeineMC/sodium/pull/3831))
- Offset overlay layers of optimized item models to prevent z-fighting
- Optimize the dried ghast model ([#3815](https://github.com/CaffeineMC/sodium/pull/3815))
- Implementation of a Incrementally Defragmenting Auto-Sizing Multi-Arena Allocator ([#3634](https://github.com/CaffeineMC/sodium/pull/3634))
- Support FRAPI on NeoForge ([#3818](https://github.com/CaffeineMC/sodium/pull/3818))
- Fix buffer overflow in Kernel32.getModuleFileName
- Use the correct atomic operations on NativeBuffer.ALLOCATED
- Ensure that VKIndirectContext.addCommand allocates enough memory
- Use clearenv() to delete environment variables
- Cleanup code around checking the IME status on Windows
- Restore the fast quad encoder for entity-format buffers ([#3848](https://github.com/CaffeineMC/sodium/pull/3848))
- Handle resizing the section time buffer to fix [#3809](https://github.com/CaffeineMC/sodium/issues/3809)
- Backport fix from 26.3 Snapshot 6 for buffer recycling to restore inventory item rendering performance

Thank you to all the contributors who added to this release!

Alpha series changelog:
- Fixed crash when opening video settings
- Disabled automatic enabling of the arena buffer debug widget
- Implement override/overlay priority in the graphics options Config API ([#3866](https://github.com/CaffeineMC/sodium/pull/3866))
