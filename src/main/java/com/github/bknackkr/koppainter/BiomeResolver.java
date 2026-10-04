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
        try {
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
        } catch (Throwable ignored) {
            // If WorldPainter internal patterns cannot be loaded (e.g. standalone execution), populate fallback
            populateFallbackBiomes(byId);
        }
        return (Collections.unmodifiableMap(byId));
    }

    private static void populateFallbackBiomes(Map<Integer, BiomeEntry> map) {
        Object[][] biomes = {
                {0, "minecraft:ocean", "Ocean"},
                {1, "minecraft:plains", "Plains"},
                {2, "minecraft:desert", "Desert"},
                {3, "minecraft:windswept_hills", "Windswept Hills"},
                {4, "minecraft:forest", "Forest"},
                {5, "minecraft:taiga", "Taiga"},
                {6, "minecraft:swamp", "Swamp"},
                {7, "minecraft:river", "River"},
                {8, "minecraft:nether_wastes", "Nether Wastes"},
                {9, "minecraft:the_end", "The End"},
                {10, "minecraft:frozen_ocean", "Frozen Ocean"},
                {11, "minecraft:frozen_river", "Frozen River"},
                {12, "minecraft:snowy_plains", "Snowy Plains"},
                {13, "minecraft:snowy_mountains", "Snowy Mountains"},
                {14, "minecraft:mushroom_fields", "Mushroom Fields"},
                {15, "minecraft:mushroom_field_shore", "Mushroom Field Shore"},
                {16, "minecraft:beach", "Beach"},
                {17, "minecraft:desert_hills", "Desert Hills"},
                {18, "minecraft:wooded_hills", "Wooded Hills"},
                {19, "minecraft:taiga_hills", "Taiga Hills"},
                {21, "minecraft:jungle", "Jungle"},
                {22, "minecraft:jungle_hills", "Jungle Hills"},
                {23, "minecraft:sparse_jungle", "Sparse Jungle"},
                {24, "minecraft:deep_ocean", "Deep Ocean"},
                {25, "minecraft:stony_shore", "Stony Shore"},
                {26, "minecraft:snowy_beach", "Snowy Beach"},
                {27, "minecraft:birch_forest", "Birch Forest"},
                {28, "minecraft:birch_forest_hills", "Birch Forest Hills"},
                {29, "minecraft:dark_forest", "Dark Forest"},
                {30, "minecraft:snowy_taiga", "Snowy Taiga"},
                {31, "minecraft:snowy_taiga_hills", "Snowy Taiga Hills"},
                {32, "minecraft:old_growth_pine_taiga", "Old Growth Pine Taiga"},
                {33, "minecraft:giant_tree_taiga_hills", "Giant Tree Taiga Hills"},
                {34, "minecraft:wooded_mountains", "Wooded Mountains"},
                {35, "minecraft:savanna", "Savanna"},
                {36, "minecraft:savanna_plateau", "Savanna Plateau"},
                {37, "minecraft:badlands", "Badlands"},
                {38, "minecraft:wooded_badlands", "Wooded Badlands"},
                {39, "minecraft:badlands_plateau", "Badlands Plateau"},
                {44, "minecraft:warm_ocean", "Warm Ocean"},
                {45, "minecraft:lukewarm_ocean", "Lukewarm Ocean"},
                {46, "minecraft:cold_ocean", "Cold Ocean"},
                {47, "minecraft:deep_warm_ocean", "Deep Warm Ocean"},
                {48, "minecraft:deep_lukewarm_ocean", "Deep Lukewarm Ocean"},
                {49, "minecraft:deep_cold_ocean", "Deep Cold Ocean"},
                {50, "minecraft:deep_frozen_ocean", "Deep Frozen Ocean"},
                {127, "minecraft:the_void", "The Void"},
                {129, "minecraft:sunflower_plains", "Sunflower Plains"},
                {130, "minecraft:desert_lakes", "Desert Lakes"},
                {131, "minecraft:windswept_gravelly_hills", "Windswept Gravelly Hills"},
                {132, "minecraft:flower_forest", "Flower Forest"},
                {140, "minecraft:ice_spikes", "Ice Spikes"},
                {160, "minecraft:old_growth_spruce_taiga", "Old Growth Spruce Taiga"},
                {163, "minecraft:windswept_savanna", "Windswept Savanna"},
                {165, "minecraft:eroded_badlands", "Eroded Badlands"},
                {168, "minecraft:bamboo_jungle", "Bamboo Jungle"},
                {170, "minecraft:soul_sand_valley", "Soul Sand Valley"},
                {171, "minecraft:crimson_forest", "Crimson Forest"},
                {172, "minecraft:warped_forest", "Warped Forest"},
                {173, "minecraft:basalt_deltas", "Basalt Deltas"},
                {174, "minecraft:dripstone_caves", "Dripstone Caves"},
                {175, "minecraft:lush_caves", "Lush Caves"},
                {177, "minecraft:meadow", "Meadow"},
                {178, "minecraft:grove", "Grove"},
                {179, "minecraft:snowy_slopes", "Snowy Slopes"},
                {180, "minecraft:jagged_peaks", "Jagged Peaks"},
                {181, "minecraft:frozen_peaks", "Frozen Peaks"},
                {182, "minecraft:stony_peaks", "Stony Peaks"},
                {183, "minecraft:deep_dark", "Deep Dark"},
                {184, "minecraft:mangrove_swamp", "Mangrove Swamp"},
                {185, "minecraft:cherry_grove", "Cherry Grove"},
                {186, "minecraft:pale_garden", "Pale Garden"}
        };
        for (Object[] biome : biomes) {
            int id = (Integer) biome[0];
            String modernId = (String) biome[1];
            String name = (String) biome[2];
            map.put(id, new BiomeEntry(id, modernId, name));
        }
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
