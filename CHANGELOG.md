[ReleaseTag]() is automatically replaced with the release tag, e.g. mc26.1-0.8.9
[MCVersion]() is automatically replaced with the minecraft version, e.g. 26.1
[SodiumVersion]() is automatically replaced with the sodium version, e.g. 0.8.9
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Sodium [SodiumVersion]() fixes some bugs and improves the wording of some video options.

- Fix crash when using `-Dmixin.debug=true` ([#3689](https://github.com/CaffeineMC/sodium/pull/3689))
- Only make environment changes if the early window will create a gl context early ([#3697](https://github.com/CaffeineMC/sodium/pull/3697))
- Improve the presentation and wording of some video options ([#3700](https://github.com/CaffeineMC/sodium/pull/3700))
- Fixes the fabric version of sodium overriding all fluid BlockTintSources while rendering ([#3729](https://github.com/CaffeineMC/sodium/pull/3729))
- Fix block tinting by not incorrectly converting to the wrong color format
- Fix crash "getResources is null" ([#3752](https://github.com/CaffeineMC/sodium/pull/3752))
- Fix the command line not being restored after NeoForge early window init ([#3803](https://github.com/CaffeineMC/sodium/pull/3803))
