package com.github.bknackkr.koppainter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TgaDecoder}.
 */
public class TgaDecoderTest {
    /**
     * Default constructor for test suite.
     */
    public TgaDecoderTest() {
    }

    /**
     * Verifies decoding of an uncompressed 24-bit RGB TGA image (2x2 pixels).
     */
    @Test
    public void testDecodeUncompressed24BitTga() {
        // Create 2x2 24-bit TGA:
        // Top-left: Red (255, 0, 0), Top-right: Green (0, 255, 0)
        // Bottom-left: Blue (0, 0, 255), Bottom-right: Yellow (255, 255, 0)
        // Standard TGA is bottom-to-top, so row 0 in file is bottom row (Blue, Yellow), row 1 is top row (Red, Green)
        byte[] tgaBytes = createTgaByteArray(2, 2, 24, false, false, new int[] {
                // Bottom row: Blue, Yellow
                0x0000FF, 0xFFFF00,
                // Top row: Red, Green
                0xFF0000, 0x00FF00
        });

        BufferedImage image = TgaDecoder.read(new ByteArrayInputStream(tgaBytes));
        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(2, image.getHeight());

        // Verify top-left is Red
        assertEquals((0xFF0000), (image.getRGB(0, 0) & 0xFFFFFF));
        // Verify top-right is Green
        assertEquals((0x00FF00), (image.getRGB(1, 0) & 0xFFFFFF));
        // Verify bottom-left is Blue
        assertEquals((0x0000FF), (image.getRGB(0, 1) & 0xFFFFFF));
        // Verify bottom-right is Yellow
        assertEquals((0xFFFF00), (image.getRGB(1, 1) & 0xFFFFFF));
    }

    /**
     * Verifies decoding of a top-to-bottom oriented 32-bit RGBA TGA image.
     */
    @Test
    public void testDecodeTopToBottom32BitTga() {
        // 2x1 32-bit TGA with top-to-bottom flag (descriptor bit 5 = 1)
        byte[] tgaBytes = createTgaByteArray(2, 1, 32, true, false, new int[] {
                0xFF0000, 0x00FF00
        });

        BufferedImage image = TgaDecoder.read(new ByteArrayInputStream(tgaBytes));
        assertNotNull(image);
        assertEquals(2, image.getWidth());
        assertEquals(1, image.getHeight());
        assertEquals((0xFF0000), (image.getRGB(0, 0) & 0xFFFFFF));
        assertEquals((0x00FF00), (image.getRGB(1, 0) & 0xFFFFFF));
    }

    /**
     * Verifies decoding of an RLE-compressed 24-bit TGA image.
     */
    @Test
    public void testDecodeRle24BitTga() {
        // 4x1 image with 3 reds followed by 1 blue
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeTgaHeader(out, 4, 1, 24, 10, true); // type 10 is RLE true-color

        // RLE packet: 3 red pixels (packet header = 0x80 | (3 - 1) = 0x82)
        out.write(0x82);
        out.write(0x00); // B
        out.write(0x00); // G
        out.write(0xFF); // R

        // Raw packet: 1 blue pixel (packet header = 0x00 | (1 - 1) = 0x00)
        out.write(0x00);
        out.write(0xFF); // B
        out.write(0x00); // G
        out.write(0x00); // R

        byte[] tgaBytes = out.toByteArray();
        BufferedImage image = TgaDecoder.read(new ByteArrayInputStream(tgaBytes));

        assertNotNull(image);
        assertEquals(4, image.getWidth());
        assertEquals(1, image.getHeight());
        assertEquals((0xFF0000), (image.getRGB(0, 0) & 0xFFFFFF));
        assertEquals((0xFF0000), (image.getRGB(1, 0) & 0xFFFFFF));
        assertEquals((0xFF0000), (image.getRGB(2, 0) & 0xFFFFFF));
        assertEquals((0x0000FF), (image.getRGB(3, 0) & 0xFFFFFF));
    }

    /**
     * Verifies reading from a temporary file on disk.
     *
     * @param tempDir JUnit temporary directory.
     * @throws IOException If file creation fails.
     */
    @Test
    public void testReadFromFile(@TempDir Path tempDir) throws IOException {
        byte[] tgaBytes = createTgaByteArray(1, 1, 24, true, false, new int[] { 0x00FF00 });
        File tempFile = tempDir.resolve("sample.tga").toFile();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(tgaBytes);
        }

        BufferedImage image = TgaDecoder.read(tempFile);
        assertNotNull(image);
        assertEquals(1, image.getWidth());
        assertEquals((0x00FF00), (image.getRGB(0, 0) & 0xFFFFFF));
    }

    /**
     * Verifies error handling for malformed or missing input.
     */
    @Test
    public void testErrorHandling() {
        assertThrows(MDCCapturingRuntimeException.class, () -> TgaDecoder.read((File) null));
        assertThrows(MDCCapturingRuntimeException.class, () -> TgaDecoder.read((ByteArrayInputStream) null));
        assertThrows(MDCCapturingRuntimeException.class, () -> TgaDecoder.read(new ByteArrayInputStream(new byte[5])));
    }

    private static byte[] createTgaByteArray(int width, int height, int bpp, boolean topToBottom,
                                             boolean rle, int[] pixels) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int imageType = (rle ? 10 : 2);
        writeTgaHeader(out, width, height, bpp, imageType, topToBottom);

        int bytesPerPixel = (bpp / 8);
        for (int pixel : pixels) {
            int b = (pixel & 0xFF);
            int g = ((pixel >> 8) & 0xFF);
            int r = ((pixel >> 16) & 0xFF);
            out.write(b);
            out.write(g);
            out.write(r);
            if ((bytesPerPixel == 4)) {
                out.write(0xFF); // Alpha
            }
        }
        return (out.toByteArray());
    }

    private static void writeTgaHeader(ByteArrayOutputStream out, int width, int height, int bpp,
                                       int imageType, boolean topToBottom) {
        out.write(0); // ID length
        out.write(0); // Color map type
        out.write(imageType); // Image type code (2 = truecolor, 10 = RLE)
        // 5 bytes color map spec
        for (int i = 0; (i < 5); i++) {
            out.write(0);
        }
        // X origin (2 bytes) and Y origin (2 bytes)
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        // Width (little-endian 2 bytes)
        out.write((width & 0xFF));
        out.write(((width >> 8) & 0xFF));
        // Height (little-endian 2 bytes)
        out.write((height & 0xFF));
        out.write(((height >> 8) & 0xFF));
        // Bits per pixel
        out.write(bpp);
        // Image descriptor: bit 5 = 1 for top-to-bottom, 0 for bottom-to-top
        out.write((topToBottom ? 0x20 : 0x00));
    }
}
