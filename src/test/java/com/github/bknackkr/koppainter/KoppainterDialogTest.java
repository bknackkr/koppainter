package com.github.bknackkr.koppainter;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pepsoft.worldpainter.DefaultPlugin;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.TileFactory;
import org.pepsoft.worldpainter.TileFactoryFactory;
import org.pepsoft.worldpainter.World2;
import org.pepsoft.worldpainter.layers.Biome;

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
        assertEquals(10.0, dialog.getColorTolerance());
        dialog.setColorTolerance(15.0);
        assertEquals(15.0, dialog.getColorTolerance());
        assertFalse(dialog.isConfirmed());

        dialog.dispose();
    }

    /**
     * Verifies that the dialog preview translates slightly-off coastline colors to the appropriate biomes.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file creation fails.
     */
    @Test
    public void testPreviewSlightlyOffColors(@TempDir Path tempDir) throws IOException {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        // Create an image with slightly-off desert color (FC0201 instead of FF0000)
        BufferedImage sampleImage = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; (y < 4); y++) {
            for (int x = 0; (x < 4); x++) {
                sampleImage.setRGB(x, y, 0xFC0201);
            }
        }
        File sampleFile = tempDir.resolve("off_climate.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), sampleFile);
        BufferedImage preview = dialog.getBiomePreviewImage();
        assertNotNull(preview);

        // Preview should display the Desert biome display color (not fallback ocean)
        BiomeEntry desert = BiomeResolver.resolve("desert");
        int desertColor = KoppainterDialog.getBiomeColor(desert);
        assertEquals(desertColor, preview.getRGB(0, 0));

        dialog.dispose();
    }

    /**
     * Verifies exporting the generated biome map preview as a PNG file.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file operations fail.
     */
    @Test
    public void testExportBiomeMapAsPng(@TempDir Path tempDir) throws IOException {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        BufferedImage sampleImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        sampleImage.setRGB(0, 0, 0xFF0000);
        File sampleFile = tempDir.resolve("sample_climate.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), sampleFile);
        File exportFile = tempDir.resolve("exported_biomes.png").toFile();

        dialog.exportBiomeMapAsPng(exportFile);
        assertTrue(exportFile.exists());
        assertTrue((exportFile.length() > 0));

        BufferedImage reloaded = ImageLoader.load(exportFile);
        assertNotNull(reloaded);
        assertEquals(8, reloaded.getWidth());
        assertEquals(8, reloaded.getHeight());

        dialog.dispose();
    }

    /**
     * Verifies the adjacent-only mode setting and getter/setter.
     */
    @Test
    public void testAdjacentOnlySetting() {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault());
        assertFalse(dialog.isAdjacentOnly());

        dialog.setAdjacentOnly(true);
        assertTrue(dialog.isAdjacentOnly());

        dialog.setAdjacentOnly(false);
        assertFalse(dialog.isAdjacentOnly());

        dialog.dispose();
    }

    /**
     * Verifies that the embedded progress bar and action label initialize properly and update during preview.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file creation fails.
     */
    @Test
    public void testProgressBarAndActionLabel(@TempDir Path tempDir) throws IOException {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        BufferedImage sampleImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        sampleImage.setRGB(0, 0, 0xFF0000);
        File sampleFile = tempDir.resolve("progress_test.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), sampleFile);
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {});
        } catch (Exception exception) {
            // Wait for EDT to process the invokeLater progress updates
        }
        assertNotNull((dialog.getProgressBar()));
        assertNotNull((dialog.getProgressLabel()));
        assertEquals((100), (dialog.getProgressBar().getValue()));
        assertTrue((dialog.getProgressLabel().getText().contains("Preview ready")));

        dialog.dispose();
    }

    /**
     * Verifies that confirming the dialog with a target dimension runs the background apply task and sets applied.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file creation fails.
     * @throws InterruptedException If waiting for the apply task is interrupted.
     */
    @Test
    public void testApplyBiomesTaskWithProgress(@TempDir Path tempDir) throws IOException, InterruptedException {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        TileFactory tileFactory = TileFactoryFactory.createFlatTileFactory(
                0L,
                Terrain.GRASS,
                0,
                256,
                62,
                62,
                false,
                false
        );
        World2 world = new World2(DefaultPlugin.JAVA_ANVIL, 0L, tileFactory);
        Dimension dimension = world.getDimension(Dimension.Anchor.NORMAL_DETAIL);
        Tile tile00 = new Tile(0, 0, 0, 256);
        dimension.addTile(tile00);

        BufferedImage sampleImage = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        sampleImage.setRGB(0, 0, 0xFF0000); // Desert (id 2)
        File sampleFile = tempDir.resolve("apply_task_test.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), dimension, sampleFile);
        assertFalse((dialog.isApplying()));
        assertFalse((dialog.isApplied()));

        dialog.ok();
        dialog.waitForApply();

        assertTrue((dialog.isConfirmed()));
        assertTrue((dialog.isApplied()));
        assertFalse((dialog.isApplying()));
        assertEquals((2), (tile00.getLayerValue(Biome.INSTANCE, 0, 0)));

        dialog.dispose();
    }

    /**
     * Verifies that closing or cancelling the dialog window during biome application persists the background task.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file creation fails.
     * @throws InterruptedException If waiting for the apply task is interrupted.
     */
    @Test
    public void testCloseWindowWhileApplyingShowsPersistentProgress(@TempDir Path tempDir)
            throws IOException, InterruptedException {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        TileFactory tileFactory = TileFactoryFactory.createFlatTileFactory(
                0L,
                Terrain.GRASS,
                0,
                256,
                62,
                62,
                false,
                false
        );
        World2 world = new World2(DefaultPlugin.JAVA_ANVIL, 0L, tileFactory);
        Dimension dimension = world.getDimension(Dimension.Anchor.NORMAL_DETAIL);
        Tile tile00 = new Tile(0, 0, 0, 256);
        dimension.addTile(tile00);

        BufferedImage sampleImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        sampleImage.setRGB(0, 0, 0xFF0000);
        File sampleFile = tempDir.resolve("persistent_dialog_test.png").toFile();
        ImageIO.write(sampleImage, "png", sampleFile);

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), dimension, sampleFile);
        dialog.ok();

        // Simulate closing the window while task is active
        dialog.cancel();

        // Verify the persistent progress dialog was created to monitor progress
        assertNotNull((dialog.getPersistentProgressDialog()));

        // The background apply task must persist until completion
        dialog.waitForApply();
        assertTrue((dialog.isApplied()));

        dialog.dispose();
    }

    /**
     * Verifies that preselecting a file without a parent path does not trigger a NullPointerException.
     */
    @Test
    public void testPreselectRelativeFileWithoutParent() {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        // Relative file with null getParentFile()
        File parentlessFile = new File("nonexistent_climate_test.png");
        assertNull(parentlessFile.getParentFile());

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault(), parentlessFile);
        assertNull(dialog.getSelectedFile());
        dialog.dispose();
    }

    /**
     * Verifies that dispose cleanly clears preview and dialog resources.
     */
    @Test
    public void testDisposeCleansUp() {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }

        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }

        KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadDefault());
        dialog.dispose();
        assertNull(dialog.getClimateImage());
        assertNull(dialog.getBiomePreviewImage());
    }
}
