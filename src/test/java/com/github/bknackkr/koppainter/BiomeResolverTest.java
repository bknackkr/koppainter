package com.github.bknackkr.koppainter;

import org.junit.jupiter.api.Test;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BiomeResolver}.
 */
public class BiomeResolverTest {
    /**
     * Default constructor for test suite.
     */
    public BiomeResolverTest() {
    }

    /**
     * Verifies resolution of common vanilla biomes by short name, modern ID, display name, and casing.
     */
    @Test
    public void testResolveVanillaBiomes() {
        // Short name
        BiomeEntry desertFromShort = BiomeResolver.resolve("desert");
        assertEquals("minecraft:desert", desertFromShort.getModernId());
        assertEquals(2, desertFromShort.getId());

        // Namespaced modern ID
        BiomeEntry desertFromModern = BiomeResolver.resolve("minecraft:desert");
        assertEquals(desertFromShort, desertFromModern);

        // Display name
        BiomeEntry desertFromDisplay = BiomeResolver.resolve("Desert");
        assertEquals(desertFromShort, desertFromDisplay);

        // Uppercase
        BiomeEntry desertFromUpper = BiomeResolver.resolve("DESERT");
        assertEquals(desertFromShort, desertFromUpper);
    }

    /**
     * Verifies resolution of multi-word biomes with spaces and underscores.
     */
    @Test
    public void testResolveMultiWordBiomes() {
        BiomeEntry hillsFromUnderscore = BiomeResolver.resolve("windswept_hills");
        BiomeEntry hillsFromDisplay = BiomeResolver.resolve("Windswept Hills");
        BiomeEntry hillsFromSpacedLower = BiomeResolver.resolve("windswept hills");

        assertEquals(hillsFromUnderscore, hillsFromDisplay);
        assertEquals(hillsFromUnderscore, hillsFromSpacedLower);
        assertEquals(3, hillsFromUnderscore.getId());
        assertEquals("minecraft:windswept_hills", hillsFromUnderscore.getModernId());
    }

    /**
     * Verifies resolution of modern 1.21+ biomes such as Pale Garden and Cherry Grove.
     */
    @Test
    public void testResolveModern121Biomes() {
        BiomeEntry paleGarden = BiomeResolver.resolve("pale_garden");
        assertNotNull(paleGarden);
        assertEquals("minecraft:pale_garden", paleGarden.getModernId());
        assertEquals(245, paleGarden.getId());

        BiomeEntry cherryGrove = BiomeResolver.resolve("Cherry Grove");
        assertNotNull(cherryGrove);
        assertEquals("minecraft:cherry_grove", cherryGrove.getModernId());
        assertEquals(246, cherryGrove.getId());
    }

    /**
     * Verifies resolution by numerical biome ID.
     */
    @Test
    public void testResolveByNumericalId() {
        BiomeEntry entry = BiomeResolver.resolve("2");
        assertEquals(2, entry.getId());
        assertEquals("minecraft:desert", entry.getModernId());

        BiomeEntry entryById = BiomeResolver.resolveById(1);
        assertEquals(1, entryById.getId());
        assertEquals("minecraft:plains", entryById.getModernId());
    }

    /**
     * Verifies resolution of custom or modded biome identifiers.
     */
    @Test
    public void testResolveCustomBiome() {
        BiomeEntry modded = BiomeResolver.resolve("biomesoplenty:bayou");
        assertTrue(modded.isCustom());
        assertEquals("biomesoplenty:bayou", modded.getModernId());

        BiomeEntry customName = BiomeResolver.resolve("mystic_grove");
        assertTrue(customName.isCustom());
        assertEquals("minecraft:mystic_grove", customName.getModernId());
    }

    /**
     * Verifies exception handling for invalid inputs.
     */
    @Test
    public void testInvalidInputs() {
        assertThrows(MDCCapturingRuntimeException.class, () -> BiomeResolver.resolve(null));
        assertThrows(MDCCapturingRuntimeException.class, () -> BiomeResolver.resolve("   "));
        assertThrows(MDCCapturingRuntimeException.class, () -> BiomeResolver.resolveById(-1));
        assertThrows(MDCCapturingRuntimeException.class, () -> BiomeResolver.resolveById(300));
    }
}
