package com.github.bknackkr.koppainter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes;

/**
 * Resolves Minecraft biome names, modern IDs, and numerical IDs into {@link BiomeEntry} instances.
 */
public final class BiomeResolver {
    private BiomeResolver() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Resolves a biome identifier into a {@link BiomeEntry}.
     *
     * <p>The identifier may be a modern namespaced ID (e.g. {@code "minecraft:desert"}),
     * a short name (e.g. {@code "desert"}), a display name (e.g. {@code "Windswept Hills"}),
     * a numerical ID (e.g. {@code "2"}), or a custom/modded biome identifier.</p>
     *
     * @param identifier The biome identifier string.
     * @return The resolved biome entry.
     */
    public static BiomeEntry resolve(String identifier) {
        if ((identifier == null) || (identifier.isBlank())) {
            throw new MDCCapturingRuntimeException("Biome identifier cannot be null or blank");
        }
        String trimmed = identifier.trim();

        // Check if the identifier is a numerical biome ID
        if ((isInteger(trimmed))) {
            int numericId = Integer.parseInt(trimmed);
            return (resolveById(numericId));
        }

        String lowerKey = trimmed.toLowerCase(Locale.ROOT);
        BiomeEntry directMatch = BIOMES_BY_KEY.get(lowerKey);
        if ((directMatch != null)) {
            return (directMatch);
        }

        // Check with or without "minecraft:" prefix
        if ((lowerKey.startsWith("minecraft:"))) {
            String strippedKey = lowerKey.substring("minecraft:".length());
            BiomeEntry strippedMatch = BIOMES_BY_KEY.get(strippedKey);
            if ((strippedMatch != null)) {
                return (strippedMatch);
            }
        } else {
            String prefixedKey = ("minecraft:" + lowerKey);
            BiomeEntry prefixedMatch = BIOMES_BY_KEY.get(prefixedKey);
            if ((prefixedMatch != null)) {
                return (prefixedMatch);
            }
        }

        // Try normalized key without spaces, underscores, or hyphens
        String normalizedKey = normalizeKey(trimmed);
        BiomeEntry normalizedMatch = BIOMES_BY_KEY.get(normalizedKey);
        if ((normalizedMatch != null)) {
            return (normalizedMatch);
        }

        // If not recognized as standard vanilla, support custom or modded biomes
        String modernId = (trimmed.contains(":") ? lowerKey : ("minecraft:" + lowerKey));
        return (new BiomeEntry(-1, modernId, trimmed));
    }

    /**
     * Resolves a numerical biome ID into a {@link BiomeEntry}.
     *
     * @param id The numerical biome ID.
     * @return The resolved biome entry.
     */
    public static BiomeEntry resolveById(int id) {
        BiomeEntry known = BIOMES_BY_ID.get(id);
        if ((known != null)) {
            return (known);
        }
        if (((id < 0) || (id > 255))) {
            throw new MDCCapturingRuntimeException("Numerical biome ID out of valid range (0-255): " + id);
        }
        return (new BiomeEntry(id, ("minecraft:biome_" + id), ("Biome " + id)));
    }

    /**
     * Returns a sorted, unmodifiable list of all standard Minecraft biomes.
     *
     * @return The list of standard biomes, sorted alphabetically by name.
     */
    public static List<BiomeEntry> getAllStandardBiomes() {
        Map<String, BiomeEntry> unique = new HashMap<>();
        for (BiomeEntry entry : BIOMES_BY_ID.values()) {
            if ((!unique.containsKey(entry.getModernId()))) {
                unique.put(entry.getModernId(), entry);
            }
        }
        List<BiomeEntry> list = new ArrayList<>(unique.values());
        list.sort(Comparator.comparing(BiomeEntry::getName));
        return (Collections.unmodifiableList(list));
    }

    private static boolean isInteger(String text) {
        if ((text.isEmpty())) {
            return (false);
        }
        for (int i = 0; (i < text.length()); i++) {
            char c = text.charAt(i);
            if ((i == 0) && (c == '-')) {
                if ((text.length() == 1)) {
                    return (false);
                }
                continue;
            }
            if ((!Character.isDigit(c))) {
                return (false);
            }
        }
        return (true);
    }

    private static String normalizeKey(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; (i < text.length()); i++) {
            char c = text.charAt(i);
            if ((Character.isLetterOrDigit(c))) {
                builder.append(Character.toLowerCase(c));
            }
        }
        return (builder.toString());
    }

    private static void registerKey(Map<String, BiomeEntry> map, String key, BiomeEntry entry) {
        if ((key != null) && (!key.isBlank())) {
            map.putIfAbsent(key.toLowerCase(Locale.ROOT), entry);
            map.putIfAbsent(normalizeKey(key), entry);
        }
    }

    private static Map<Integer, BiomeEntry> initBiomesById() {
        Map<Integer, BiomeEntry> byId = new HashMap<>();
        String[] modernIds = Minecraft1_21Biomes.MODERN_IDS;
        String[] biomeNames = Minecraft1_21Biomes.BIOME_NAMES;

        for (int i = 0; (i < 256); i++) {
            String modernId = ((i < modernIds.length) ? modernIds[i] : null);
            if ((modernId != null)) {
                String displayName = ((i < biomeNames.length) ? biomeNames[i] : null);
                if ((displayName == null)) {
                    displayName = modernId;
                }
                byId.put(i, new BiomeEntry(i, modernId, displayName));
            }
        }
        return (Collections.unmodifiableMap(byId));
    }

    private static Map<String, BiomeEntry> initBiomesByKey(Map<Integer, BiomeEntry> byId) {
        Map<String, BiomeEntry> byKey = new HashMap<>();
        for (BiomeEntry entry : byId.values()) {
            registerKey(byKey, entry.getModernId(), entry);
            if ((entry.getModernId().startsWith("minecraft:"))) {
                registerKey(byKey, entry.getModernId().substring("minecraft:".length()), entry);
            }
            registerKey(byKey, entry.getName(), entry);
        }
        return (Collections.unmodifiableMap(byKey));
    }

    private static final Map<Integer, BiomeEntry> BIOMES_BY_ID;
    private static final Map<String, BiomeEntry> BIOMES_BY_KEY;

    static {
        Map<Integer, BiomeEntry> byId = initBiomesById();
        BIOMES_BY_ID = byId;
        BIOMES_BY_KEY = initBiomesByKey(byId);
    }
}
