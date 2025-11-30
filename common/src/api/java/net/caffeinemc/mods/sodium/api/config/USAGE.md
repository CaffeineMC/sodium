# Usage of the Sodium Config API

The Sodium Config API lets mods add their own pages to the Video Settings screen, which Sodium replaces with its own screen.

If you encounter difficulties using the API, find bugs in it, or it's missing features you need, don't hesitate to contact us or make a contribution directly.

## Scope

The Sodium Config API is intended for mods that add video settings, not as a general purpose config API. For general purpose configuration, use the platform's appropriate mod list and a config library.

As a presentation API, it does not handle loading, parsing, or saving configuration data to files. It is up to your mod to handle that on its own, or use a third-party library dedicated to that task.

## Overview

Sodium redirects Minecraft's "Video Settings" screen to its own screen. Historically, third-party mods have mixed into Sodium to add buttons to their own settings pages or additional options to Sodium's pages.

With this API, these mods will not need to touch Sodium's internals anymore and should be able to operate independently of the GUI's implementation details. The API may not be able to cover all use cases where mods mixed into Sodium's options code, but it should cover most of the common ones.

Registration of options happens in two stages: Early and late. Early registration happens when Sodium initializes its own early options before the window is created. Late registration happens after the game launched. Most mods will only need to use late registration. These stages are independent and only options that are registered in the late stage will show up in the GUI.

### Features of the API

Here's a summary of the features this config API provides:

- a list of option pages per mod
  - ID, name, and version
  - a theme color
  - optionally a square monochrome icon
- a list of option groups per page
  - optionally named option groups
  - external pages that simply open a mod-defined `Screen`
- a list of options per group
- options of various types
  - `ResourceIdentifiers` as IDs
  - types: integer slider, enum, boolean, external (opens a new `Screen`)
  - value bindings through callbacks
  - dynamic values for enablement, default value, allowed values
    - dynamic values may depend on other option's values
    - cycle and declared dependency consistency checking
  - value formatters for presentation
  - value-dependent tooltips
  - flags for renderer reload and other graphics events
- all user facing strings are translatable
- storage providers are flushed after changes are applied
- early registration for mods that need to have their options available before the window is created
- entrypoint-based config registration
- declarative builder-based style

## Getting Started

### Dependency on Sodium's API

Sodium publishes its api package on a maven repository that you can depend on in your buildscript. It needs `modImplementation` on Fabric 1.21.11, and `implementation` on Fabric 1.21.12+ and NeoForge 1.21.11+.

Fabric:

```groovy
dependencies {
    // ... other dependencies
    
    // using a tagged API build 
    modImplementation "net.caffeinemc:sodium-fabric-api:0.8.0+mc1.21.11"
    // OR using a snapshot build
    modImplementation "net.caffeinemc:sodium-fabric-api:0.8.0-SNAPSHOT+mc1.21.11-pre3-build.773"
}
```

NeoForge:

```groovy
dependencies {
    // ... other dependencies
    
    // using a tagged API build 
    implementation "net.caffeinemc:sodium-neoforge-api:0.8.0+mc1.21.11"
    // OR using a snapshot build
    implementation "net.caffeinemc:sodium-neoforge-api:0.8.0-SNAPSHOT+mc1.21.11-pre3-build.773"
}
```

Make sure you have our Maven repository declared:

```groovy
maven {
    name "CaffeineMC"
    url "https://maven.caffeinemc.net/releases" // or /snapshots
}
```
Kotlin:
```kotlin
maven {
    name = "CaffeineMC"
    url = uri("https://maven.caffeinemc.net/releases") // or /snapshots
}
```

The Maven Repository has a web frontend that you can use to view the list of available versions: https://maven.caffeinemc.net

### Creating an Entrypoint

Entrypoint classes that Sodium calls to run your options registration code can be declared either in your mod's metadata file, or on NeoForge with a special annotation.

#### With a Metadata Entry

Metadata-based entrypoints use the key `sodium:config_api_user` and the value is the full reference to a class that implements the `net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint` interface.

Fabric `fabric.mod.json`:

```json5
{
    "entrypoints": {
        // ... other entrypoints
        
        "sodium:config_api_user": [
            "com.example.examplemod.ExampleModConfigBuilder"
        ]
    }
}
```

NeoForge `neoforge.mods.toml`:
```toml
[modproperties.examplemod]
"sodium:config_api_user" = "com.example.examplemod.ExampleModConfigBuilder"
```

The implementation of the entrypoint can look something like this:

```java
package com.example.examplemod;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ExampleConfigUser implements ConfigEntryPoint {
    private final OptionStorage storage = new OptionStorage();
    private final Runnable handler = this.storage::flush; // typically gets referenced many times
    
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .setIcon(ResourceLocation.parse("examplemod:textures/gui/icon.png"))
                .addPage(builder.createOptionPage()
                        .setName(Component.literal("Example Page"))
                        .addOptionGroup(builder.createOptionGroup()
                                .setName(Component.literal("Example Group")) // only if necessary for clarity
                                .addOption(builder.createBooleanOption(ResourceLocation.parse("examplemod:example_option"))
                                        .setName(Component.literal("Example Option")) // use translation keys here
                                        .setTooltip(Component.literal("Example tooltip"))
                                        .setStorageHandler(this.handler)
                                        .setBinding(this.storage::setExampleOption, this.storage::getExampleOption)
                                        .setDefaultValue(true)
                                )
                        )
                );
    }
}

// OptionStorage.java
class OptionStorage {
    private boolean exampleOption = true;

    public boolean getExampleOption() {
        return this.exampleOption;
    }

    public void setExampleOption(boolean value) {
        this.exampleOption = value;
    }

    public void flush() {
        // flush options to config file
    }
}
```

