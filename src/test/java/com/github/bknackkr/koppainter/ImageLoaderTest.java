package com.github.bknackkr.koppainter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImageLoader}.
 */
public class ImageLoaderTest {
    /**
     * Default constructor for test suite.
     */
    public ImageLoaderTest() {
    }

    /**
     * Verifies that PNG images are correctly loaded.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image writing fails.
     */
    @Test
    public void testLoadPng(@TempDir Path tempDir) throws IOException {
        BufferedImage original = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        original.setRGB(0, 0, 0xFF0000);
        File pngFile = tempDir.resolve("test.png").toFile();
        ImageIO.write(original, "png", pngFile);

        BufferedImage loaded = ImageLoader.load(pngFile);
        assertNotNull(loaded);
        assertEquals(4, loaded.getWidth());
        assertEquals(4, loaded.getHeight());
        assertEquals((0xFF0000), (loaded.getRGB(0, 0) & 0xFFFFFF));
    }

    /**
     * Verifies that BMP images are correctly loaded.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image writing fails.
     */
    @Test
    public void testLoadBmp(@TempDir Path tempDir) throws IOException {
        BufferedImage original = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        original.setRGB(1, 1, 0x00FF00);
        File bmpFile = tempDir.resolve("test.bmp").toFile();
        ImageIO.write(original, "bmp", bmpFile);

        BufferedImage loaded = ImageLoader.load(bmpFile);
        assertNotNull(loaded);
        assertEquals(3, loaded.getWidth());
        assertEquals(3, loaded.getHeight());
        assertEquals((0x00FF00), (loaded.getRGB(1, 1) & 0xFFFFFF));
    }

    /**
     * Verifies that TGA images are correctly loaded.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image writing fails.
     */
    @Test
    public void testLoadTga(@TempDir Path tempDir) throws IOException {
        byte[] tgaBytes = createSimpleTgaBytes(2, 2, 0x0000FF);
        File tgaFile = tempDir.resolve("test.tga").toFile();
        try (FileOutputStream fos = new FileOutputStream(tgaFile)) {
            fos.write(tgaBytes);
        }

        BufferedImage loaded = ImageLoader.load(tgaFile);
        assertNotNull(loaded);
        assertEquals(2, loaded.getWidth());
        assertEquals(2, loaded.getHeight());
        assertEquals((0x0000FF), (loaded.getRGB(0, 0) & 0xFFFFFF));
    }

    /**
     * Verifies extension detection and supported formats checking.
     */
    @Test
    public void testSupportedFormats() {
        assertTrue(ImageLoader.isSupported(new File("climate.png")));
        assertTrue(ImageLoader.isSupported(new File("climate.PNG")));
        assertTrue(ImageLoader.isSupported(new File("climate.bmp")));
        assertTrue(ImageLoader.isSupported(new File("climate.tif")));
        assertTrue(ImageLoader.isSupported(new File("climate.tiff")));
        assertTrue(ImageLoader.isSupported(new File("climate.tga")));

        assertFalse(ImageLoader.isSupported(new File("climate.jpg")));
        assertFalse(ImageLoader.isSupported(new File("climate.jpeg")));
        assertFalse(ImageLoader.isSupported(new File("climate.txt")));
        assertFalse(ImageLoader.isSupported((File) null));

        assertEquals("png", ImageLoader.getExtension(new File("image.png")));
        assertEquals("tga", ImageLoader.getExtension(new File("image.TGA")));
        assertEquals("", ImageLoader.getExtension(new File("image_no_ext")));
    }

    /**
     * Verifies error handling when loading invalid or missing files.
     */
    @Test
    public void testErrorHandling() {
        assertThrows(MDCCapturingRuntimeException.class, () -> ImageLoader.load((File) null));
        assertThrows(MDCCapturingRuntimeException.class, () -> ImageLoader.load(new File("nonexistent_file.png")));
        assertThrows(MDCCapturingRuntimeException.class, () -> ImageLoader.saveAsPng(null, new File("test.png")));
        assertThrows(MDCCapturingRuntimeException.class, () -> ImageLoader.saveAsPng(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), (File) null));
    }

    /**
     * Verifies saving a BufferedImage as a PNG file and reading it back.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If image file operations fail.
     */
    @Test
    public void testSaveAsPng(@TempDir Path tempDir) throws IOException {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        image.setRGB(2, 2, 0x123456);
        File exportFile = tempDir.resolve("exported.png").toFile();

        ImageLoader.saveAsPng(image, exportFile);
        assertTrue(exportFile.exists());
        assertTrue((exportFile.length() > 0));

        BufferedImage reloaded = ImageLoader.load(exportFile);
        assertNotNull(reloaded);
        assertEquals(5, reloaded.getWidth());
        assertEquals(5, reloaded.getHeight());
        assertEquals(0x123456, (reloaded.getRGB(2, 2) & 0xFFFFFF));
    }

    private static byte[] createSimpleTgaBytes(int width, int height, int rgb) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0); // ID length
        out.write(0); // Color map type
        out.write(2); // Uncompressed true-color
        for (int i = 0; (i < 9); i++) {
            out.write(0);
        }
        out.write((width & 0xFF));
        out.write(((width >> 8) & 0xFF));
        out.write((height & 0xFF));
        out.write(((height >> 8) & 0xFF));
        out.write(24); // 24 bpp
        out.write(0x20); // Top-to-bottom

        int b = (rgb & 0xFF);
        int g = ((rgb >> 8) & 0xFF);
        int r = ((rgb >> 16) & 0xFF);

        for (int i = 0; (i < (width * height)); i++) {
            out.write(b);
            out.write(g);
            out.write(r);
        }
        return (out.toByteArray());
    }
}
