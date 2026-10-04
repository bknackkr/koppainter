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
    }
}
