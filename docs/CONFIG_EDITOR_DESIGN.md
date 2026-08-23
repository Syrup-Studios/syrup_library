# Syrup Library Config Presentation and Editor Design

Status: Implemented  
Audience: Syrup Library maintainers and mod developers  
Scope: Config schema presentation, in-game editing, and public API evolution

## 1. Summary

This document defines the next version of the Syrup Library config presentation and editor API.
It takes useful presentation ideas from MidnightLib, but keeps Syrup Library's current typed schema,
transactional edit session, JSON5 preservation, conflict detection, restart handling, and support for
multiple config files per mod.

The design adds these capabilities:

1. Localized config, section, value, group, information-row, and enum-option text.
2. Typed editor hints for sliders, colors, paths, and multiline strings.
3. Conditional visibility and conditional editing.
4. Non-persistent information rows.
5. A proper string-list editor.
6. Presentation-only groups or tabs.
7. Client, common, and server scope metadata plus explicit edit policies.
8. A client-only custom editor registry.
9. Localized and ordered enum choices.
10. Fluent builders for all new metadata.

All features are optional. A mod that uses only the existing API must continue to compile and must
keep the same config file and screen behavior.

## 2. Goals

- Give mod developers a typed way to describe how config values appear in the game.
- Keep storage rules independent from screen rules.
- Keep the current JSON5 format stable.
- Keep existing source and binary compatibility where Java permits it.
- Keep dedicated servers free of Minecraft client class-loading failures.
- Make simple configs remain simple.
- Let advanced mods extend the editor without writing files directly.
- Make all screen edits pass through `ConfigEditSession`.
- Preserve unknown JSON5 keys and comments during saves.
- Support all current Minecraft and loader targets.

## 3. Non-goals

- An annotation or reflection config system.
- Public static config fields.
- Remote server config editing.
- Client-to-server config synchronization.
- A replacement for JSON5.
- A general-purpose GUI framework.
- Automatic migration of config keys or paths.
- Absolute filesystem access from the first path-editor version.
- Changes to tests unless test work is requested separately.

## 4. Current system

Syrup Library currently has four useful layers:

```text
ConfigSpec and ConfigValue
        |
        v
ConfigLoader and DefaultJson5Writer
        |
        v
RegisteredConfig and immutable snapshots
        |
        v
ConfigEditSession and config screens
```

The current strengths must remain:

- Values are typed.
- Schema paths are explicit and stable.
- Specs are sealed after registration.
- Config state uses immutable snapshots.
- Restart-required values keep their startup value active.
- The screen edits drafts, not live values.
- Cancel discards the session.
- Save changes only edited paths.
- Unknown JSON5 keys remain in the file.
- Existing comments on edited values are preserved.
- Save refuses to replace a file that changed outside the game.
- One mod can own more than one config file.

The current limitation is that `ConfigEntryWidget` chooses widgets directly from the value class.
Presentation metadata is also limited to a literal description and a generated label.

## 5. Core design decisions

### 5.1 Storage and presentation are separate

Storage metadata controls:

- Key and path
- Java type
- Default value
- Validation
- Numeric bounds
- Restart requirement
- JSON5 serialization

Presentation metadata controls:

- Display name
- Screen description
- Editor style
- Group
- Visibility
- Enabled state
- Scope notice
- Read-only or hidden behavior
- Enum-option labels and order

Localized screen descriptions do not replace JSON5 comments. The existing literal `description`
argument remains the source for generated file comments.

### 5.2 The core API stays client-neutral

Core config packages must not expose these types:

- `Component`
- `Screen`
- `AbstractWidget`
- `Minecraft`
- Loader-specific client types

The core stores translation keys, editor IDs, conditions, and other plain Java metadata. The client
package turns that metadata into Minecraft components and widgets.

### 5.3 Existing methods stay valid

This remains valid:

```java
IntConfigValue volume = spec.intValue(
        "volume",
        80,
        0,
        100,
        "Master volume from 0 to 100."
);
```

