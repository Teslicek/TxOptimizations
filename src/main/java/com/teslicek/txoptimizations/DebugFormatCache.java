package com.teslicek.txoptimizations;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

public final class DebugFormatCache {

    private static final Map<String, Entry> ENTRIES = new IdentityHashMap<>();

    private DebugFormatCache() {
    }

    public static String format(Locale locale, String format, Object[] args) {
        Entry entry = ENTRIES.get(format);

        if (entry != null && entry.locale == locale && Arrays.equals(entry.args, args))
            return entry.text;

        String text = String.format(locale, format, args);

        if (!isValues(args)) {
            ENTRIES.remove(format);

            return text;
        }

        if (entry == null) {
            ENTRIES.put(format, new Entry(locale, args, text));

            return text;
        }

        entry.locale = locale;
        entry.args   = args;
        entry.text   = text;

        return text;
    }

    private static boolean isValues(Object[] args) {
        for (Object arg : args) {
            if (!(arg instanceof String || arg instanceof Integer || arg instanceof Long || arg instanceof Double || arg instanceof Float || arg instanceof Short || arg instanceof Byte || arg instanceof Boolean || arg instanceof Character || arg instanceof Enum<?>))
                return false;
        }

        return true;
    }

    private static final class Entry {

        private Locale   locale;
        private Object[] args;
        private String   text;

        private Entry(Locale locale, Object[] args, String text) {
            this.locale = locale;
            this.args   = args;
            this.text   = text;
        }
    }
}
