package org.bukkit.util;

import java.util.Collection;
public final class StringUtil {
    private StringUtil() {
    }

    public static <T extends Collection<? super String>> T copyPartialMatches(String token, Iterable<String> originals, T collection) {
        for (String option : originals) {
            if (option.regionMatches(true, 0, token, 0, token.length())) {
                collection.add(option);
            }
        }
        return collection;
    }
}
