[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() for Minecraft [MCVersion]() makes some improvements to the options UI, introduces a new automatic publishing system, fixes some bugs, and introduces some small performance optimizations.

- Fix issues with improved item meshing and add documentation ([#3581](https://github.com/CaffeineMC/sodium/pull/3581))
- Update neoforge to stable
- Workflow and Gradle based publishing setup with changelog system ([#3575](https://github.com/CaffeineMC/sodium/pull/3575), [#3622](https://github.com/CaffeineMC/sodium/pull/3622))
- refactor: Lazily construct expensive ShapeComparisonCache object ([#3607](https://github.com/CaffeineMC/sodium/pull/3607))
- Invalidate cached search index sources to be properly indexed after language switch
- Jump to the mod's options when its header in the page list is clicked
- Show that an entry in the options page list leads to an external page more clearly
- Hide options search bar clear button when search bar is empty
- Implement Alt shortcuts and ESC discard behavior ([#2769](https://github.com/CaffeineMC/sodium/pull/2769)) ([#3604](https://github.com/CaffeineMC/sodium/pull/3604))
- Reset button for options with hold-shift to reset functionality
- Fix rare translucency sorting crash when repeatedly interacting with specific chunks with some resource packs ([#3609](https://github.com/CaffeineMC/sodium/pull/3609))
- Configurable filtering mode ([#3618](https://github.com/CaffeineMC/sodium/pull/3618))
- Fix animated sprite mipping ([#3619](https://github.com/CaffeineMC/sodium/pull/3619))
- Add caching to `GlStateManager#glViewport` ([#3309](https://github.com/CaffeineMC/sodium/pull/3309))
