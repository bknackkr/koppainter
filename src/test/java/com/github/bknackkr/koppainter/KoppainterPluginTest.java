package com.github.bknackkr.koppainter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link KoppainterPlugin}.
 */
public class KoppainterPluginTest {
    /**
     * Default constructor for test suite.
     */
    public KoppainterPluginTest() {
    }

    /**
     * Verifies that the plugin initialises with expected metadata and valid operation list.
     */
    @Test
    public void testPluginMetadata() {
        KoppainterPlugin plugin = new KoppainterPlugin();
        assertEquals("Köppainter", plugin.getName());
        assertNotNull(plugin.getVersion());
        assertNotNull(plugin.getOperations());
        assertEquals(1, plugin.getOperations().size());
        assertEquals("Import Köppen Climate Map", plugin.getOperations().get(0).getName());
        assertNotNull(plugin.getColorBiomeMap());
        assertEquals("minecraft:desert", plugin.getColorBiomeMap().getBiome("FF0000").getModernId());
    }

    /**
     * Verifies setting a custom color biome map on the plugin.
     */
    @Test
    public void testSetColorBiomeMap() {
        KoppainterPlugin plugin = new KoppainterPlugin();
        ColorBiomeMap customMap = new ColorBiomeMap();
        customMap.put("123456", "plains");
        plugin.setColorBiomeMap(customMap);
        assertEquals(customMap, plugin.getColorBiomeMap());
        assertEquals("minecraft:plains", plugin.getColorBiomeMap().getBiome("123456").getModernId());
    }
}