Internally, the method can delegate to a new builder with default presentation metadata. The method
signature and behavior must not change.

### 5.4 New features use builders

Large overload sets do not scale. New presentation features use fluent builders:

```java
IntConfigValue volume = spec.intEntry("volume", 80)
        .range(0, 100)
        .description("Master volume from 0 to 100.")
        .editor(ConfigEditor.slider())
        .group(audioGroup)
        .build();
```

Builders validate their immediate input. `ConfigSpec.sealForRegistration()` validates relationships
between entries, such as conditions and group references.

## 6. Proposed data model

### 6.1 `ConfigPresentation`

Each schema node receives an immutable presentation object:

```java
public final class ConfigPresentation {
    Optional<String> labelTranslationKey();
    Optional<String> descriptionTranslationKey();
    ConfigEditorHint editor();
    Optional<String> groupId();
    ConfigCondition visibilityCondition();
    ConfigCondition enabledCondition();
    ConfigScope scope();
    ConfigEditPolicy editPolicy();
    EnumPresentation enumPresentation();
}
```

Defaults:

| Property | Default |
| --- | --- |
| Label key | No explicit key |
| Description key | No explicit key |
| Editor | `AUTO` |
| Group | Default group |
| Visibility | Always visible |
| Enabled state | Always enabled |
| Scope | `COMMON` |
| Edit policy | `EDITABLE` |
| Enum presentation | Declaration order and automatic labels |

`ConfigSchemaNode` owns this object because sections and values both need presentation metadata.

### 6.2 Ordered screen elements

Information rows must not become fake config values. Each section therefore has two views:

```text
Storage children
  Used by ConfigLoader and DefaultJson5Writer
  Contains sections and values only

Screen elements
  Used by the client editor
  Contains sections, values, and information rows
```

Proposed type:

```java
public sealed interface ConfigScreenElement
        permits ConfigSchemaNode, ConfigInfoRow {
}
```

Adding a section or value inserts the schema node into both ordered collections. Adding an
information row inserts it only into the screen collection.

### 6.3 Translation prefix

`ConfigSpec.Builder` gains an optional prefix:

```java
ConfigSpec spec = ConfigSpec.builder("client")
        .translationPrefix("example_mod.config.client")
        .build();
```

The prefix is plain client-neutral text. An absent prefix disables convention-based translation
lookup.

### 6.4 Presentation groups

Groups organize a screen but do not create JSON5 objects:

```java
ConfigGroup general = spec.group("general");
ConfigGroup advanced = spec.group("advanced");
```

A group has:

- Stable ID
- Declaration order
- Optional label translation key
- Optional description translation key
- Optional visibility condition

Config paths do not include group IDs.

## 7. Public API design

### 7.1 Config and section builders

Add entry methods to `ConfigContainer`:

```java
booleanEntry(String key, boolean defaultValue)
intEntry(String key, int defaultValue)
longEntry(String key, long defaultValue)
doubleEntry(String key, double defaultValue)
stringEntry(String key, String defaultValue)
stringListEntry(String key, List<String> defaultValue)
enumEntry(String key, Class<E> enumType, E defaultValue)
sectionEntry(String key)
info(String id)
```

Each builder supports the metadata that is valid for its type. Shared options include:

```java
.description(String text)
.restartRequirement(RestartRequirement requirement)
.labelTranslationKey(String key)
.descriptionTranslationKey(String key)
.group(ConfigGroup group)
.visibleWhen(ConfigCondition condition)
.enabledWhen(ConfigCondition condition)
.scope(ConfigScope scope)
.editPolicy(ConfigEditPolicy policy)
.editor(ConfigEditorHint hint)
.build()
```

Type-specific options include:

- Numeric range and slider step
- String validator and validation message
- List size and item validation
- Enum order, option labels, and enabled options

### 7.2 Complete example

