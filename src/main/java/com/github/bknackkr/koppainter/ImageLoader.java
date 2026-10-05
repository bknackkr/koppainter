package com.github.bknackkr.koppainter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Service for loading uncompressed and lossless climate map image formats (PNG, BMP, TIFF, and TGA).
 */
public final class ImageLoader {
    private ImageLoader() {
        // Utility class; prevent direct instantiation
    }

    /**
     * Loads an image from the specified {@link File}.
     *
     * <p>Supports PNG, BMP, TIFF, and TGA image formats.</p>
     *
     * @param file The image file to load.
     * @return The loaded {@link BufferedImage}.
     */
    public static BufferedImage load(File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("Image file cannot be null");
        }
        if ((!file.exists())) {
            throw new MDCCapturingRuntimeException("Image file does not exist: " + file.getAbsolutePath());
        }
        if ((!file.canRead())) {
            throw new MDCCapturingRuntimeException("Cannot read image file: " + file.getAbsolutePath());
        }

        String extension = getExtension(file);
        if (("tga".equalsIgnoreCase(extension))) {
            return (TgaDecoder.read(file));
        }

        try {
            BufferedImage image = ImageIO.read(file);
            if ((image == null)) {
                // If ImageIO failed, attempt TGA fallback in case extension was missing or varied
                if (("tga".equalsIgnoreCase(extension)) || ("tpic".equalsIgnoreCase(extension))) {
                    return (TgaDecoder.read(file));
                }
                throw new MDCCapturingRuntimeException("Unsupported or corrupted image format: " + file.getName());
            }
            return (image);
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("I/O error loading image from " + file.getAbsolutePath(), exception);
        }
    }

    /**
     * Loads an image from the specified {@link Path}.
     *
     * @param path The path of the image file.
     * @return The loaded {@link BufferedImage}.
     */
    public static BufferedImage load(Path path) {
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("Path cannot be null");
        }
        return (load(path.toFile()));
    }

    /**
     * Saves the specified {@link BufferedImage} to disk as a PNG file.
     *
     * @param image The image to save.
     * @param file The destination file.
     */
    public static void saveAsPng(BufferedImage image, File file) {
        if ((image == null)) {
            throw new MDCCapturingRuntimeException("Image cannot be null");
        }
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("Destination file cannot be null");
        }
        try {
            if ((file.getParentFile() != null) && (!file.getParentFile().exists())) {
                file.getParentFile().mkdirs();
            }
            boolean success = ImageIO.write(image, "png", file);
            if ((!success)) {
                throw new MDCCapturingRuntimeException("No appropriate PNG image writer found");
            }
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to save image as PNG to " + file.getAbsolutePath(), exception);
        }
    }

    /**
     * Saves the specified {@link BufferedImage} to disk as a PNG file.
     *
     * @param image The image to save.
     * @param path The destination path.
     */
    public static void saveAsPng(BufferedImage image, Path path) {
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("Destination path cannot be null");
        }
        saveAsPng(image, path.toFile());
    }

    /**
     * Checks whether the specified file has a supported lossless/uncompressed image extension.
     *
     * @param file The file to check.
     * @return {@code true} if supported, {@code false} otherwise.
     */
    public static boolean isSupported(File file) {
        if ((file == null)) {
            return (false);
        }
        String extension = getExtension(file);
        return ((extension != null) && EXTENSIONS_SET.contains(extension.toLowerCase(Locale.ROOT)));
    }

    /**
     * Extracts the lowercase file extension from the specified file.
     *
     * @param file The file.
     * @return The extension without the leading dot, or empty string if none.
     */
    public static String getExtension(File file) {
        if ((file == null)) {
            return ("");
        }
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (((dotIndex == -1) || (dotIndex == (name.length() - 1)))) {
            return ("");
        }
        return (name.substring((dotIndex + 1)).toLowerCase(Locale.ROOT));
    }

    /**
     * Returns an array of supported file extension strings.
     *
     * @return An array of extensions (e.g. {@code ["png", "bmp", "tif", "tiff", "tga"]}).
     */
    public static String[] getSupportedExtensions() {
        return (SUPPORTED_EXTENSIONS.clone());
    }

    /**
     * Supported lossless image format extensions.
     */
    public static final String[] SUPPORTED_EXTENSIONS = {"png", "bmp", "tif", "tiff", "tga"};

    private static final Set<String> EXTENSIONS_SET = Set.of("png", "bmp", "tif", "tiff", "tga");
}
