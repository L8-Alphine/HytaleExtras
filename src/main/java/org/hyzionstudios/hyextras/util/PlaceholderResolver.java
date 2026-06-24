package org.hyzionstudios.hyextras.util;

public interface PlaceholderResolver {
    String resolve(String text, StringTemplateContext context);
}