```java
ConfigSpec spec = ConfigSpec.builder("client")
        .translationPrefix("example_mod.config.client")
        .header("Example Mod client configuration")
        .build();

ConfigGroup general = spec.group("general");
ConfigGroup advancedGroup = spec.group("advanced");

BooleanConfigValue advanced = spec.booleanEntry("advanced", false)
        .description("Shows advanced settings.")
        .group(general)
        .build();

IntConfigValue volume = spec.intEntry("volume", 80)
        .range(0, 100)
        .description("Master volume from 0 to 100.")
        .group(general)
        .editor(ConfigEditor.slider(1))
        .build();

StringConfigValue backend = spec.stringEntry("backend", "automatic")
        .description("The rendering backend.")
        .group(advancedGroup)
        .visibleWhen(ConfigCondition.isTrue(advanced))
        .build();

StringListConfigValue servers = spec.stringListEntry("servers", List.of())
        .description("Preferred server addresses.")
        .maximumSize(20)
        .group(advancedGroup)
        .build();

spec.info("advanced_warning")
        .style(ConfigInfoStyle.WARNING)
        .group(advancedGroup)
        .visibleWhen(ConfigCondition.isTrue(advanced))
        .build();
```

## 8. Localized names and descriptions

### 8.1 Key convention

Use these keys when a prefix exists:

```text
<prefix>.title
<prefix>.<path>.name
<prefix>.<path>.description
<prefix>.group.<group-id>.name
<prefix>.group.<group-id>.description
<prefix>.info.<info-id>.text
<prefix>.<enum-path>.option.<serialized-value>
```

Example language file:

```json
{
  "example_mod.config.client.title": "Client Settings",
  "example_mod.config.client.audio.name": "Audio",
  "example_mod.config.client.audio.description": "Change the audio settings.",
  "example_mod.config.client.audio.volume.name": "Master Volume",
  "example_mod.config.client.audio.volume.description": "Controls all mod sounds.",
  "example_mod.config.client.mode.option.fast": "Fast",
  "example_mod.config.client.mode.option.quality": "High Quality"
}
```

### 8.2 Lookup order

For names:

1. Explicit translation key, when it exists in the active language.
2. Convention-based translation key, when it exists.
3. Current readable-name generation from the stable key.

For descriptions:

1. Explicit translation key, when it exists.
2. Convention-based translation key, when it exists.
3. Existing literal description.

For enum choices:

1. Explicit option translation key.
2. Convention-based option key.
3. Readable enum constant name.

### 8.3 File behavior

The existing literal description remains in generated JSON5 comments:

```json5
/*
 * Master volume from 0 to 100.
 * Default: 80 | Range: 0 ~ 100
 */
volume: 80
```

Changing the game language must not modify the config file.

### 8.4 End result

Developers can localize the complete config screen without changing stable config paths. Mods that
do not add a prefix or language keys continue to show generated labels and literal descriptions.

## 9. Typed editor hints

### 9.1 Client-neutral hints

Proposed factories:

```java
ConfigEditor.auto()
ConfigEditor.slider()
ConfigEditor.slider(Number step)
ConfigEditor.color(boolean alpha)
ConfigEditor.multiline(int visibleLines)
ConfigEditor.path(ConfigPathMode mode, Set<String> extensions)
ConfigEditor.custom(String editorId)
```

Hints describe intent. They do not contain widgets.

### 9.2 Validation rules

- `SLIDER` accepts `int`, `long`, and `double` values only.
- Slider bounds come from the value's existing numeric bounds.
- `COLOR` accepts validated strings only.
- `MULTILINE` accepts strings only.
- `PATH` accepts strings only.
- `CUSTOM` requires a valid namespaced editor ID.
- Invalid combinations fail during spec registration.

### 9.3 End result

Developers can select a suitable editor without changing serialization or value types. `AUTO`
continues to produce the current boolean, enum, and text controls.

## 10. Client editor registry

### 10.1 Client API

Add these client-only types:

- `ConfigEditorRegistry`
- `ConfigEditorFactory`
- `ConfigEditorContext`
- `ConfigEditorHandle`

The context provides:

- Schema node
- Typed config value
- Edit session
- Current draft value
- Change callback
- Screen-navigation callback
- Available size
- Client text resolver

