# Trigger Volume API Research

Analyzed JARs:
- `Server-0.5.5.jar` — used in project build (`compileOnly("com.hypixel.hytale:Server:0.5.5")`)
- `Server-2026.03.26-89796e57b.jar` — pre-release; does **not** contain `TriggerVolumesPlugin`

**Pin the project to 0.5.5** until a later release version re-adds the TriggerVolumes module.

---

## 1. Registration Entry Point

```java
TriggerVolumesPlugin.get()
    .registerEffectType(String typeId, Class<T> clazz, BuilderCodec<T> codec)
    .registerConditionType(String typeId, Class<T> clazz, BuilderCodec<T> codec)
    .getManagerResourceType()  // ResourceType<EntityStore, TriggerVolumeManager>
```

- Must be called after `TriggerVolumesPlugin` has started (i.e. from your plugin's `start()` method).
- `TriggerVolumesPlugin` is a built-in plugin and is started before third-party plugins.

---

## 2. Base Classes

### `TriggerEffect` — `com.hypixel.hytale.builtin.triggervolumes.effect`

```java
public abstract class TriggerEffect {
    public abstract void execute(TriggerContext ctx);
    public void onEntityExit(UUID entityId);          // optional cleanup
    public TriggerEventType getEventType();
    public void setEventType(TriggerEventType);
    public float getInterval();
    public float getDelay();
    public static final CodecMapCodec<TriggerEffect> CODEC;      // type dispatch
    public static final BuilderCodec<TriggerEffect> BASE_CODEC;  // base fields
}
```

Extend this and implement `execute(TriggerContext)` to create a new action.
Declare `public static final BuilderCodec<MyAction> CODEC` in your subclass.

### `TriggerCondition` — same package

```java
public abstract class TriggerCondition {
    public abstract boolean test(TriggerContext ctx);
    public void applyOnAccept(TriggerContext ctx);    // optional side-effect on pass
    public void onEntityExit(UUID entityId);
    public static final CodecMapCodec<TriggerCondition> CODEC;
    public static final BuilderCodec<TriggerCondition> BASE_CODEC;
}
```

---

## 3. TriggerContext

Available inside `execute()` and `test()`:

| Method | Return type | Notes |
|---|---|---|
| `getEntityRef()` | `Ref<EntityStore>` | The triggering entity (no `getId()` — use PlayerRef) |
| `getStore()` | `Store<EntityStore>` | The ECS world store |
| `getEventType()` | `TriggerEventType` | Which event fired |
| `getVolume()` | `VolumeEntry` | The volume that fired |
| `getSpatialVolumes()` | `List<VolumeEntry>` | Other overlapping volumes |
| `getTagKey()` / `getTagValue()` | `String` | For TAG_ADDED/TAG_REMOVED events |
| `getBlockPosition()` | `Vector3d` | For BLOCK_PLACED/BLOCK_BROKEN events |
| `getBlockId()` | `String` | For BLOCK_PLACED/BLOCK_BROKEN events |

**Getting the entity UUID:** `Ref<EntityStore>` has no `getId()`. Get UUID via `PlayerRef`:
```java
PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
UUID uuid = (pr != null) ? pr.getUuid() : null;  // null if entity is not a player
```

---

## 4. TriggerEventType

```java
public enum TriggerEventType {
    ENTER, EXIT, TICK, TAG_ADDED, TAG_REMOVED, BLOCK_PLACED, BLOCK_BROKEN
}
```

---

## 5. BuilderCodec Pattern

```java
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;  // also: FloatCodec, BooleanCodec, LongCodec

public static final BuilderCodec<MyAction> CODEC = BuilderCodec.builder(
        MyAction.class, MyAction::new, TriggerEffect.BASE_CODEC)   // 3rd arg inherits base fields
    .append(
        new KeyedCodec<>("fieldName", new StringCodec()),           // required field
        MyAction::setFieldName,                                     // setter: BiConsumer<T, FieldType>
        MyAction::getFieldName                                      // getter: Function<T, FieldType>
    ).add()                                                         // .add() is the terminal method
    .append(
        new KeyedCodec<>("optField", new StringCodec(), false),     // optional field (false = not required)
        MyAction::setOptField,
        MyAction::getOptField
    ).add()
    .build();                                                       // produces the BuilderCodec<T>
```

- `KeyedCodec(String key, Codec<T>)` — required by default
- `KeyedCodec(String key, Codec<T>, boolean required)` — pass `false` for optional
- `BuilderField$FieldBuilder` terminal method: **`.add()`** (returns the parent builder)
- `BuilderCodec$BuilderBase` terminal method: **`.build()`**

---

## 6. TriggerVolumeManager

Retrieved from context: `TriggerVolumeApiAdapter.getManagerForStore(ctx.getStore())`

Key methods:
- `getVolume(String id)` → `VolumeEntry` (nullable)
- `getVolumes()` → `Collection<VolumeEntry>`
- `hasVolume(String id)` → `boolean`
- `setTag(String volumeId, String key, String value, Ref<EntityStore>, UUID)` → `boolean`

---

## 7. VolumeEntry

```java
VolumeEntry.getEffects()           → List<TriggerEffect>
VolumeEntry.getConditions()        → List<TriggerCondition>
VolumeEntry.getRejectionEffects()  → List<TriggerEffect>
VolumeEntry.isEnabled()            → boolean
VolumeEntry.getRawTags()           → Map<String, String>
```

---

## 8. PlayerRef

```java
// com.hypixel.hytale.server.core.universe.PlayerRef
PlayerRef.getComponentType()       // static — ComponentType for ECS lookup
pr.getUuid()                       // UUID
pr.getUsername()                   // String
pr.hasPermission(String)           // boolean
pr.getPacketHandler()              // PacketHandler — for sending packets
```

---

## 9. Command System

```java
// Root / group commands
class MyCommand extends AbstractCommandCollection {
    MyCommand() { super("name", "description"); addSubCommand(new SubCmd()); }
}

// Leaf commands
class SubCmd extends AbstractAsyncCommand {
    private final RequiredArg<PlayerRef> playerArg;

    SubCmd() {
        super("sub", "description");
        this.playerArg = new RequiredArg<>(this, "player", "The player", ArgTypes.PLAYER_REF);
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        PlayerRef p = ctx.get(playerArg);
        // ...
        return CompletableFuture.completedFuture(null);
    }
}
```

Available `ArgTypes`: `PLAYER_REF`, `STRING`, `GREEDY_STRING`, `INTEGER`, `FLOAT`, `BOOLEAN`, `UUID`, `PLAYER_UUID`, and many asset types.

Sending a plain-text message:
```java
FormattedMessage fm = new FormattedMessage();
fm.rawText = "Hello!";
ctx.sendMessage(new Message(fm));
```

---

## 10. Event Registration

```java
// Subscribe to player disconnect (cleanup state)
getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
    UUID uuid = event.getPlayerRef().getUuid();
    // ...
});
```

Package: `com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent`

---

## 11. Native Effects (DO NOT duplicate)

`SendMessage`, `Teleport`, `GiveItem`, `PlaySound`, `ShowEventTitle`, `SetGameMode`, `DamageEntity`,
`SetVelocity`, `PlaceBlock`, `SetWeather`, `PlayVfx`, `PastePrefab`, `ControlDoors`, `EnableVolume`,
`DisableVolume`, `DeleteVolume`, `ModifyTags`, `ReplaceBlockType`, `RunRootInteraction`,
`EntityEffect`, `SetMusic`, `TriggerNpcMarkers`, `VolumeState`

---

## 12. Native Conditions (DO NOT duplicate)

`Permission`, `Cooldown`, `RandomChance`, `Tag`, `Item`, `BlockType`, `GameMode`, `PlayerCount`

---

## 13. Implemented Follow-Ups

| # | Item | Where |
|---|---|---|
| 1 | `RemoveItemAction` removes from full inventory, hotbar, or active in-hand slot using native inventory containers | `RemoveItemAction.execute()` |
| 2 | `SendTitleAction` sends title/subtitle and action-bar style notification packets | `SendTitleAction.execute()` |
| 3 | `RunCommandAction` and `SendTitleAction` resolve `{player}`, `{uuid}`, and `{variable:key}` placeholders | `StringTemplate.resolve()` |
| 4 | `TriggerNamedVolumeAction` uses the HyExtras dispatcher so target conditions, rejection effects, effects, enabled state, and cooldown are honored | `ExtraTriggerDispatcher.dispatch()` |
| 5 | HyExtras editor translations are merged into `I18nModule` from `Server.Languages.en-US/server.lang` at startup | `I18nDefaultsLoader.load()` |
| 6 | Editor dropdowns use `EnumCodec.documentKey(...)` for lowercase JSON values and readable `.option.*` language labels | `CodecHelper.enumField()` / `optEnum()` |

## 14. Remaining Watch Items

| # | Item | Where |
|---|---|---|
| 1 | If a future Hytale API adds `TriggerVolumeManager.fireVolume()`, compare it against `ExtraTriggerDispatcher` and switch only if it preserves rejection/cooldown behavior | `TriggerNamedVolumeAction.execute()` |
| 2 | Re-check TriggerVolumes availability before moving off `Server-0.5.5` | `build.gradle` |
