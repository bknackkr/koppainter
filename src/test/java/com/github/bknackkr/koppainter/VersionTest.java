package com.github.bknackkr.koppainter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link Version}.
 */
public class VersionTest {
    /**
     * Default constructor for test suite.
     */
    public VersionTest() {
    }

    /**
     * Verifies that the plugin version is correctly loaded and not empty.
     */
    @Test
    public void testVersionIsLoaded() {
        assertNotNull(Version.VERSION);
        assertFalse(Version.VERSION.isBlank());
    }
}
