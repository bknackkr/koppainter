package com.github.bknackkr.koppainter;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Decoder for Truevision TGA (TARGA) images supporting uncompressed and RLE-compressed 24-bit RGB and 32-bit RGBA formats.
 */
public final class TgaDecoder {
    private TgaDecoder() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Reads a TGA image from the specified file.
     *
     * @param file The TGA image file.
     * @return The decoded buffered image.
     */
    public static BufferedImage read(File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("File cannot be null");
        }
        if ((!file.exists())) {
            throw new MDCCapturingRuntimeException("TGA file does not exist: " + file.getAbsolutePath());
        }
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            return (read(inputStream));
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to read TGA file: " + file.getAbsolutePath(), exception);
        }
    }

    /**
     * Reads a TGA image from the provided input stream.
     *
     * @param in The input stream.
     * @return The decoded buffered image.
     */
    public static BufferedImage read(InputStream in) {
        if ((in == null)) {
            throw new MDCCapturingRuntimeException("InputStream cannot be null");
        }
        try {
            int idLength = in.read();
            int colorMapType = in.read();
            int imageType = in.read();
            if (((idLength == -1) || (colorMapType == -1) || (imageType == -1))) {
                throw new MDCCapturingRuntimeException("Unexpected end of file reading TGA header");
            }

            // Skip color map specification (5 bytes)
            skipFully(in, 5);
            // Skip x-origin (2 bytes) and y-origin (2 bytes)
            skipFully(in, 4);

            int width = readShort(in);
            int height = readShort(in);
            int bitsPerPixel = in.read();
            int imageDescriptor = in.read();

            if (((width <= 0) || (height <= 0))) {
                throw new MDCCapturingRuntimeException("Invalid TGA dimensions: " + width + "x" + height);
            }
            if (((bitsPerPixel != 24) && (bitsPerPixel != 32) && (bitsPerPixel != 8))) {
                throw new MDCCapturingRuntimeException("Unsupported TGA bit depth: " + bitsPerPixel
                        + " bpp. Only 8, 24, and 32 bpp are supported.");
            }
            if (((imageType != TYPE_TRUECOLOR) && (imageType != TYPE_RLE_TRUECOLOR)
                    && (imageType != TYPE_GRAYSCALE) && (imageType != TYPE_RLE_GRAYSCALE))) {
                throw new MDCCapturingRuntimeException("Unsupported TGA image type code: " + imageType);
            }

            // Skip image ID field if present
            if ((idLength > 0)) {
                skipFully(in, idLength);
            }

            boolean topToBottom = (((imageDescriptor & 0x20) != 0));
            int bytesPerPixel = (bitsPerPixel / 8);
            int imageTypeConstant = ((bytesPerPixel == 4) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            BufferedImage image = new BufferedImage(width, height, imageTypeConstant);
            int totalPixels = (width * height);
            int[] pixelBuffer = new int[totalPixels];

            if (((imageType == TYPE_TRUECOLOR) || (imageType == TYPE_GRAYSCALE))) {
                byte[] rawBytes = in.readNBytes((totalPixels * bytesPerPixel));
                if ((rawBytes.length < (totalPixels * bytesPerPixel))) {
                    throw new MDCCapturingRuntimeException("Premature end of file reading uncompressed TGA data");
                }
                int rawIndex = 0;
                for (int i = 0; (i < totalPixels); i++) {
                    pixelBuffer[i] = parsePixel(rawBytes, rawIndex, bytesPerPixel);
                    rawIndex += bytesPerPixel;
                }
            } else {
                int pixelsRead = 0;
                byte[] singlePixel = new byte[bytesPerPixel];
                while ((pixelsRead < totalPixels)) {
                    int header = in.read();
                    if ((header == -1)) {
                        throw new MDCCapturingRuntimeException("Premature end of stream reading RLE TGA packet header");
                    }
                    int count = ((header & 0x7F) + 1);
                    if (((header & 0x80) != 0)) {
                        readFully(in, singlePixel);
                        int pixelValue = parsePixel(singlePixel, 0, bytesPerPixel);
                        for (int i = 0; (i < count); i++) {
                            if ((pixelsRead < totalPixels)) {
                                pixelBuffer[pixelsRead++] = pixelValue;
                            }
                        }
                    } else {
                        for (int i = 0; (i < count); i++) {
                            readFully(in, singlePixel);
                            if ((pixelsRead < totalPixels)) {
                                pixelBuffer[pixelsRead++] = parsePixel(singlePixel, 0, bytesPerPixel);
                            }
                        }
                    }
                }
            }

            for (int y = 0; (y < height); y++) {
                int targetY = (topToBottom ? y : ((height - 1) - y));
                for (int x = 0; (x < width); x++) {
                    image.setRGB(x, targetY, pixelBuffer[((y * width) + x)]);
                }
            }

            return (image);
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Error decoding TGA image stream", exception);
        }
    }

    private static int parsePixel(byte[] data, int offset, int bytesPerPixel) {
        if ((bytesPerPixel == 4)) {
            int b = (data[offset] & 0xFF);
            int g = (data[(offset + 1)] & 0xFF);
            int r = (data[(offset + 2)] & 0xFF);
            int a = (data[(offset + 3)] & 0xFF);
            return (((a << 24) | (r << 16) | (g << 8) | b));
        } else if ((bytesPerPixel == 3)) {
            int b = (data[offset] & 0xFF);
            int g = (data[(offset + 1)] & 0xFF);
            int r = (data[(offset + 2)] & 0xFF);
            return (((0xFF << 24) | (r << 16) | (g << 8) | b));
        } else {
            int gray = (data[offset] & 0xFF);
            return (((0xFF << 24) | (gray << 16) | (gray << 8) | gray));
        }
    }

    private static int readShort(InputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        if (((b1 == -1) || (b2 == -1))) {
            throw new MDCCapturingRuntimeException("Unexpected end of stream reading short");
        }
        return ((b1 | (b2 << 8)));
    }

    private static void skipFully(InputStream in, long count) throws IOException {
        long remaining = count;
        while ((remaining > 0)) {
            long skipped = in.skip(remaining);
            if ((skipped <= 0)) {
                if ((in.read() == -1)) {
                    throw new MDCCapturingRuntimeException("Unexpected end of stream while skipping bytes");
                }
                remaining--;
            } else {
                remaining -= skipped;
            }
        }
    }

    private static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while ((offset < buffer.length)) {
            int read = in.read(buffer, offset, (buffer.length - offset));
            if ((read == -1)) {
                throw new MDCCapturingRuntimeException("Premature end of stream while reading pixel bytes");
            }
            offset += read;
        }
    }

    private static final int TYPE_TRUECOLOR = 2;
    private static final int TYPE_GRAYSCALE = 3;
    private static final int TYPE_RLE_TRUECOLOR = 10;
    private static final int TYPE_RLE_GRAYSCALE = 11;
}
