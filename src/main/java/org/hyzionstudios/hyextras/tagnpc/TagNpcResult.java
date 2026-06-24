package org.hyzionstudios.hyextras.tagnpc;

public record TagNpcResult(boolean success, String message) {

    public static TagNpcResult success(String message) {
        return new TagNpcResult(true, message);
    }

    public static TagNpcResult failure(String message) {
        return new TagNpcResult(false, message);
    }
}
