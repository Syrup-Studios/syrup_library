---
title: Config architecture
description: Public config API, state rules, and compatibility facts.
---

# Config architecture

## Public API

| Area | Main types | Purpose |
| --- | --- | --- |
| Declarations | `ConfigSpec`, `ConfigSection`, `ConfigValue<T>`, `ConfigType<T>` | Define ordered settings, types, defaults, constraints, and metadata. |
| Registration | `SyrupConfigManager`, `RegisteredConfig` | Register a config, choose its path, and load or reload it. |
| Reads | `ConfigSnapshot`, `RestartRequirement` | Read consistent values and inspect restart-required settings. |
| Results | `ConfigLoadResult`, `ConfigUpdateResult`, `ConfigSaveResult`, `ConfigIssue` | Report load, update, and save outcomes. |
| Client editing | `SyrupConfigScreen`, `ConfigEditorRegistry` | Edit registered values with validation and save support. |

`ConfigState`, `ConfigLoader`, and `DefaultJson5Writer` are implementation details, not public API types.

## State and file rules

`ConfigValue<T>.get()` returns its declared default before registration. After registration, it reads the state of its owning `RegisteredConfig`. A `ConfigSnapshot` is immutable and reads one published state.

A successful reload publishes all values together. Missing values use defaults and are added to the file. A present invalid value rejects the candidate; the library does not partially apply it. On initial load, defaults remain active. On a later reload, the last valid state remains active. A file parse failure also keeps the last valid state.

Restart-required values keep their startup value active while exposing the latest valid configured value. This behavior applies to reloads and programmatic updates.

## Compatibility

The typed wrapper API was removed in version 0.3.0. Consumers that use that API must stay on 0.2.0 or migrate to `ConfigValue<T>` and rebuild. The repository target version is 0.5.1.

## Known parser limit

The bundled JSON5 parser has an exponent lexer issue for some manually written scientific notation. Syrup's generated numeric output uses plain decimal values. Manually written scientific notation can still be affected.

## Custom types and editors

A custom `ConfigType<T>` defines JSON5 decoding, encoding, and normalization. Its normalizer must return an independent value for mutable types. The library uses it for defaults, decoded values, updates, and snapshots.

`ConfigEditorRegistry.register(type, adapter)` adds a client editor for a custom type. A type with choices gets a choice editor. A type without an editor uses a read-only control; values still use normal validation and save behavior.