The handle provides:

- Child GUI elements
- Position and size updates
- Refresh from session
- Active-state updates
- Optional narration text
- Cleanup

### 10.2 Registration

```java
ConfigEditorRegistry.register(
        "example_mod:special",
        context -> new SpecialConfigEditor(context)
);
```

Rules:

- Register only during client setup.
- Reject duplicate IDs.
- Freeze the registry before the first config screen opens.
- Do not load registry classes on a dedicated server.
- Fall back to the automatic editor when an editor ID is unavailable.
- Log one useful warning for a missing editor.
- Require all edits to use `ConfigEditSession`.

### 10.3 `ConfigEntryWidget` refactor

`ConfigEntryWidget` becomes a row container. It remains responsible for:

- Label
- Description
- Validation message
- Restart notice
- Reset button
- Layout
- Conditional active state

The registry creates and manages the actual value control.

### 10.4 End result

Syrup Library owns stable row behavior. Mods can add specialized controls without copying save,
validation, reset, or conflict code.

## 11. Conditional entries

### 11.1 Condition API

```java
ConfigCondition.isTrue(booleanValue)
ConfigCondition.equals(value, expected)
ConfigCondition.oneOf(value, allowedValues)
ConfigCondition.modLoaded("mod_id")
ConfigCondition.allOf(conditions)
ConfigCondition.anyOf(conditions)
ConfigCondition.not(condition)
```

Conditions use typed `ConfigValue<T>` references. They do not use string paths.

### 11.2 Two behaviors

```java
.visibleWhen(condition)
.enabledWhen(condition)
```

- A false visibility condition removes the row from the current list.
- A false enabled condition keeps the row visible but disables its editor and reset button.

### 11.3 Draft evaluation

Conditions read values through `ConfigEditSession.value(...)`. A user can therefore enable an
advanced option and see dependent entries immediately, before Save.

### 11.4 State rules

- A hidden entry keeps any existing draft.
- Hiding an entry does not reset it.
- Reset All acts only on entries that the current screen can edit.
- Focus clears if its row becomes hidden.
- The selected group remains selected when possible.
- The screen rebuilds rows only when a condition result changes.

### 11.5 Validation at registration

Reject:

- References to values from another spec
- Self-dependencies
- Cyclic condition graphs
- Expected values with the wrong type
- Invalid mod IDs

### 11.6 End result

Config screens can show simple modes first and reveal advanced controls when needed. All dependent
UI uses unsaved draft values, while the file remains unchanged until Save.

## 12. Information rows

### 12.1 Supported styles

- `TEXT`
- `HELP`
- `WARNING`
- `ERROR`
- `LINK`
- `SPACER`

An information row can have:

- Stable ID
- Literal fallback text
- Translation key
- Group
- Visibility condition
- Link target when its style is `LINK`

### 12.2 Link safety

A link row uses Minecraft's normal confirmation screen. It must not open an external link without
user confirmation.

### 12.3 Persistence rules

Information rows:

- Do not become `ConfigValue` objects.
- Do not enter runtime snapshots.
- Do not enter `ConfigEditSession`.
- Do not appear in JSON5.
- Do not affect dirty state.

### 12.4 End result

Developers can add help, warnings, spacing, and documentation links at the correct place in a config
screen without creating fake stored fields.

## 13. Proper string-list editor

### 13.1 Screen behavior

The main config row shows a summary and an Edit button. The dedicated list screen supports:

- Add item
- Edit item
- Remove item
- Move item up
- Move item down
- Reset to default
- Done
- Cancel

### 13.2 Draft behavior

- Open with an independent copy of the session value.
- Do not modify the session for each local screen action.
- Apply one immutable list to the session on Done.
- Discard local edits on Cancel.
- Preserve order, empty strings, commas, whitespace, and newlines inside items.

### 13.3 Optional constraints

```java
.minimumSize(1)
.maximumSize(20)
.itemValidator(value -> ...)
.itemValidationMessage("Invalid list item")
```

