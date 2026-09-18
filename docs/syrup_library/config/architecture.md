---
title: Config architecture and compatibility
description: Baseline, refactor scope, and compatibility rules for Syrup Library configs.
---

# Config architecture and compatibility

This note records the config API baseline and the generic model. It is
an implementation reference for the completed refactor.

## Baseline map

The published 0.2.0 API has these public layers:

| Layer | Main types | Role |
| --- | --- | --- |
| Declaration | `ConfigSpec`, `ConfigSection`, `ConfigContainer` | Define ordered sections and values before registration. |
| Values | `ConfigValue` and typed value classes | Store defaults, declared type, limits, and validation. |
| Registry | `SyrupConfigManager`, `RegisteredConfig` | Own config paths, registration, initial load, and reload. |
| State | `ConfigSnapshot`, `ConfigState`, `RestartRequirement` | Provide immutable views and restart-aware effective values. |
| Diagnostics | `ConfigLoadResult`, `ConfigIssue`, `ConfigIssueSeverity` | Report load status and per-path issues. |
| File format | `ConfigLoader`, `DefaultJson5Writer` | Read, validate, preserve schema order, fill missing values, and write JSON5. |

Six type dispatch sites were removed across loader parsing, update validation,
writer metadata, screen initialization, screen metadata, and screen save. The
runtime writer's Java-value switch was also removed. Generic JSON representation
branches remain necessary for JSON5 output.

## Generic model

The generic model keeps one concrete, sealed `ConfigValue<T>` abstraction and
exposes small convenience
factories on the existing declaration containers:

```java
bool(key, defaultValue, description)
integer(key, defaultValue, minimum, maximum, description)
string(key, defaultValue, description)
stringList(key, defaultValue, description)
enumValue(key, defaultValue, description)
```

The enum factory infers `E` from the default value. Existing typed factories return deprecated compatibility wrappers. They remain available to protect
the published ABI and source compatibility. No new factory should add a second
validation rule or state path.

`ConfigValue<T>` permits the seven published typed wrapper classes. Those
wrappers remain for ABI compatibility; custom values use the concrete generic
factory and do not require a new subclass. `ConfigType<T>` is the codec
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

A declaration before the refactor:

```java
IntConfigValue radius = gameplay.intValue("radius", 16, 1, 128, "Search radius.");
```

The generic declaration:

```java
ConfigValue<Integer> radius = gameplay.integer("radius", 16, 1, 128, "Search radius.");
```

No published class was deleted. The seven classes now delegate to descriptors
and constraints. Their old constructors and metadata accessors remain. The
loader's per-type parser, update serialization round trip, writer range switch,
and three editor dispatch chains were removed. State ownership moved out of
`ConfigSpec`; entry descriptions are no longer copied into schema tree nodes.

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
published base methods and classes. Treat update/save APIs, owner metadata, and
GUI integrations as feature-only until they are deliberately released.

The current source keeps all seven old typed factory families as compatibility
wrappers. Validation routes through `ConfigValue`; representation conversion routes through `ConfigType`. The physical class count does not decrease: the
core adds `ConfigType` and `ConfigConstraint`, and the client adds the editor registry. The client
editor remains in the existing config package, with three loader bridge files
(Fabric, Forge, and NeoForge).

Before and after:

| Scope | Before | After |
| --- | --- | --- |
| Core config | 27 files, 24 top-level types, 1,557 LOC | 29 files, 26 types, 1,546 LOC |
| Client editor | 3 files, 3 types, 365 LOC | 4 files, 4 types, 476 LOC |
| Loader bridges | 3 files, 3 types, 88 LOC | 3 files, 3 types, 88 LOC |
| Total scope | 33 files, 30 types, 2,010 LOC | 36 files, 33 types, 2,110 LOC |

The seven published compatibility wrappers remain. No primitive-specific
validation or serialization remains in those wrappers.

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
typed handles, JSON5 comments and order, diagnostics, immutable snapshots, and
previous-valid-config retention are the guarantees that consumers rely on.
