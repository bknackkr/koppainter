package com.github.bknackkr.koppainter;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link KoppainterDialog}.
 */
public class KoppainterDialogTest {
    /**
     * Default constructor for test suite.
     */
    public KoppainterDialogTest() {
    }

    /**
     * Verifies biome display color lookup.
     */
    @Test
    public void testGetBiomeColor() {
        BiomeEntry desert = BiomeResolver.resolve("desert");
        BiomeEntry ocean = BiomeResolver.resolve("ocean");
        BiomeEntry custom = BiomeResolver.resolve("mod:custom_biome");

        int desertColor = KoppainterDialog.getBiomeColor(desert);
        int oceanColor = KoppainterDialog.getBiomeColor(ocean);
        int customColor = KoppainterDialog.getBiomeColor(custom);

        assertNotNull(desert);
        assertTrue((desertColor != 0));
        assertTrue((oceanColor != 0));
        assertTrue((customColor != 0));
    }

    /**
     * Verifies dialog initialization and file preloading when not in a headless environment.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file creation fails.
     */
    @Test
    public void testDialogInitialization(@TempDir Path tempDir) throws IOException {
        if ((GraphicsEnvironment.isHeadless())) {
            // JDialog requires a display device; skip GUI instantiation in headless CI environments
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        BufferedImage sampleImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        // Paint red (desert in default properties)
        sampleImage.setRGB(0, 0, 0xFF0000);
        File sampleFile = tempDir.resolve("climate.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), sampleFile);
        assertNotNull(dialog.getClimateImage());
        assertNotNull(dialog.getBiomePreviewImage());
        assertNotNull(dialog.getDefaultBiome());
        assertEquals(sampleFile, dialog.getSelectedFile());
        assertFalse(dialog.isConfirmed());

        dialog.dispose();
    }
}