Existing lists have no size limit and accept all strings.

### 13.4 End result

Players edit one list item at a time. They no longer need to understand raw JSON5 array syntax. The
stored value remains the same JSON5 string array.

## 14. Specialized built-in editors

### 14.1 Sliders

Support `int`, `long`, and `double` values.

Requirements:

- Use existing inclusive bounds.
- Support an optional step.
- Show the exact current value.
- Support keyboard and narration behavior.
- Avoid lossy long conversion.
- Use stable rounding for doubles.

End result: bounded values can use a slider while keeping the same numeric storage and validation.

### 14.2 Colors

Use an in-game color editor. Do not use Swing or AWT.

Requirements:

- Hex text input
- RGB controls
- Optional alpha
- Preview rectangle
- Validation before Done
- Cancel without a draft change

End result: players can select a color and still store the validated string format chosen by the mod.

### 14.3 Multiline strings

Use a dedicated text screen.

Requirements:

- Multiple lines
- Optional character limit
- Existing string validator
- Done and Cancel
- Exact newline preservation

End result: long text is easier to edit without changing its JSON5 string representation.

### 14.4 Files and directories

Use an in-game path browser.

Initial requirements:

- Root inside the loader config directory.
- Store relative paths by default.
- Block path traversal.
- Support file, directory, or either mode.
- Support extension filters.
- Permit direct text entry.
- Do not require desktop APIs.

End result: a mod can request a safe local path without platform-specific desktop dialogs. Absolute
paths remain outside the first version.

## 15. Presentation groups

Groups are optional. They become visible only when a section has more than one visible group.

Requirements:

- Keep declaration order.
- Keep an ungrouped default group.
- Do not change config paths.
- Do not create JSON5 sections.
- Remember selection while the edit session remains open.
- Hide an empty group when conditions remove all its rows.
- Select the first available group if the selected group becomes empty.

Use a small Syrup-owned group bar. Do not depend on Minecraft's changing tab implementation for the
public behavior.

### End result

Large config sections can use clear pages such as General, Audio, and Advanced. Small configs keep
the current single-list layout.

## 16. Scope and edit policy

### 16.1 Scope

```java
public enum ConfigScope {
    CLIENT,
    COMMON,
    SERVER
}
```

Scope describes intent. It does not provide networking.

### 16.2 Edit policy

```java
public enum ConfigEditPolicy {
    EDITABLE,
    READ_ONLY,
    HIDDEN
}
```

Scope and edit policy are separate concepts.

### 16.3 Screen behavior

- `CLIENT` and `COMMON` values can be edited locally by default.
- `SERVER` values are read-only by default in a local client screen.
- A hidden value remains stored and available at runtime.
- A read-only row shows the configured value and a reason.
- Reset and Reset All skip values that the screen cannot edit.
- The public `ConfigEditSession.resetAll()` method keeps its current behavior.
- Add a new scoped reset method for screen use.

### 16.4 End result

Players can see which settings are local or controlled elsewhere. The API does not imply that a
client can change a remote server file.

## 17. Enum presentation

Add these builder options:

```java
.optionOrder(List<E> values)
.optionLabel(E value, String translationKey)
.enabledOptions(Predicate<E> predicate)
```

Rules:

- Serialization stays as the current lowercase enum constant name.
- Loading accepts all declared enum constants.
- Display order can differ from declaration order.
- Disabled choices are not offered for a new selection.
- A currently stored disabled choice remains visible.
- The library does not replace a disabled stored choice automatically.

### End result

Enum controls use player-facing localized text and useful ordering without changing existing files.

## 18. Registration validation

Extend `ConfigSpec.sealForRegistration()` to validate the complete schema.

Validation includes:

- Unique value, section, information-row, and group IDs in their correct namespace
- Valid translation keys
- Valid namespaced custom editor IDs
- Editor and value-type compatibility
- Slider step and range correctness
- List size limits
- Enum order without duplicates
- Enum choices from the correct enum type
- Group references within the same section or spec
- Conditions that reference values from the same spec
- Condition graph cycle detection
- Scope and edit-policy combinations

