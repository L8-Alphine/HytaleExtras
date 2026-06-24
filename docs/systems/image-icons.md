# Image Icons

Image Icons is a **developer-facing** system: it lets other mods own local or remote **PNG/GIF**
icons and attach them to player or entity UUIDs, without HyExtras owning the assets or risking global
ID collisions. Icons are **provider-scoped**, so `mysticnametags:vip.sparkle` never collides with
another mod's `vip.sparkle`.

**Provided by:** the `image_icons` [module](modules.md). Driven entirely through the
[Developer API](../DEVELOPER_API.md#imageicons) — there are no trigger effects for it.

---

## Providers

A provider is a registered, readable asset folder (commonly a mod-owned path like
`mods/MysticNameTags/data/imageicons`). Provider and icon IDs are normalized to lowercase
`[a-z0-9_.-]`; local icon IDs are derived from each file's path relative to the provider folder (with
`/` becoming `.`). Only `.png` and `.gif` are supported; GIFs load all frames.

```java
HyExtrasApi api = HyExtrasApi.get();
api.registerImageIconProvider("mysticnametags", Path.of("mods/MysticNameTags/data/imageicons"));
```

### Local hot reload
When [`imageIcons.hotReload=true`](../configuration.md#imageicons), HyExtras watches each provider folder
(recursively) on a daemon thread and reloads changed PNG/GIF assets automatically.

### Remote icons
`registerRemoteImageIcon(providerId, iconId, uri)` downloads an `http`/`https` PNG/GIF into the
HyExtras cache (`<dataDir>/image-icons-cache/<provider>/`), validated by content-type/extension and
size-limited by [`imageIcons.remoteCache.maxBytes`](../configuration.md#imageicons) (default 5 MiB), then
loads it like a local asset. Requires `imageIcons.remoteCache.enabled=true`.

```java
api.registerRemoteImageIcon("mysticnametags", "vip.sparkle",
        URI.create("https://example.com/assets/vip.gif"));
```

---

## Attachments

Attach a provider-scoped icon to a player or entity UUID. Attachments are **runtime-only**, keyed by a
returned attachment UUID, and require the PacketAPI backend to be available.

```java
ImageIconTuning tuning = ImageIconTuning.defaults(null);
ImageIconResult attached = api.attachImageIconToPlayer(playerUuid, "mysticnametags", "vip.sparkle", tuning);
if (attached.success()) {
    UUID attachmentId = attached.attachmentId();
    // later…
    api.clearImageIcon(attachmentId);     // one attachment
    api.clearImageIcons(playerUuid);      // all attachments on a target
}
```

Attachments are cleared automatically on player **disconnect**, provider **unregister**, module
**disable**, and **shutdown**. Per viewer, attachments are sorted by priority (then creation time) and
capped by [`imageIcons.maxIconsPerViewer`](../configuration.md#imageicons) (default 64).

### Tuning (`ImageIconTuning`)
A record of presentation/placement fields (runtime-only). `defaults(config)` provides sensible values;
`withFallbacks` fills zeros from defaults and the icon definition's pixel size:

| Field | Default | Meaning |
|---|---|---|
| `offsetX/Y/Z` | `0.0` | World-space offset. |
| `heightOffset` | `0.65` | Vertical offset above the target. |
| `scale` | `1.0` | Size multiplier. |
| `maxDistance` | `imageIcons.defaultVisibilityRadius` (`48.0`) | Viewer render distance. |
| `billboardMode` | `ALWAYS` | `ALWAYS`, `VERTICAL`, or `NONE`. |
| `pixelOffsetX/Y` | `0` | Screen-space pixel offset. |
| `width` / `height` | from image | Override render size in pixels. |
| `zOrder` | `0` | Draw order. |

---

## Inspection

`snapshotImageIconProviders()`, `snapshotImageIcons(providerId)`, `snapshotImageIcons()`,
`snapshotImageIconAttachments()`, and `snapshotImageIconLoadErrors()` expose the current state and any
asset load errors for debugging.

---

## See also

- [Developer API → ImageIcons](../DEVELOPER_API.md#imageicons) — full method list.
- [Configuration](../configuration.md#imageicons) — hot reload, remote cache, limits.
- [Modules](modules.md) — enabling/disabling `image_icons`.
