package org.hyzionstudios.hyextras.imageicons;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public final class ImageIconService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".gif");

    private final HyExtrasPlugin plugin;
    private final ImageIconRenderer renderer;
    private final ConcurrentHashMap<String, ImageIconProviderRegistration> providers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ImageIconDefinition>> icons =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ImageIconAttachment> attachments = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> loadErrors = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile boolean running;
    private volatile WatchService watchService;
    private volatile Thread watchThread;

    public ImageIconService(HyExtrasPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new ImageIconRenderer(plugin);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        reloadAllProviders();
        restartWatcher();
    }

    public synchronized void stop() {
        running = false;
        closeWatcher();
        attachments.values().forEach(renderer::clear);
        attachments.clear();
        icons.clear();
        loadErrors.clear();
    }

    public ImageIconResult registerProvider(String providerId, Path assetsPath) {
        if (!moduleEnabled()) {
            return ImageIconResult.failure("ImageIcons module is disabled.");
        }
        String normalizedProvider = normalizeProviderId(providerId);
        if (normalizedProvider == null) {
            return ImageIconResult.failure("Provider id must use letters, numbers, '_', '-', or '.'.");
        }
        if (assetsPath == null) {
            return ImageIconResult.failure("Provider assets path is required.");
        }
        Path normalizedPath = assetsPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedPath) || !Files.isReadable(normalizedPath)) {
            return ImageIconResult.failure("Provider assets path must be a readable directory: " + normalizedPath);
        }

        boolean hotReload = plugin.getExtrasConfig() == null || plugin.getExtrasConfig().imageIconsHotReload;
        ImageIconProviderRegistration registration =
                new ImageIconProviderRegistration(normalizedProvider, normalizedPath, hotReload);
        providers.put(normalizedProvider, registration);
        ImageIconResult result = reloadProvider(normalizedProvider);
        restartWatcher();
        return result.success() ? ImageIconResult.success("Image icon provider registered: " + normalizedProvider) : result;
    }

    public ImageIconResult registerRemoteIcon(String providerId, String iconId, URI remoteUri) {
        if (!moduleEnabled()) {
            return ImageIconResult.failure("ImageIcons module is disabled.");
        }
        String normalizedProvider = normalizeProviderId(providerId);
        String normalizedIcon = normalizeIconId(iconId);
        if (normalizedProvider == null || normalizedIcon == null) {
            return ImageIconResult.failure("Provider id or icon id is invalid.");
        }
        ImageIconProviderRegistration registration = providers.get(normalizedProvider);
        if (registration == null) {
            return ImageIconResult.failure("Image icon provider is not registered: " + normalizedProvider);
        }
        if (!remoteCacheEnabled()) {
            return ImageIconResult.failure("ImageIcons remote cache is disabled.");
        }
        if (remoteUri == null || remoteUri.getScheme() == null
                || (!remoteUri.getScheme().equalsIgnoreCase("http")
                && !remoteUri.getScheme().equalsIgnoreCase("https"))) {
            return ImageIconResult.failure("Remote icon URI must be http or https.");
        }

        try {
            Path cached = downloadRemoteIcon(normalizedProvider, normalizedIcon, remoteUri);
            ImageIconDefinition definition = loadDefinition(
                    normalizedProvider,
                    normalizedIcon,
                    cached,
                    true,
                    remoteUri);
            registration.putRemoteSource(normalizedIcon, remoteUri);
            icons.computeIfAbsent(normalizedProvider, ignored -> new ConcurrentHashMap<>())
                    .put(normalizedIcon, definition);
            clearLoadError(normalizedProvider, normalizedIcon);
            refreshAttachments(normalizedProvider, normalizedIcon);
            return ImageIconResult.success("Remote image icon registered: "
                    + normalizedProvider + ":" + normalizedIcon);
        } catch (Exception e) {
            recordLoadError(normalizedProvider, normalizedIcon, e.getMessage());
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras image-icons] Failed to register remote icon "
                            + normalizedProvider + ":" + normalizedIcon);
            return ImageIconResult.failure("Failed to register remote icon: " + e.getMessage());
        }
    }

    public ImageIconResult reloadProvider(String providerId) {
        String normalizedProvider = normalizeProviderId(providerId);
        ImageIconProviderRegistration registration = normalizedProvider != null
                ? providers.get(normalizedProvider)
                : null;
        if (registration == null) {
            return ImageIconResult.failure("Image icon provider is not registered: " + providerId);
        }

        ConcurrentHashMap<String, ImageIconDefinition> loaded = new ConcurrentHashMap<>();
        List<String> errors = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(registration.assetsPath())) {
            stream.filter(Files::isRegularFile)
                    .filter(ImageIconService::isSupportedImagePath)
                    .forEach(path -> {
                        String iconId = iconIdForPath(registration.assetsPath(), path);
                        try {
                            loaded.put(iconId, loadDefinition(normalizedProvider, iconId, path, false, null));
                        } catch (Exception e) {
                            errors.add(iconId + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            errors.add("scan: " + e.getMessage());
        }

        for (Map.Entry<String, URI> entry : registration.remoteSources().entrySet()) {
            try {
                Path cached = downloadRemoteIcon(normalizedProvider, entry.getKey(), entry.getValue());
                loaded.put(entry.getKey(), loadDefinition(normalizedProvider, entry.getKey(), cached, true, entry.getValue()));
            } catch (Exception e) {
                errors.add(entry.getKey() + ": " + e.getMessage());
            }
        }

        icons.put(normalizedProvider, loaded);
        if (errors.isEmpty()) {
            loadErrors.remove(normalizedProvider);
        } else {
            loadErrors.put(normalizedProvider, List.copyOf(errors));
        }
        dropMissingAttachments(normalizedProvider, loaded.keySet());
        refreshProviderAttachments(normalizedProvider);
        return errors.isEmpty()
                ? ImageIconResult.success("Image icon provider reloaded: " + normalizedProvider)
                : ImageIconResult.failure("Image icon provider reloaded with " + errors.size() + " error(s).");
    }

    public ImageIconResult reloadAllProviders() {
        int failures = 0;
        for (String providerId : providers.keySet()) {
            if (!reloadProvider(providerId).success()) {
                failures++;
            }
        }
        restartWatcher();
        return failures == 0
                ? ImageIconResult.success("Image icon providers reloaded.")
                : ImageIconResult.failure("Image icon providers reloaded with " + failures + " failure(s).");
    }

    public ImageIconResult unregisterProvider(String providerId) {
        String normalizedProvider = normalizeProviderId(providerId);
        if (normalizedProvider == null || providers.remove(normalizedProvider) == null) {
            return ImageIconResult.failure("Image icon provider is not registered: " + providerId);
        }
        icons.remove(normalizedProvider);
        loadErrors.remove(normalizedProvider);
        attachments.values().removeIf(attachment -> {
            boolean remove = attachment.providerId().equals(normalizedProvider);
            if (remove) {
                renderer.clear(attachment);
            }
            return remove;
        });
        restartWatcher();
        return ImageIconResult.success("Image icon provider unregistered: " + normalizedProvider);
    }

    public ImageIconResult attachIcon(UUID target, String providerId, String iconId, ImageIconTuning tuning) {
        return attachIcon(target, providerId, iconId, tuning, Set.of(), 0);
    }

    public ImageIconResult attachIconToPlayer(UUID targetPlayer, String providerId, String iconId, ImageIconTuning tuning) {
        return attachIcon(targetPlayer, providerId, iconId, tuning, Set.of(), 0);
    }

    public ImageIconResult attachIcon(
            UUID target,
            String providerId,
            String iconId,
            ImageIconTuning tuning,
            Set<UUID> viewers,
            int priority) {
        if (!moduleEnabled()) {
            return ImageIconResult.failure("ImageIcons module is disabled.");
        }
        String normalizedProvider = normalizeProviderId(providerId);
        String normalizedIcon = normalizeIconId(iconId);
        if (target == null || normalizedProvider == null || normalizedIcon == null) {
            return ImageIconResult.failure("Target, provider id, and icon id are required.");
        }
        if (!renderer.packetBackendAvailable(normalizedProvider, "attach")) {
            return ImageIconResult.failure("PacketAPI is unavailable for ImageIcons rendering.");
        }
        ImageIconDefinition definition = definition(normalizedProvider, normalizedIcon);
        if (definition == null) {
            return ImageIconResult.failure("Image icon is not loaded: " + normalizedProvider + ":" + normalizedIcon);
        }
        ImageIconTuning resolvedTuning = (tuning == null ? ImageIconTuning.defaults(plugin.getExtrasConfig()) : tuning)
                .withFallbacks(plugin.getExtrasConfig(), definition);
        UUID attachmentId = UUID.randomUUID();
        ImageIconAttachment attachment = new ImageIconAttachment(
                attachmentId,
                target,
                normalizedProvider,
                normalizedIcon,
                viewers,
                priority,
                resolvedTuning.maxDistance(),
                resolvedTuning,
                Instant.now());
        attachments.put(attachmentId, attachment);
        renderer.refresh(attachment, definition);
        return ImageIconResult.attachment(attachmentId);
    }

    public boolean clearIcon(UUID attachmentId) {
        if (attachmentId == null) {
            return false;
        }
        ImageIconAttachment removed = attachments.remove(attachmentId);
        if (removed != null) {
            renderer.clear(removed);
            return true;
        }
        return false;
    }

    public int clearIcons(UUID target) {
        if (target == null) {
            return 0;
        }
        List<UUID> remove = attachments.values().stream()
                .filter(attachment -> attachment.target().equals(target))
                .map(ImageIconAttachment::attachmentId)
                .toList();
        remove.forEach(this::clearIcon);
        return remove.size();
    }

    public void clearPlayer(UUID player) {
        clearIcons(player);
    }

    public Map<String, ImageIconProviderRegistration> snapshotProviders() {
        return Map.copyOf(providers);
    }

    public Map<String, ImageIconDefinition> snapshotIcons(String providerId) {
        String normalizedProvider = normalizeProviderId(providerId);
        if (normalizedProvider == null) {
            return Map.of();
        }
        Map<String, ImageIconDefinition> providerIcons = icons.get(normalizedProvider);
        return providerIcons == null ? Map.of() : Map.copyOf(providerIcons);
    }

    public Map<String, Map<String, ImageIconDefinition>> snapshotIcons() {
        Map<String, Map<String, ImageIconDefinition>> snapshot = new LinkedHashMap<>();
        icons.forEach((providerId, providerIcons) -> snapshot.put(providerId, Map.copyOf(providerIcons)));
        return Map.copyOf(snapshot);
    }

    public Map<UUID, ImageIconAttachment> snapshotAttachments() {
        return Map.copyOf(attachments);
    }

    public List<ImageIconAttachment> snapshotAttachmentsForViewer(UUID viewer) {
        HyExtrasConfig config = plugin.getExtrasConfig();
        int limit = config != null ? config.imageIconsMaxIconsPerViewer : 64;
        return attachments.values().stream()
                .filter(attachment -> attachment.visibleToAll() || attachment.viewers().contains(viewer))
                .sorted(Comparator
                        .comparingInt(ImageIconAttachment::priority).reversed()
                        .thenComparing(ImageIconAttachment::createdAt))
                .limit(Math.max(0, limit))
                .toList();
    }

    public Map<String, List<String>> snapshotLoadErrors() {
        return Map.copyOf(loadErrors);
    }

    private boolean moduleEnabled() {
        return plugin.isModuleEnabled(HyExtrasConfig.MODULE_IMAGE_ICONS);
    }

    private boolean remoteCacheEnabled() {
        HyExtrasConfig config = plugin.getExtrasConfig();
        return config != null && config.imageIconsRemoteCacheEnabled;
    }

    private ImageIconDefinition definition(String providerId, String iconId) {
        Map<String, ImageIconDefinition> providerIcons = icons.get(providerId);
        return providerIcons != null ? providerIcons.get(iconId) : null;
    }

    private ImageIconDefinition loadDefinition(
            String providerId,
            String iconId,
            Path path,
            boolean remote,
            URI remoteUri) throws IOException {
        String extension = extension(path);
        ImageIconDefinition.SourceType sourceType = switch (extension) {
            case ".gif" -> remote ? ImageIconDefinition.SourceType.REMOTE_GIF : ImageIconDefinition.SourceType.GIF;
            case ".png" -> remote ? ImageIconDefinition.SourceType.REMOTE_PNG : ImageIconDefinition.SourceType.PNG;
            default -> throw new IOException("Unsupported image extension: " + extension);
        };

        List<ImageIconFrame> frames = extension.equals(".gif")
                ? readGifFrames(providerId, iconId, path)
                : readPngFrame(providerId, iconId, path);
        ImageIconFrame first = frames.getFirst();
        return new ImageIconDefinition(
                providerId,
                iconId,
                sourceType,
                path.toAbsolutePath().normalize(),
                remoteUri,
                frames,
                first.width(),
                first.height(),
                Files.getLastModifiedTime(path).toMillis());
    }

    private List<ImageIconFrame> readPngFrame(String providerId, String iconId, Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Unable to read PNG image.");
        }
        return List.of(new ImageIconFrame(
                0,
                path,
                glyphFor(providerId, iconId, 0),
                image.getWidth(),
                image.getHeight(),
                0));
    }

    private List<ImageIconFrame> readGifFrames(String providerId, String iconId, Path path) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("No GIF reader available.");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new IOException("Unable to read GIF image.");
            }
            reader.setInput(input);
            int count = Math.max(1, reader.getNumImages(true));
            List<ImageIconFrame> frames = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                frames.add(new ImageIconFrame(
                        i,
                        path,
                        glyphFor(providerId, iconId, i),
                        reader.getWidth(i),
                        reader.getHeight(i),
                        100));
            }
            return frames;
        } finally {
            reader.dispose();
        }
    }

    private Path downloadRemoteIcon(String providerId, String iconId, URI remoteUri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(remoteUri).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Remote server returned HTTP " + response.statusCode());
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        String extension = extensionFromUri(remoteUri);
        if (!isSupportedRemote(contentType, extension)) {
            throw new IOException("Remote icon must be PNG or GIF.");
        }

        Path cacheDir = plugin.getDataDirectory()
                .resolve("image-icons-cache")
                .resolve(providerId)
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(cacheDir);
        Path destination = cacheDir.resolve(iconId + extension);
        Path temp = cacheDir.resolve(iconId + extension + ".tmp");
        long maxBytes = plugin.getExtrasConfig() != null
                ? plugin.getExtrasConfig().imageIconsRemoteCacheMaxBytes
                : 5_242_880L;
        try (InputStream input = response.body()) {
            copyLimited(input, temp, maxBytes);
        }
        Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return destination;
    }

    private static void copyLimited(InputStream input, Path destination, long maxBytes) throws IOException {
        long total = 0;
        byte[] buffer = new byte[16_384];
        try (var output = Files.newOutputStream(
                destination,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Remote icon exceeds max size of " + maxBytes + " bytes.");
                }
                output.write(buffer, 0, read);
            }
        }
    }

    private void refreshProviderAttachments(String providerId) {
        attachments.values().stream()
                .filter(attachment -> attachment.providerId().equals(providerId))
                .forEach(attachment -> {
                    ImageIconDefinition definition = definition(providerId, attachment.iconId());
                    if (definition != null) {
                        renderer.refresh(attachment, definition);
                    }
                });
    }

    private void refreshAttachments(String providerId, String iconId) {
        attachments.values().stream()
                .filter(attachment -> attachment.providerId().equals(providerId) && attachment.iconId().equals(iconId))
                .forEach(attachment -> renderer.refresh(attachment, Objects.requireNonNull(definition(providerId, iconId))));
    }

    private void dropMissingAttachments(String providerId, Set<String> loadedIconIds) {
        attachments.values().removeIf(attachment -> {
            boolean remove = attachment.providerId().equals(providerId)
                    && !loadedIconIds.contains(attachment.iconId());
            if (remove) {
                renderer.clear(attachment);
            }
            return remove;
        });
    }

    private void recordLoadError(String providerId, String iconId, String message) {
        loadErrors.compute(providerId, (ignored, existing) -> {
            List<String> next = new ArrayList<>(existing == null ? List.of() : existing);
            next.add(iconId + ": " + message);
            return List.copyOf(next);
        });
    }

    private void clearLoadError(String providerId, String iconId) {
        loadErrors.computeIfPresent(providerId, (ignored, existing) -> {
            List<String> next = existing.stream()
                    .filter(error -> !error.startsWith(iconId + ":"))
                    .toList();
            return next.isEmpty() ? null : next;
        });
    }

    private synchronized void restartWatcher() {
        closeWatcher();
        if (!running || plugin.getExtrasConfig() == null || !plugin.getExtrasConfig().imageIconsHotReload) {
            return;
        }
        Map<WatchKey, String> keyProviders = new ConcurrentHashMap<>();
        try {
            WatchService nextWatchService = plugin.getDataDirectory().getFileSystem().newWatchService();
            for (ImageIconProviderRegistration registration : providers.values()) {
                if (registration.hotReload()) {
                    registerProviderTree(nextWatchService, keyProviders, registration);
                }
            }
            if (keyProviders.isEmpty()) {
                nextWatchService.close();
                return;
            }
            watchService = nextWatchService;
            watchThread = new Thread(() -> watchLoop(nextWatchService, keyProviders), "HyExtras-ImageIcons-Watch");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras image-icons] Failed to start hot reload watcher.");
        }
    }

    private void registerProviderTree(
            WatchService service,
            Map<WatchKey, String> keyProviders,
            ImageIconProviderRegistration registration) throws IOException {
        Files.walkFileTree(registration.assetsPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                keyProviders.put(key, registration.providerId());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void watchLoop(WatchService service, Map<WatchKey, String> keyProviders) {
        while (running && service == watchService) {
            WatchKey key;
            try {
                key = service.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                return;
            }
            String providerId = keyProviders.get(key);
            boolean reload = false;
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    reload = true;
                    continue;
                }
                Object context = event.context();
                if (context instanceof Path path && isSupportedImagePath(path)) {
                    reload = true;
                } else if (event.kind() == ENTRY_CREATE) {
                    reload = true;
                }
            }
            key.reset();
            if (reload && providerId != null) {
                reloadProvider(providerId);
                restartWatcher();
            }
        }
    }

    private synchronized void closeWatcher() {
        WatchService current = watchService;
        watchService = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean isSupportedRemote(String contentType, String extension) {
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(extension)
                || normalizedContentType.contains("image/png")
                || normalizedContentType.contains("image/gif");
    }

    private static boolean isSupportedImagePath(Path path) {
        return SUPPORTED_EXTENSIONS.contains(extension(path));
    }

    private static String extensionFromUri(URI uri) {
        String path = uri.getPath();
        String extension = extension(Path.of(path == null || path.isBlank() ? "remote.png" : path));
        return SUPPORTED_EXTENSIONS.contains(extension) ? extension : ".png";
    }

    private static String extension(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private static String iconIdForPath(Path root, Path path) {
        Path relative = root.relativize(path);
        String value = relative.toString().replace('\\', '/');
        int dot = value.lastIndexOf('.');
        if (dot >= 0) {
            value = value.substring(0, dot);
        }
        return normalizeIconId(value);
    }

    private static String normalizeProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return null;
        }
        String normalized = providerId.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_.-]+") ? normalized : null;
    }

    private static String normalizeIconId(String iconId) {
        if (iconId == null || iconId.isBlank()) {
            return null;
        }
        String normalized = iconId.trim()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.\\-/]", "_")
                .replace('/', '.');
        while (normalized.contains("..")) {
            normalized = normalized.replace("..", ".");
        }
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private static String glyphFor(String providerId, String iconId, int frame) {
        int codePoint = 0xE000 + Math.floorMod(Objects.hash(providerId, iconId, frame), 0x1900);
        return new String(Character.toChars(codePoint));
    }
}