Messages must include the schema path or metadata ID.

## 19. Screen behavior and layout

### 19.1 Root screen

- Use the localized config title when present.
- Continue to show the edited filename.
- Show group controls only when needed.
- Show Save, Reset All, Cancel or Back, and Reload as today.

### 19.2 Rows

Each value row can show:

- Localized label
- Description tooltip
- Editor
- Reset button
- Validation error
- Restart notice
- Scope or read-only notice

Message priority:

1. Validation error
2. Read-only reason
3. Restart requirement
4. Normal description

### 19.3 Reset behavior

- Per-value Reset sets the schema default as an explicit draft.
- Screen Reset All affects visible and editable stored values in the current config.
- Information rows are ignored.
- Hidden and read-only entries are ignored by the screen action.

### 19.4 Save behavior

All editors use the existing save path:

```text
Widget
  -> ConfigEditSession candidate validation
  -> draft map
  -> RegisteredConfig.save
  -> JSON5 conflict check
  -> selective value replacement
  -> reload and immutable snapshot publication
```

No editor can bypass this path.

## 20. Compatibility guarantees

### 20.1 Source compatibility

- Keep all existing public declaration methods.
- Keep current method parameter order and meaning.
- Keep current public value accessors.
- Add builders and metadata as new API.

### 20.2 Binary compatibility

- Do not remove or change existing public methods or constructors.
- Add fields privately.
- Add public methods without changing existing descriptors.
- Review sealed-class changes carefully because permitted subclasses affect extension options.

### 20.3 File compatibility

- Do not change config IDs.
- Do not change schema paths.
- Do not change enum serialization.
- Do not write translation keys to JSON5.
- Do not write groups or information rows to JSON5.
- Keep unknown keys and comments.
- Keep conflict detection.

### 20.4 Visual compatibility

When a spec uses no new metadata:

- Labels use the current readable-name fallback.
- Literal descriptions remain tooltips.
- Existing widget selection remains the default.
- Sections remain navigation rows.
- No group bar appears.
- Save and Cancel behavior remains unchanged.

### 20.5 Dedicated-server compatibility

- Presentation model classes remain plain Java.
- Client widget and translation code stays in the client package.
- Loader registration must not reference client classes on a dedicated server.
- Custom editor registration is client-only.

## 21. Implementation phases and end results

### Phase 1: Metadata and builder foundation

Work:

- Add `ConfigPresentation`.
- Add ordered screen elements.
- Add translation prefix.
- Add entry builders.
- Make old declaration methods delegate internally.
- Add registration validation framework.

End result:

- The schema can hold all planned presentation data.
- Existing screens still behave as before.
- Existing JSON5 output remains unchanged.

### Phase 2: Localization

Work:

- Add the client text resolver.
- Apply it to titles, sections, values, descriptions, and enums.
- Add missing-translation fallbacks.

End result:

- A mod can localize names and descriptions.
- A mod with no translations sees no behavior change.

### Phase 3: Editor registry

Work:

- Add the client editor registry and context.
- Refactor value-row widget creation.
- Register current controls as built-in editors.
- Add custom editor fallback.

End result:

- Widget selection is extensible.
- Current automatic controls remain available.

### Phase 4: List editor

Work:

- Add the dedicated string-list screen.
- Add list constraints.
- Connect Done and Cancel to one session draft.

End result:

- Players can add, edit, remove, and reorder list items without raw JSON5.

### Phase 5: Specialized editors

Work:

- Add sliders.
- Add the color editor.
- Add multiline text.
- Add the safe path browser.

End result:

- Common specialized values have suitable built-in controls.
- Storage formats stay unchanged.

### Phase 6: Conditions

Work:

- Add typed condition objects.
- Validate dependencies and cycles.
- Evaluate conditions from draft values.
- Refresh only affected screen rows.

End result:

- Entries can appear or unlock immediately based on unsaved values.

### Phase 7: Information rows and groups

Work:

