package org.hyzionstudios.hyextras.imageicons;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageIconProviderRegistration {

    private final String providerId;
    private final Path assetsPath;
    private final boolean hotReload;
    private final ConcurrentHashMap<String, URI> remoteSources = new ConcurrentHashMap<>();

    public ImageIconProviderRegistration(String providerId, Path assetsPath, boolean hotReload) {
        this.providerId = providerId;
        this.assetsPath = assetsPath;
        this.hotReload = hotReload;
    }

    public String providerId() {
        return providerId;
    }

    public Path assetsPath() {
        return assetsPath;
    }

    public boolean hotReload() {
        return hotReload;
    }

    public void putRemoteSource(String iconId, URI remoteUri) {
        remoteSources.put(iconId, remoteUri);
    }

    public Map<String, URI> remoteSources() {
        return Map.copyOf(remoteSources);
    }
}
