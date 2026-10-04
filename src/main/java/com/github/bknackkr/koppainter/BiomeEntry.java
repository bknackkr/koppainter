package com.github.bknackkr.koppainter;

import java.util.Objects;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Represents a Minecraft biome entry containing its numerical ID, modern namespaced identifier, and display name.
 */
public final class BiomeEntry {
    /**
     * Constructs a new {@code BiomeEntry}.
     *
     * @param id The numerical biome ID (or -1 if unallocated/custom).
     * @param modernId The modern Minecraft technical identifier (e.g. {@code "minecraft:desert"}).
     * @param name The human-readable display name (e.g. {@code "Desert"}).
     */
    public BiomeEntry(int id, String modernId, String name) {
        if ((modernId == null) || (modernId.isBlank())) {
            throw new MDCCapturingRuntimeException("Modern biome ID cannot be null or blank");
        }
        if ((name == null) || (name.isBlank())) {
            throw new MDCCapturingRuntimeException("Biome name cannot be null or blank");
        }
        this.id = id;
        this.modernId = modernId.trim();
        this.name = name.trim();
    }

    /**
     * Returns the numerical biome ID.
     *
     * @return The numerical biome ID, or -1 for custom/unallocated biomes.
     */
    public int getId() {
        return (id);
    }

    /**
     * Returns the modern namespaced identifier (e.g. {@code "minecraft:desert"}).
     *
     * @return The modern biome identifier.
     */
    public String getModernId() {
        return (modernId);
    }

    /**
     * Returns the human-readable display name of the biome (e.g. {@code "Desert"}).
     *
     * @return The display name.
     */
    public String getName() {
        return (name);
    }

    /**
     * Indicates whether this biome represents a custom/modded biome without a standard vanilla numerical ID.
     *
     * @return {@code true} if custom, {@code false} if a standard vanilla biome.
     */
    public boolean isCustom() {
        return (id < 0);
    }

    @Override
    public boolean equals(Object obj) {
        if ((this == obj)) {
            return (true);
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return (false);
        }
        BiomeEntry other = (BiomeEntry) obj;
        return ((id == other.id)
                && Objects.equals(modernId, other.modernId)
                && Objects.equals(name, other.name));
    }

    @Override
    public int hashCode() {
        return (Objects.hash(id, modernId, name));
    }

    @Override
    public String toString() {
        return (modernId + " (id=" + id + ", name=\"" + name + "\")");
    }

    private final int id;
    private final String modernId;
    private final String name;
}
