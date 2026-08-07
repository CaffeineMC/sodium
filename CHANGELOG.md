[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() fixes potential minor bugs.

- Fix buffer overflow in Kernel32.getModuleFileName
- Use the correct atomic operations on NativeBuffer.ALLOCATED
- Use clearenv() to delete environment variables
- Implement override/overlay priority in the graphics options Config API ([#3866](https://github.com/CaffeineMC/sodium/pull/3866))
