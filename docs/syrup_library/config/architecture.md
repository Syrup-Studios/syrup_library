---
title: Config architecture
description: Current config architecture and release compatibility rules for Syrup Library configs.
---

# Config architecture

This note records the current 0.3.0 config API and its generic model.

## Baseline map

The current API has these public layers:

| Layer | Main types | Role |
| --- | --- | --- |
| Declaration | `ConfigSpec`, `ConfigSection`, `ConfigContainer` | Define ordered sections and values before registration. |
| Values | `ConfigValue` and `ConfigType` | Store defaults, declared type, limits, and validation. |
| Registry | `SyrupConfigManager`, `RegisteredConfig` | Own config paths, registration, initial load, and reload. |
| State | `ConfigSnapshot`, `ConfigState`, `RestartRequirement` | Provide immutable views and restart-aware effective values. |
| Diagnostics | `ConfigLoadResult`, `ConfigIssue`, `ConfigIssueSeverity` | Report load status and per-path issues. |
| File format | `ConfigLoader`, `DefaultJson5Writer` | Read, validate, preserve schema order, fill missing values, and write JSON5. |

## Generic model

The generic model keeps one concrete, final `ConfigValue<T>` abstraction and
exposes small convenience
factories on the existing declaration containers:

```java
bool(key, defaultValue, description)
integer(key, defaultValue, minimum, maximum, description)
string(key, defaultValue, description)
stringList(key, defaultValue, description)
enumValue(key, defaultValue, description)
```

The enum factory infers `E` from the default value. `ConfigType<T>` is the codec
contract. It contains a decoder, encoder, and
normalizer, plus immutable choices and optional element type metadata. Mutable
custom values must be copied by the normalizer. The core uses that normalizer
at every boundary, including defaults, decode, update, encode, and snapshots.

The data flow is:

```text
ConfigSpec / ConfigSection -> ConfigValue<T> (type, constraints, metadata)
file -> JSON5 document -> type.decode -> value.validate -> candidate snapshot
     -> RegisteredConfig publishes one ConfigState -> ConfigSnapshot reads
editor registry -> local drafts -> shared validation -> atomic save -> same state path
```

`RegisteredConfig` owns one volatile state reference. Reload and update writers
are synchronized. Each state contains configured, active, and startup snapshots.
Restart rules run in one place in `RegisteredConfig`. Readers use one published
state; a snapshot never converts file data or re-runs constraints.

`VALUE.get()` follows the value's spec to its one registered owner. Before
registration it returns the schema default. It does not search a global registry.
`registered.get(VALUE)` and `snapshot.get(VALUE)` are also available.

The declaration:

```java
ConfigValue<Integer> radius = gameplay.integer("radius", 16, 1, 128, "Search radius.");
```

State ownership moved out of `ConfigSpec`; entry descriptions are no longer
copied into schema tree nodes.

Loader integration stays at the edge. The three existing client bridges register
screens. `SyrupConfigManager.create(Path)` runs without a loader. Its lazy singleton
uses the existing `Platform` only to locate the config directory. No new platform
interface was needed. Core classes do not import Minecraft GUI classes.

The small load, save, and update result records remain. They describe different
public operations and share `ConfigIssue`. Tree organization and safe file IO
also remain separate from entry definitions. These are distinct responsibilities,
not another hierarchy for each primitive type.

## Required behavior

Missing values use the schema default and are filled into the file. A present
invalid value is strict: it is rejected and does not become a default, clamped
value, or partial update. On initial load, the default remains active. On a
later reload, the previous configured, active, and startup snapshots stay unchanged. A load
candidate is published only when all declared values pass. File-level parse
failure also keeps the previous valid snapshot.

Snapshots remain immutable and internally consistent. Restart-required values
keep their startup value effective while exposing the latest valid configured
value. These guarantees are part of the compatibility contract.

## Release evidence

Syrup has no GitHub release or tag, but it does have published artifacts:

- [Modrinth project versions](https://api.modrinth.com/v2/project/9tBNzmjo/version) lists nine listed versions: eight `0.2.0+...` builds and one `0.1.1+1.20.1-fabric` build.
- [Maven metadata](https://maven.syrupstudios.net/releases/net/syrupstudios/syrup_library/maven-metadata.xml) lists `0.1.0+1.20.1-fabric`, `0.1.1+1.20.1-fabric`, and the `0.2.0` builds.
- The [published 0.2.0 Fabric directory](https://maven.syrupstudios.net/releases/net/syrupstudios/syrup_library/0.2.0%2B1.20.1-fabric/) contains the jar and sources jar.

Inspection of the published `0.2.0+1.20.1-fabric` jar and sources shows
`RegisteredConfig.reload`, `initialResult`, `spec`, `path`, `snapshot`,
`configuredSnapshot`, and `startupSnapshot`. It does not contain the later
update/save result types, owner metadata, or client config screens. Keep the
published 0.2.0 consumers on that version. Version 0.3.0 removes the typed
value wrappers and requires consumers to migrate to the generic API and rebuild.

Validation routes through `ConfigValue`; representation conversion routes
through `ConfigType`. The client editor remains in the existing config package,
with three loader bridge files (Fabric, Forge, and NeoForge).

`ConfigEditorRegistry.register(type, adapter)` registers an editor for a custom
type. Built-in types use built-in adapters. A type with no adapter uses the
read-only fallback, which preserves view and save validation without guessing
how to edit an unknown value. An adapter creates controls through
`create(SyrupConfigScreen, ...)`; shared screen code exposes `widget(W)` for
adding an `AbstractWidget`.

The bundled `json5-java` 3.0.0 parser has a remaining exponent lexer issue for
some manually written scientific notation. Generated numeric output, including nested values and missing-value fills, uses
plain decimal values. Manually written scientific notation can still be affected. This is a dependency
limitation and is separate from the generic config model.

## MidnightLib comparison

[MidnightLib's official source](https://github.com/TeamMidnightDust/MidnightLib/blob/multiversion/src/main/java/eu/midnightdust/lib/config/MidnightConfig.java)
uses a single annotation-based class. `init` reflects annotated fields and
loads JSON. Load failure calls `write`, and `writeChanges` serializes mutable
fields directly. It uses `<modid>.json`, a global config instance map, and GUI
annotations.

Syrup can stay compact without copying that design. Its schema-first API,
generic handles, JSON5 comments and order, diagnostics, immutable snapshots, and
previous-valid-config retention are the guarantees that consumers rely on.