- Add information-row types.
- Add link confirmation.
- Add presentation groups and the group bar.

End result:

- Config screens can contain structured help and optional tabs without changing JSON5.

### Phase 8: Scope, policy, and enum presentation

Work:

- Add scope and edit policy.
- Add screen-aware Reset All.
- Add enum labels, order, and enabled choices.

End result:

- Screens explain edit ownership.
- Enums have complete presentation control.

### Phase 9: Documentation and release preparation

Work:

- Update the README.
- Add old and new API examples.
- Document translation keys.
- Document client-only custom registration.
- Document scope limits.
- Add the compatibility statement to the changelog.

End result:

- Existing mod developers have a clear migration path.
- New mod developers can use advanced features without reading implementation code.

## 22. Final product results

### 22.1 Result for mod developers

Mod developers can:

- Keep the old API without changes.
- Opt into fluent entry builders.
- Localize all visible config text.
- Choose built-in editor styles.
- Create conditional advanced settings.
- Add help and warning content.
- Organize large configs into groups.
- Mark settings as client, common, or server scope.
- Register a custom client editor when built-ins are insufficient.

### 22.2 Result for players

Players get:

- Text in their selected language.
- Better controls for numbers, colors, paths, text, and lists.
- Clear descriptions and validation errors.
- Fewer irrelevant advanced options.
- Read-only notices for settings they cannot safely change.
- The same safe Save, Cancel, Reset, conflict, and restart behavior.

### 22.3 Result for maintainers

Maintainers get:

- One client-neutral presentation model.
- One editor registry instead of a growing `instanceof` widget constructor.
- Storage code that does not know about GUI behavior.
- Registration-time errors for invalid schemas.
- A staged implementation that can ship in small releases.

### 22.4 Result for config files

Config files keep:

- The same `.json5` location.
- The same paths and value types.
- The same enum strings.
- The same literal schema comments.
- Unknown keys.
- Existing comments on changed values.
- External-change protection.

Groups, translation keys, conditions, information rows, and editor hints never appear in JSON5.

## 23. Verification plan

Do not add or change test files as part of this design work.

Run the existing build for all targets:

```shell
./gradlew \
  :1.20.1-fabric:build \
  :1.20.1-forge:build \
  :1.21.1-fabric:build \
  :1.21.1-neoforge:build \
  :1.21.11-fabric:build \
  :1.21.11-neoforge:build \
  :26.2-fabric:build \
  :26.2-neoforge:build
```

Manual verification must cover:

- A mod that uses only the old API
- A mod that uses all new metadata
- Present and missing translations
- Language changes while a screen is open
- Nested sections
- Multiple config files for one owner
- Save, Cancel, Reset, Reset All, and Reload
- External file changes during editing
- Restart-required values
- Hidden and read-only entries
- Conditions based on draft values
- Lists with commas, whitespace, and empty strings
- A missing custom editor
- Fabric Mod Menu integration
- NeoForge config-screen integration
- Dedicated-server startup
- JSON5 comments and unknown keys after Save

## 24. Release conditions

The feature set is ready for a stable release when:

- All eight target builds succeed.
- Old API example code compiles without changes.
- An old config file loads and saves without path or value migration.
- A spec with no presentation metadata keeps the current screen behavior.
- Dedicated servers do not load client editor classes.
- Every built-in editor uses `ConfigEditSession`.
- Conditions use draft values and reject cycles.
- Information rows and groups never enter JSON5.
- Missing translations and custom editors have safe fallbacks.
- The README documents both old and new APIs.

## 25. Implemented release boundary

The implementation includes all phases in this document:

1. Presentation metadata and fluent builders.
2. Localized names and descriptions.
3. The built-in and custom editor registry.
4. String-list, slider, color, multiline, and safe path editors.
5. Conditional visibility and enabled state.
6. Information rows and presentation groups.
7. Scope and edit policies.
8. Advanced enum presentation.

The features remain separate in the API. A future release can still document them in smaller groups,
but they share the same compatibility and storage guarantees.
