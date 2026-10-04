package com.github.bknackkr.koppainter;

import org.junit.jupiter.api.Test;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BiomeEntry}.
 */
public class BiomeEntryTest {
    /**
     * Default constructor for test suite.
     */
    public BiomeEntryTest() {
    }

    /**
     * Verifies getters and standard property values for vanilla biomes.
     */
    @Test
    public void testGettersAndVanillaProperties() {
        BiomeEntry entry = new BiomeEntry(2, "minecraft:desert", "Desert");
        assertEquals(2, entry.getId());
        assertEquals("minecraft:desert", entry.getModernId());
        assertEquals("Desert", entry.getName());
        assertFalse(entry.isCustom());
    }

    /**
     * Verifies properties for custom/modded biomes with negative IDs.
     */
    @Test
    public void testCustomBiomeProperties() {
        BiomeEntry entry = new BiomeEntry(-1, "custom:volcano", "Volcano");
        assertEquals(-1, entry.getId());
        assertEquals("custom:volcano", entry.getModernId());
        assertEquals("Volcano", entry.getName());
        assertTrue(entry.isCustom());
    }

    /**
     * Verifies equals and hashCode contracts.
     */
    @Test
    public void testEqualsAndHashCode() {
        BiomeEntry entry1 = new BiomeEntry(2, "minecraft:desert", "Desert");
        BiomeEntry entry2 = new BiomeEntry(2, "minecraft:desert", "Desert");
        BiomeEntry entry3 = new BiomeEntry(1, "minecraft:plains", "Plains");

        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());
        assertNotEquals(entry1, entry3);
        assertNotEquals(null, entry1);
        assertEquals(entry1, entry1);
    }

    /**
     * Verifies constructor input validations.
     */
    @Test
    public void testConstructorValidations() {
        assertThrows(MDCCapturingRuntimeException.class, () -> new BiomeEntry(1, null, "Plains"));
        assertThrows(MDCCapturingRuntimeException.class, () -> new BiomeEntry(1, "   ", "Plains"));
        assertThrows(MDCCapturingRuntimeException.class, () -> new BiomeEntry(1, "minecraft:plains", null));
        assertThrows(MDCCapturingRuntimeException.class, () -> new BiomeEntry(1, "minecraft:plains", "   "));
    }
}
