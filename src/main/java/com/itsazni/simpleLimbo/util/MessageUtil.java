package com.itsazni.simpleLimbo.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private MessageUtil() {
    }

    public static Component component(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(message);
    }

    public static String replace(String message, String key, String value) {
        if (message == null) {
            return "";
        }
        return message.replace(key, value);
    }
}
