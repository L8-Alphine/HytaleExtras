package org.hyzionstudios.hyextras.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlaceholderRegistry {

    private static final PlaceholderRegistry GLOBAL = new PlaceholderRegistry();

    private final CopyOnWriteArrayList<PlaceholderResolver> resolvers = new CopyOnWriteArrayList<>();

    public static PlaceholderRegistry global() {
        return GLOBAL;
    }

    public void register(PlaceholderResolver resolver) {
        resolvers.addIfAbsent(resolver);
    }

    public void unregister(PlaceholderResolver resolver) {
        resolvers.remove(resolver);
    }

    public List<PlaceholderResolver> resolvers() {
        return List.copyOf(resolvers);
    }

    public String resolve(String text, StringTemplateContext context) {
        String result = text;
        for (PlaceholderResolver resolver : resolvers) {
            result = resolver.resolve(result, context);
        }
        return result;
    }
}