#### NeoForge: With an Annotation

Since NeoForge has the convention of using annotations for entrypoints, this option is provided as an alternative. Any classes annotated with `@ConfigEntryPointForge("examplemod")` will be loaded as config entrypoints too. Note that the annotation must be given the mod id that should be associated as the default mod for which a config is registered with `ConfigBuilder.registerOwnModOptions`. This is necessary as it's otherwise impossible to uniquely determine which mod a class is associated with on NeoForge.

```java
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;

@ConfigEntryPointForge("examplemod")
public class ExampleConfigUser implements ConfigEntryPoint {
    // class body identical to the above
}
```

### Registering Your Options

Each mod adds a page for its options, within each page there are groups of options, and each group contains a list of options. Each option has an id, a name, a tooltip, a storage handler, a binding, and a default value. There are three types of options: boolean (tickbox), integer (slider), and enum. Optionally, all types of options can be disabled, while integer and enum options can have their allowed values restricted. Those two types also require you to set a function that assigns a label to each selected value.

Some attributes of an option can be provided dynamically, meaning the returned value can depend on the state of another option. The default value, option enablement, and the allowed values can be computed dynamically. The methods for setting a dynamic value provider also require you to specify a list of dependencies, which are the resource locations of the options that the dynamic value provider reads from. Since dynamically evaluated attributes may change the state of an option, cyclic dependencies will lead to option registration failing and the game crashing.

Sodium constructs one instance of the entrypoint class, and then calls the early and late registration methods at the right time. 

## API Notes

The API is largely self-explanatory and an example is provided above. Also see Sodium's own options registration for a more in-depth example of the API's usage.

### Using `ConfigBuilder` and `ModOptions`

The `ConfigBuilder` instance passed to the registration method allows quick and easy registration of a mod's own options using `ConfigBuilder.registerOwnModOptions`. The mod's id, name, version or a formatter for the existing version, and the color theme can be configured on the returned `ModOptionsBuilder`. It's also possible to register options for additional mods using `ConfigBuilder.registerModOptions`. Which mod is the "own" mod for `registerOwnModOptions` is determined by the mod that owns the metadata-based entrypoint or the mod id passed to the `@ConfigEntryPointForge("examplemod")` annotation.

Each registered mod gets its own header in the page list. The color of the header and the corresponding entries is randomly selected from a predefined list by default, but can be customized using `ModOptionsBuilder.setColorTheme`. A color theme is created either by specifying three (A)RGB colors or a single base color with the lighter and darker colors getting derived automatically. A mod can also specify an icon with `ModOptionsBuilder.setIcon`, which takes a `ResourceLocation` pointing to a texture, which will be tinted in the theme color and rendered in its entirety as a square.

To simply switch to a new `Screen` when an entry in the video settings screen's page list is clicked, use `ConfigBuilder.createExternalPage` and add the returned page normally after configuring it with a name and a `Consumer<Screen>` that receives the current screen and switches to your custom screen.

### Using `OptionBuilder`

The storage handler set with `OptionBuilder.setStorageHandler` is called after changes have been made to the options through the bindings. This lets you flush the changes to the config file once, instead of every time an option is changed.

The tooltip set with `OptionBuilder.setTooltip` can optionally be a function that generates a tooltip depending on the option's current value. This is useful for enum options for which the description would be too long otherwise.

Optionally a performance impact can be specified with `OptionBuilder.setImpact` where the impact ranges from low to high (or "varies").

Flags set with `OptionBuilder.setFlags` control what things are reset when this option is applied. They include reloading chunks or reloading resource packs. See `OptionFlag` for the available values.

The default value set with `OptionBuilder.setDefaultValue`, or dynamically with `OptionBuilder.setDefaultProvider`, is used if the value returned by the binding does not fulfill the option's value constraint (in the case of an integer or enum option).

Disabling an option with `OptionBuilder.setEnabled(false)` shows the option as strikethrough and makes it non-interactive. Otherwise, especially with regard to value constraints, it will behave as usual.

The binding configured with `OptionBuilder.setBinding` is called when changes to the options have been made and are applied, or when the value no longer fulfills the option's constraints and is reset to the default value. It's also used to initially load a value during initialization.

### Using `? extends OptionBuilder`

Some of the attributes of an option are required and you must set them, or registration will fail. The concrete extensions of `OptionBuilder` for each of the option types have some additional methods for configuring type-specific things, some of which are also required. Notably, `EnumOptionBuilder.setElementNameProvider` and `IntegerOptionBuilder.setValueFormatter` are required in order to display these types of options. The method `setValueFormatter` for integer options takes a `ControlValueFormatter`, which simply formats a number as a `Component`. Many standard value formatters are provided in `ControlValueFormatterImpls` (not part of the API package).

Integer and enum options can have value constraints that restrict the set of allowed values the user can select. For integer options, a `Range` must be configured with `IntegerOptionBuilder.setRange` so that the slider's start, end, and step positions are well-defined. Enum options may be configured to only allow the selection of certain elements with `EnumOptionBuilder.setAllowedValues`.