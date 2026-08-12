# 0.2.0

- Added an in-game config editor that opens from the Fabric Mod Menu page and the NeoForge mod options page.
- Added `SyrupConfigManager.register(String ownerModId, ConfigSpec)`, `registeredConfigs(String)`, and `registeredOwnerIds()` so config screens can be attached to the owning mod.
- Added an immutable public schema tree through `ConfigSpec.schema()` and `ConfigSchemaNode`.
- Added value validation to the edit session.
- Added a transaction-based edit session (`ConfigEditSession`) that only writes to disk on save. Dirty state is value-based: changing a value and changing it back does not leave the session dirty.
- Added safe atomic JSON5 updates that preserve unknown keys and associated comments, compare the file again immediately before replacement, and update only draft paths.
- Added serialized-value validation so a malformed edit cannot replace a file.
- Added exact string-list editing through JSON5 array text.
- Nested sections navigate with Back while sharing one edit session; the root section uses Cancel.
- NeoForge config screens are attached automatically during client setup; consumers need no loader-specific registration code.
- Fabric Mod Menu remains optional and is never packaged.
- Existing public API (`register(spec)`, all `ConfigContainer` builder methods, `ConfigValue#get()`, JSON5 file format) is unchanged.
