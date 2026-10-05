package com.github.bknackkr.koppainter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Manages the mapping between image RGB colors and Minecraft biomes.
 *
 * <p>Supports loading user-definable configuration files in properties format (e.g. {@code FF0000 = desert}),
 * saving configurations, querying biomes by exact color or nearest Euclidean RGB color match, and exporting defaults.</p>
 */
public final class ColorBiomeMap {
    /**
     * Constructs a new, empty {@code ColorBiomeMap}.
     */
    public ColorBiomeMap() {
        mappings = new LinkedHashMap<>();
        nearestNeighborDistances = new LinkedHashMap<>();
        colorTolerance = DEFAULT_COLOR_TOLERANCE;
        neighborDistancesDirty = true;
    }

    /**
     * Constructs a new {@code ColorBiomeMap} containing a copy of the specified mappings.
     *
     * @param initialMappings The initial mappings to copy into this map.
     */
    public ColorBiomeMap(Map<Integer, BiomeEntry> initialMappings) {
        if ((initialMappings == null)) {
            throw new MDCCapturingRuntimeException("Initial mappings cannot be null");
        }
        mappings = new LinkedHashMap<>(initialMappings);
        nearestNeighborDistances = new LinkedHashMap<>();
        colorTolerance = DEFAULT_COLOR_TOLERANCE;
        neighborDistancesDirty = true;
    }

    /**
     * Returns the biome associated with the given 24-bit RGB color.
     *
     * @param rgb The RGB color value (the alpha component is ignored).
     * @return The associated biome entry, or {@code null} if no exact match is defined.
     */
    public BiomeEntry getBiome(int rgb) {
        return (mappings.get((rgb & RGB_MASK)));
    }

    /**
     * Returns the biome associated with the given {@link Color}.
     *
     * @param color The color to look up.
     * @return The associated biome entry, or {@code null} if no exact match is defined.
     */
    public BiomeEntry getBiome(Color color) {
        if ((color == null)) {
            throw new MDCCapturingRuntimeException("Color cannot be null");
        }
        return (getBiome(color.getRGB()));
    }

    /**
     * Returns the biome associated with the given hex color string.
     *
     * @param hexColor The hex color string (e.g. {@code "FF0000"} or {@code "#00FF00"}).
     * @return The associated biome entry, or {@code null} if no exact match is defined.
     */
    public BiomeEntry getBiome(String hexColor) {
        return (getBiome(parseHexColor(hexColor)));
    }

    /**
     * Finds the biome whose defined color is closest to the given RGB color using Euclidean distance.
     *
     * <p>If an exact match exists, it is returned immediately. This method is especially helpful
     * for handling image anti-aliasing, compression artifacts, or subtle shading.</p>
     *
     * @param rgb The target RGB color.
     * @return The nearest biome entry.
     */
    public BiomeEntry findNearestBiome(int rgb) {
        if ((mappings.isEmpty())) {
            throw new MDCCapturingRuntimeException("Cannot find nearest biome: color definition map is empty");
        }
        int targetRgb = (rgb & RGB_MASK);
        BiomeEntry exact = mappings.get(targetRgb);
        if ((exact != null)) {
            return (exact);
        }

        int targetRed = ((targetRgb >> 16) & 0xFF);
        int targetGreen = ((targetRgb >> 8) & 0xFF);
        int targetBlue = (targetRgb & 0xFF);

        BiomeEntry closestBiome = null;
        long smallestDistanceSquared = Long.MAX_VALUE;

        for (Map.Entry<Integer, BiomeEntry> entry : mappings.entrySet()) {
            int entryColor = entry.getKey();
            int redDiff = (((entryColor >> 16) & 0xFF) - targetRed);
            int greenDiff = (((entryColor >> 8) & 0xFF) - targetGreen);
            int blueDiff = ((entryColor & 0xFF) - targetBlue);
            long distanceSquared = (((long) redDiff * redDiff)
                    + ((long) greenDiff * greenDiff)
                    + ((long) blueDiff * blueDiff));

            if ((distanceSquared < smallestDistanceSquared)) {
                smallestDistanceSquared = distanceSquared;
                closestBiome = entry.getValue();
            }
        }
        return (closestBiome);
    }

    /**
     * Finds the biome whose defined color is closest to the given {@link Color}.
     *
     * @param color The target color.
     * @return The nearest biome entry.
     */
    public BiomeEntry findNearestBiome(Color color) {
        if ((color == null)) {
            throw new MDCCapturingRuntimeException("Color cannot be null");
        }
        return (findNearestBiome(color.getRGB()));
    }

    /**
     * Finds the biome whose defined color is closest to the given RGB color within the specified margin of error.
     *
     * <p>If an exact match exists, it is returned immediately. Otherwise, the nearest biome is returned only if
     * the color distance does not exceed {@code maxTolerance} and satisfies neighbor separation safety limits.
     * If the color is outside the margin of error, {@code null} is returned.</p>
     *
     * @param rgb The target RGB color.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The nearest biome entry within tolerance, or {@code null} if none matches.
     */
    public BiomeEntry findNearestBiome(int rgb, double maxTolerance) {
        Integer matchedColor = findNearestColor(rgb, maxTolerance);
        if ((matchedColor != null)) {
            return (mappings.get(matchedColor));
        }
        return (null);
    }

    /**
     * Finds the biome whose defined color is closest to the given {@link Color} within the specified margin of error.
     *
     * @param color The target color.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The nearest biome entry within tolerance, or {@code null} if none matches.
     */
    public BiomeEntry findNearestBiome(Color color, double maxTolerance) {
        if ((color == null)) {
            throw new MDCCapturingRuntimeException("Color cannot be null");
        }
        return (findNearestBiome(color.getRGB(), maxTolerance));
    }

    /**
     * Finds the 24-bit RGB indexed color closest to the specified target color within the given margin of error.
     *
     * <p>If an exact match exists, it is returned immediately (distance 0). Otherwise, the nearest indexed color
     * is returned if its Euclidean RGB distance does not exceed {@code maxTolerance}, and is strictly closer
     * than any competing indexed color. If the target color is outside this margin of error or is tied between
     * two colors, {@code null} is returned.</p>
     *
     * @param rgb The target 24-bit RGB color.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The 24-bit RGB integer of the matching indexed color, or {@code null} if no indexed color matches
     *         within the margin of error.
     */
    public Integer findNearestColor(int rgb, double maxTolerance) {
        if ((mappings.isEmpty())) {
            return (null);
        }
        int targetRgb = (rgb & RGB_MASK);
        if ((mappings.containsKey(targetRgb))) {
            return (targetRgb);
        }
        if ((maxTolerance <= 0.0)) {
            return (null);
        }

        int targetRed = ((targetRgb >> 16) & 0xFF);
        int targetGreen = ((targetRgb >> 8) & 0xFF);
        int targetBlue = (targetRgb & 0xFF);

        Integer closestColor = null;
        long smallestDistanceSquared = Long.MAX_VALUE;
        long secondSmallestDistanceSquared = Long.MAX_VALUE;

        for (Integer entryColor : mappings.keySet()) {
            int redDiff = (((entryColor >> 16) & 0xFF) - targetRed);
            int greenDiff = (((entryColor >> 8) & 0xFF) - targetGreen);
            int blueDiff = ((entryColor & 0xFF) - targetBlue);
            long distanceSquared = (((long) redDiff * redDiff)
                    + ((long) greenDiff * greenDiff)
                    + ((long) blueDiff * blueDiff));

            if ((distanceSquared < smallestDistanceSquared)) {
                secondSmallestDistanceSquared = smallestDistanceSquared;
                smallestDistanceSquared = distanceSquared;
                closestColor = entryColor;
            } else if ((distanceSquared < secondSmallestDistanceSquared)) {
                secondSmallestDistanceSquared = distanceSquared;
            }
        }

        if ((closestColor == null)) {
            return (null);
        }

        double distance = Math.sqrt((double) smallestDistanceSquared);

        // Safe conversion condition: within user tolerance and strictly closer than the second closest color
        if (((distance <= maxTolerance)
                && (smallestDistanceSquared < secondSmallestDistanceSquared))) {
            return (closestColor);
        }

        return (null);
    }

    /**
     * Finds the 24-bit RGB indexed color closest to the specified {@link Color} within the given margin of error.
     *
     * @param color The target color.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The 24-bit RGB integer of the matching indexed color, or {@code null} if none matches safely.
     */
    public Integer findNearestColor(Color color, double maxTolerance) {
        if ((color == null)) {
            throw new MDCCapturingRuntimeException("Color cannot be null");
        }
        return (findNearestColor(color.getRGB(), maxTolerance));
    }

    /**
     * Finds the nearest 24-bit RGB indexed color among the 4 orthogonal adjacent pixels (up, down, left, right)
     * of the specified pixel coordinates within the given margin of error.
     *
     * <p>Only adjacent pixels that have defined indexed colors in this map are evaluated as candidate matches.
     * If multiple adjacent indexed colors are present, the one closest in Euclidean RGB distance to the target
     * pixel's color is selected, provided it is within {@code maxTolerance} and not ambiguous/tied. If no adjacent
     * pixel has an indexed color or none is within tolerance, {@code null} is returned.</p>
     *
     * @param image The image containing the pixel and its neighbors.
     * @param x The x coordinate of the target pixel.
     * @param y The y coordinate of the target pixel.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The 24-bit RGB integer of the matching adjacent indexed color, or {@code null} if none matches.
     */
    public Integer findNearestAdjacentColor(BufferedImage image, int x, int y, double maxTolerance) {
        if ((image == null)) {
            throw new MDCCapturingRuntimeException("Image cannot be null");
        }
        if ((mappings.isEmpty()) || (maxTolerance <= 0.0)) {
            return (null);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (((x < 0) || (x >= width) || (y < 0) || (y >= height))) {
            throw new MDCCapturingRuntimeException(String.format("Coordinates (%d, %d) out of bounds (%dx%d)",
                    x, y, width, height));
        }

        int targetRgb = (image.getRGB(x, y) & RGB_MASK);
        if ((mappings.containsKey(targetRgb))) {
            return (targetRgb);
        }

        int[] candidateColors = new int[4];
        int candidateCount = 0;

        if ((y > 0)) {
            int upRgb = (image.getRGB(x, (y - 1)) & RGB_MASK);
            if ((mappings.containsKey(upRgb))) {
                candidateColors[candidateCount++] = upRgb;
            }
        }
        if (((y + 1) < height)) {
            int downRgb = (image.getRGB(x, (y + 1)) & RGB_MASK);
            if ((mappings.containsKey(downRgb))) {
                boolean exists = false;
                for (int i = 0; (i < candidateCount); i++) {
                    if ((candidateColors[i] == downRgb)) {
                        exists = true;
                        break;
                    }
                }
                if ((!exists)) {
                    candidateColors[candidateCount++] = downRgb;
                }
            }
        }
        if ((x > 0)) {
            int leftRgb = (image.getRGB((x - 1), y) & RGB_MASK);
            if ((mappings.containsKey(leftRgb))) {
                boolean exists = false;
                for (int i = 0; (i < candidateCount); i++) {
                    if ((candidateColors[i] == leftRgb)) {
                        exists = true;
                        break;
                    }
                }
                if ((!exists)) {
                    candidateColors[candidateCount++] = leftRgb;
                }
            }
        }
        if (((x + 1) < width)) {
            int rightRgb = (image.getRGB((x + 1), y) & RGB_MASK);
            if ((mappings.containsKey(rightRgb))) {
                boolean exists = false;
                for (int i = 0; (i < candidateCount); i++) {
                    if ((candidateColors[i] == rightRgb)) {
                        exists = true;
                        break;
                    }
                }
                if ((!exists)) {
                    candidateColors[candidateCount++] = rightRgb;
                }
            }
        }

        if ((candidateCount == 0)) {
            return (null);
        }

        int targetRed = ((targetRgb >> 16) & 0xFF);
        int targetGreen = ((targetRgb >> 8) & 0xFF);
        int targetBlue = (targetRgb & 0xFF);

        Integer closestColor = null;
        long smallestDistanceSquared = Long.MAX_VALUE;
        long secondSmallestDistanceSquared = Long.MAX_VALUE;

        for (int i = 0; (i < candidateCount); i++) {
            int candidateColor = candidateColors[i];
            int redDiff = (((candidateColor >> 16) & 0xFF) - targetRed);
            int greenDiff = (((candidateColor >> 8) & 0xFF) - targetGreen);
            int blueDiff = ((candidateColor & 0xFF) - targetBlue);
            long distanceSquared = (((long) redDiff * redDiff)
                    + ((long) greenDiff * greenDiff)
                    + ((long) blueDiff * blueDiff));

            if ((distanceSquared < smallestDistanceSquared)) {
                secondSmallestDistanceSquared = smallestDistanceSquared;
                smallestDistanceSquared = distanceSquared;
                closestColor = candidateColor;
            } else if ((distanceSquared < secondSmallestDistanceSquared)) {
                secondSmallestDistanceSquared = distanceSquared;
            }
        }

        if ((closestColor == null)) {
            return (null);
        }

        double distance = Math.sqrt((double) smallestDistanceSquared);

        if (((distance <= maxTolerance)
                && ((candidateCount == 1) || (smallestDistanceSquared < secondSmallestDistanceSquared)))) {
            return (closestColor);
        }

        return (null);
    }

    /**
     * Finds the nearest {@link BiomeEntry} among the 4 orthogonal adjacent pixels (up, down, left, right)
     * of the specified pixel coordinates within the given margin of error.
     *
     * @param image The image containing the pixel and its neighbors.
     * @param x The x coordinate of the target pixel.
     * @param y The y coordinate of the target pixel.
     * @param maxTolerance The maximum allowable Euclidean distance margin of error.
     * @return The matching adjacent biome entry, or {@code null} if none matches safely.
     */
    public BiomeEntry findNearestAdjacentBiome(BufferedImage image, int x, int y, double maxTolerance) {
        Integer matchedColor = findNearestAdjacentColor(image, x, y, maxTolerance);
        if ((matchedColor != null)) {
            return (mappings.get(matchedColor));
        }
        return (null);
    }

    /**
     * Returns the Euclidean RGB distance between the specified indexed color and its closest indexed neighbor.
     *
     * @param rgb The 24-bit RGB color.
     * @return The distance to the nearest neighbor color, or {@link Double#POSITIVE_INFINITY} if only one or no colors
     *         are defined, or {@code -1.0} if the specified color is not in this map.
     */
    public double getNearestNeighborDistance(int rgb) {
        int targetRgb = (rgb & RGB_MASK);
        if ((!mappings.containsKey(targetRgb))) {
            return (-1.0);
        }
        ensureNeighborDistances();
        Double dist = nearestNeighborDistances.get(targetRgb);
        return (((dist != null) ? dist : Double.POSITIVE_INFINITY));
    }

    /**
     * Calculates the effective tolerance (margin of error) for matching slightly-off colors against the specified
     * indexed color.
     *
     * @param rgb The 24-bit RGB indexed color.
     * @param maxTolerance The global maximum tolerance.
     * @return The effective tolerance, clamped to non-negative values.
     */
    public double getEffectiveTolerance(int rgb, double maxTolerance) {
        if ((maxTolerance <= 0.0)) {
            return (0.0);
        }
        return (maxTolerance);
    }

    /**
     * Converts slightly-off colors in the given image to the nearest indexed color within the specified tolerance.
     *
     * <p>If {@code adjacentOnly} is {@code true}, only indexed colors physically adjacent (4-connected) to each
     * non-indexed pixel are considered as candidates, preventing distant palette colors from matching.</p>
     *
     * @param image The input image to process.
     * @param maxTolerance The margin of error tolerance.
     * @param adjacentOnly If {@code true}, restricts matching to adjacent indexed colors.
     * @return A new {@link BufferedImage} containing the converted colors.
     */
    public BufferedImage convertToIndexedColors(BufferedImage image, double maxTolerance, boolean adjacentOnly) {
        if ((image == null)) {
            throw new MDCCapturingRuntimeException("Image cannot be null");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Map<Integer, Integer> colorCache = new LinkedHashMap<>();

        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                int originalRgb = (image.getRGB(x, y) & RGB_MASK);
                if ((mappings.containsKey(originalRgb))) {
                    result.setRGB(x, y, originalRgb);
                } else if (adjacentOnly) {
                    Integer matched = findNearestAdjacentColor(image, x, y, maxTolerance);
                    result.setRGB(x, y, (((matched != null) ? matched : originalRgb)));
                } else {
                    int convertedRgb = colorCache.computeIfAbsent(originalRgb, c -> {
                        Integer matched = findNearestColor(c, maxTolerance);
                        return (((matched != null) ? matched : c));
                    });
                    result.setRGB(x, y, convertedRgb);
                }
            }
        }
        return (result);
    }

    /**
     * Converts slightly-off colors in the given image to the nearest indexed color within the specified tolerance.
     *
     * <p>Colors that are way off or ambiguous between similar indexed colors remain unchanged.</p>
     *
     * @param image The input image to process.
     * @param maxTolerance The margin of error tolerance.
     * @return A new {@link BufferedImage} containing the converted colors.
     */
    public BufferedImage convertToIndexedColors(BufferedImage image, double maxTolerance) {
        return (convertToIndexedColors(image, maxTolerance, false));
    }

    /**
     * Generates a Minecraft biome map image from the specified climate map image using representative biome colors.
     *
     * @param climateImage The source climate map image.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     * @param adjacentOnly If {@code true}, restricts matching to adjacent indexed colors.
     * @return A new {@link BufferedImage} representing the biome map.
     */
    public BufferedImage generateBiomeMapImage(BufferedImage climateImage, BiomeEntry defaultBiome,
                                                double maxTolerance, boolean adjacentOnly) {
        if ((climateImage == null)) {
            throw new MDCCapturingRuntimeException("Climate image cannot be null");
        }
        int width = climateImage.getWidth();
        int height = climateImage.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Map<Integer, Integer> colorCache = new LinkedHashMap<>();

        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                int rgb = (climateImage.getRGB(x, y) & RGB_MASK);
                int biomeColor;
                if ((mappings.containsKey(rgb))) {
                    biomeColor = KoppainterDialog.getBiomeColor(getBiome(rgb));
                } else if (adjacentOnly) {
                    BiomeEntry biome = null;
                    if ((maxTolerance > 0.0)) {
                        biome = findNearestAdjacentBiome(climateImage, x, y, maxTolerance);
                    }
                    if ((biome == null)) {
                        biome = defaultBiome;
                    }
                    biomeColor = KoppainterDialog.getBiomeColor(biome);
                } else {
                    biomeColor = colorCache.computeIfAbsent(rgb, c -> {
                        BiomeEntry biome = getBiome(c);
                        if ((biome == null) && (maxTolerance > 0.0)) {
                            biome = findNearestBiome(c, maxTolerance);
                        }
                        if ((biome == null)) {
                            biome = defaultBiome;
                        }
                        return (KoppainterDialog.getBiomeColor(biome));
                    });
                }
                result.setRGB(x, y, biomeColor);
            }
        }
        return (result);
    }

    /**
     * Generates a Minecraft biome map image from the specified climate map image using representative biome colors.
     *
     * @param climateImage The source climate map image.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     * @return A new {@link BufferedImage} representing the biome map.
     */
    public BufferedImage generateBiomeMapImage(BufferedImage climateImage, BiomeEntry defaultBiome,
                                                double maxTolerance) {
        return (generateBiomeMapImage(climateImage, defaultBiome, maxTolerance, false));
    }

    /**
     * Exports a generated biome map from the specified climate map image directly to disk as a PNG file.
     *
     * @param climateImage The source climate map image.
     * @param destination The destination PNG file path.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     * @param adjacentOnly If {@code true}, restricts matching to adjacent indexed colors.
     */
    public void exportBiomeMapAsPng(BufferedImage climateImage, Path destination, BiomeEntry defaultBiome,
                                    double maxTolerance, boolean adjacentOnly) {
        if ((destination == null)) {
            throw new MDCCapturingRuntimeException("Destination path cannot be null");
        }
        BufferedImage biomeMap = generateBiomeMapImage(climateImage, defaultBiome, maxTolerance, adjacentOnly);
        ImageLoader.saveAsPng(biomeMap, destination);
    }

    /**
     * Exports a generated biome map from the specified climate map image directly to disk as a PNG file.
     *
     * @param climateImage The source climate map image.
     * @param destination The destination PNG file path.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     */
    public void exportBiomeMapAsPng(BufferedImage climateImage, Path destination, BiomeEntry defaultBiome,
                                    double maxTolerance) {
        exportBiomeMapAsPng(climateImage, destination, defaultBiome, maxTolerance, false);
    }

    /**
     * Exports a generated biome map from the specified climate map image directly to disk as a PNG file.
     *
     * @param climateImage The source climate map image.
     * @param destinationFile The destination PNG file.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     * @param adjacentOnly If {@code true}, restricts matching to adjacent indexed colors.
     */
    public void exportBiomeMapAsPng(BufferedImage climateImage, File destinationFile, BiomeEntry defaultBiome,
                                    double maxTolerance, boolean adjacentOnly) {
        if ((destinationFile == null)) {
            throw new MDCCapturingRuntimeException("Destination file cannot be null");
        }
        exportBiomeMapAsPng(climateImage, destinationFile.toPath(), defaultBiome, maxTolerance, adjacentOnly);
    }

    /**
     * Exports a generated biome map from the specified climate map image directly to disk as a PNG file.
     *
     * @param climateImage The source climate map image.
     * @param destinationFile The destination PNG file.
     * @param defaultBiome The fallback biome for unmapped colors.
     * @param maxTolerance The color tolerance margin of error.
     */
    public void exportBiomeMapAsPng(BufferedImage climateImage, File destinationFile, BiomeEntry defaultBiome,
                                    double maxTolerance) {
        exportBiomeMapAsPng(climateImage, destinationFile, defaultBiome, maxTolerance, false);
    }

    /**
     * Returns the default color tolerance (margin of error) configured for this map.
     *
     * @return The color tolerance value in Euclidean RGB distance.
     */
    public double getColorTolerance() {
        return (colorTolerance);
    }

    /**
     * Sets the default color tolerance (margin of error) configured for this map.
     *
     * @param newColorTolerance The color tolerance value.
     */
    public void setColorTolerance(double newColorTolerance) {
        if ((newColorTolerance < 0.0)) {
            throw new MDCCapturingRuntimeException("Color tolerance cannot be negative: " + newColorTolerance);
        }
        colorTolerance = newColorTolerance;
    }

    /**
     * Checks whether an exact mapping exists for the specified RGB color.
     *
     * @param rgb The RGB color value (the alpha component is ignored).
     * @return {@code true} if an exact mapping exists, otherwise {@code false}.
     */
    public boolean hasColor(int rgb) {
        return (mappings.containsKey((rgb & RGB_MASK)));
    }

    /**
     * Checks whether an exact mapping exists for the specified {@link Color}.
     *
     * @param color The color to check.
     * @return {@code true} if an exact mapping exists, otherwise {@code false}.
     */
    public boolean hasColor(Color color) {
        if ((color == null)) {
            throw new MDCCapturingRuntimeException("Color cannot be null");
        }
        return (hasColor(color.getRGB()));
    }

    /**
     * Returns an unmodifiable view of all color-to-biome mappings in this map.
     *
     * @return An unmodifiable map of 24-bit RGB values to biome entries.
     */
    public Map<Integer, BiomeEntry> getMappings() {
        return (Collections.unmodifiableMap(mappings));
    }

    /**
     * Returns the total number of color-to-biome mappings defined.
     *
     * @return The number of mappings.
     */
    public int size() {
        return (mappings.size());
    }

    /**
     * Checks whether this map contains no mappings.
     *
     * @return {@code true} if empty, otherwise {@code false}.
     */
    public boolean isEmpty() {
        return (mappings.isEmpty());
    }

    /**
     * Adds or updates a color mapping.
     *
     * @param rgb The 24-bit RGB color.
     * @param biome The biome entry to associate with the color.
     */
    public void put(int rgb, BiomeEntry biome) {
        if ((biome == null)) {
            throw new MDCCapturingRuntimeException("Biome entry cannot be null for color 0x" + Integer.toHexString(rgb));
        }
        mappings.put((rgb & RGB_MASK), biome);
        neighborDistancesDirty = true;
    }

    /**
     * Adds or updates a color mapping from string representations.
     *
     * @param hexColor The hex color string (e.g. {@code "FF0000"}).
     * @param biomeIdentifier The biome identifier (e.g. {@code "desert"} or {@code "minecraft:desert"}).
     */
    public void put(String hexColor, String biomeIdentifier) {
        int rgb = parseHexColor(hexColor);
        BiomeEntry biome = BiomeResolver.resolve(biomeIdentifier);
        put(rgb, biome);
    }

    /**
     * Removes the mapping for the specified RGB color.
     *
     * @param rgb The RGB color to remove.
     * @return The previous biome entry mapped to this color, or {@code null} if none.
     */
    public BiomeEntry remove(int rgb) {
        BiomeEntry removed = mappings.remove((rgb & RGB_MASK));
        if ((removed != null)) {
            neighborDistancesDirty = true;
        }
        return (removed);
    }

    /**
     * Clears all color mappings.
     */
    public void clear() {
        mappings.clear();
        nearestNeighborDistances.clear();
        neighborDistancesDirty = false;
    }

    /**
     * Saves these color mappings to the specified file path in properties format.
     *
     * @param path The destination path.
     */
    public void save(Path path) {
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("Destination path cannot be null");
        }
        try {
            if ((path.getParent() != null)) {
                Files.createDirectories(path.getParent());
            }
            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                save(writer);
            }
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to save color definitions to " + path, exception);
        }
    }

    /**
     * Saves these color mappings to the specified {@link File}.
     *
     * @param file The destination file.
     */
    public void save(File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("Destination file cannot be null");
        }
        save(file.toPath());
    }

    /**
     * Saves these color mappings to the specified {@link Writer}.
     *
     * @param writer The destination writer.
     */
    public void save(Writer writer) {
        if ((writer == null)) {
            throw new MDCCapturingRuntimeException("Writer cannot be null");
        }
        try {
            writer.write("# Köppainter Color-to-Biome Definition File\n");
            writer.write("# Format: <HEX_COLOR> = <BIOME>\n\n");
            for (Map.Entry<Integer, BiomeEntry> entry : mappings.entrySet()) {
                String hex = formatHexColor(entry.getKey());
                BiomeEntry biome = entry.getValue();
                writer.write(hex + " = " + biome.getModernId() + "\n");
            }
            writer.flush();
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to write color definitions", exception);
        }
    }

    /**
     * Saves these color mappings to the specified {@link OutputStream} using UTF-8 encoding.
     *
     * @param outputStream The destination output stream.
     */
    public void save(OutputStream outputStream) {
        if ((outputStream == null)) {
            throw new MDCCapturingRuntimeException("OutputStream cannot be null");
        }
        save(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    private void loadFromReader(BufferedReader reader, String sourceDescription) {
        try {
            String line;
            int lineNumber = 0;
            while (((line = reader.readLine()) != null)) {
                lineNumber++;
                String trimmed = line.trim();
                if ((trimmed.isEmpty()) || (trimmed.startsWith("!")) || (trimmed.startsWith("//"))) {
                    continue;
                }

                int delimiterIndex = trimmed.indexOf('=');
                if ((delimiterIndex == -1)) {
                    delimiterIndex = trimmed.indexOf(':');
                }

                if ((trimmed.startsWith("#"))) {
                    // Check if this line is a hex color definition like '#00FF00 = plains'
                    if ((delimiterIndex != -1)) {
                        String possibleKey = trimmed.substring(1, delimiterIndex).trim();
                        if ((!trimmed.startsWith("# ")) && (isHexDigits(possibleKey))
                                && ((possibleKey.length() == 6) || (possibleKey.length() == 8))) {
                            // Valid hex color key starting with '#' - proceed
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }

                // Strip inline comments if present (must occur after the delimiter)
                if ((delimiterIndex != -1)) {
                    int commentIndex = trimmed.indexOf('#', delimiterIndex + 1);
                    if ((commentIndex != -1)) {
                        trimmed = trimmed.substring(0, commentIndex).trim();
                        delimiterIndex = trimmed.indexOf('=');
                        if ((delimiterIndex == -1)) {
                            delimiterIndex = trimmed.indexOf(':');
                        }
                    }
                }

                if ((delimiterIndex == -1)) {
                    throw new MDCCapturingRuntimeException("Invalid line in color definition (" + sourceDescription
                            + " at line " + lineNumber + "): missing '=' or ':' delimiter: \"" + line + "\"");
                }

                String key = trimmed.substring(0, delimiterIndex).trim();
                String value = trimmed.substring(delimiterIndex + 1).trim();

                if ((key.isEmpty())) {
                    throw new MDCCapturingRuntimeException("Invalid line in color definition (" + sourceDescription
                            + " at line " + lineNumber + "): empty color key: \"" + line + "\"");
                }
                if ((value.isEmpty())) {
                    throw new MDCCapturingRuntimeException("Invalid line in color definition (" + sourceDescription
                            + " at line " + lineNumber + "): empty biome value: \"" + line + "\"");
                }

                int rgb = parseHexColor(key);
                BiomeEntry biome = BiomeResolver.resolve(value);
                mappings.put(rgb, biome);
            }
            neighborDistancesDirty = true;
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Error reading color definitions from " + sourceDescription, exception);
        }
    }

    private void ensureNeighborDistances() {
        if ((neighborDistancesDirty)) {
            recomputeNeighborDistances();
        }
    }

    private void recomputeNeighborDistances() {
        nearestNeighborDistances.clear();
        if ((mappings.size() <= 1)) {
            for (Integer color : mappings.keySet()) {
                nearestNeighborDistances.put(color, Double.POSITIVE_INFINITY);
            }
            neighborDistancesDirty = false;
            return;
        }

        for (Integer color1 : mappings.keySet()) {
            int r1 = ((color1 >> 16) & 0xFF);
            int g1 = ((color1 >> 8) & 0xFF);
            int b1 = (color1 & 0xFF);
            long minDistanceSquared = Long.MAX_VALUE;

            for (Integer color2 : mappings.keySet()) {
                if ((color1.equals(color2))) {
                    continue;
                }
                int rDiff = (((color2 >> 16) & 0xFF) - r1);
                int gDiff = (((color2 >> 8) & 0xFF) - g1);
                int bDiff = ((color2 & 0xFF) - b1);
                long distanceSquared = (((long) rDiff * rDiff)
                        + ((long) gDiff * gDiff)
                        + ((long) bDiff * bDiff));
                if ((distanceSquared < minDistanceSquared)) {
                    minDistanceSquared = distanceSquared;
                }
            }

            nearestNeighborDistances.put(color1, Math.sqrt((double) minDistanceSquared));
        }
        neighborDistancesDirty = false;
    }

    /**
     * Loads color mappings from the specified file {@link Path}.
     *
     * @param path The path to the color definition file.
     * @return The loaded {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap load(Path path) {
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("File path cannot be null");
        }
        if ((!Files.exists(path))) {
            throw new MDCCapturingRuntimeException("Color definition file does not exist: " + path);
        }
        ColorBiomeMap map = new ColorBiomeMap();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            map.loadFromReader(reader, path.toString());
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to read color definition file from " + path, exception);
        }
        return (map);
    }

    /**
     * Loads color mappings from the specified {@link File}.
     *
     * @param file The color definition file.
     * @return The loaded {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap load(File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("File cannot be null");
        }
        return (load(file.toPath()));
    }

    /**
     * Loads color mappings from the specified {@link InputStream} using UTF-8 encoding.
     *
     * @param inputStream The input stream containing color definitions.
     * @return The loaded {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap load(InputStream inputStream) {
        if ((inputStream == null)) {
            throw new MDCCapturingRuntimeException("InputStream cannot be null");
        }
        return (load(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
    }

    /**
     * Loads color mappings from the specified {@link Reader}.
     *
     * @param reader The reader containing color definitions.
     * @return The loaded {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap load(Reader reader) {
        if ((reader == null)) {
            throw new MDCCapturingRuntimeException("Reader cannot be null");
        }
        BufferedReader bufferedReader = ((reader instanceof BufferedReader)
                ? (BufferedReader) reader : new BufferedReader(reader));
        ColorBiomeMap map = new ColorBiomeMap();
        map.loadFromReader(bufferedReader, "Reader stream");
        return (map);
    }

    /**
     * Loads the default color mappings packaged inside the plugin JAR.
     *
     * @return The default {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap loadDefault() {
        try (InputStream inputStream = ColorBiomeMap.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if ((inputStream == null)) {
                throw new MDCCapturingRuntimeException("Default color definition resource not found: "
                        + DEFAULT_RESOURCE_PATH);
            }
            ColorBiomeMap map = new ColorBiomeMap();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                map.loadFromReader(reader, ("classpath resource " + DEFAULT_RESOURCE_PATH));
            }
            return (map);
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to load default color definitions from classpath", exception);
        }
    }

    /**
     * Loads user-defined color mappings if found, falling back to the bundled defaults.
     *
     * <p>The search order is:</p>
     * <ol>
     *   <li>Explicit {@code userPath} if non-null and the file exists.</li>
     *   <li>Path specified by system property {@value #PROPERTY_BIOMES_FILE} if set and exists.</li>
     *   <li>User configuration file at {@link #getDefaultUserFilePath()} if it exists.</li>
     *   <li>Local working directory file {@code ./biomes.properties} if it exists.</li>
     *   <li>Built-in default classpath definitions.</li>
     * </ol>
     *
     * @param userPath An optional explicit user file path, or {@code null}.
     * @return The loaded {@code ColorBiomeMap}.
     */
    public static ColorBiomeMap loadUserOrDefault(Path userPath) {
        // 1. Explicit path parameter
        if ((userPath != null) && (Files.exists(userPath))) {
            return (load(userPath));
        }

        // 2. System property
        String sysPropPath = System.getProperty(PROPERTY_BIOMES_FILE);
        if ((sysPropPath != null) && (!sysPropPath.isBlank())) {
            Path propPath = Paths.get(sysPropPath.trim());
            if ((Files.exists(propPath))) {
                return (load(propPath));
            }
        }

        // 3. User configuration file in WorldPainter plugins directory
        Path defaultUserFile = getDefaultUserFilePath();
        if ((Files.exists(defaultUserFile))) {
            return (load(defaultUserFile));
        }

        // Also check ~/.worldpainter/plugins/koppainter if default was APPDATA
        String userHome = System.getProperty("user.home", ".");
        Path fallbackUserFile = Paths.get(userHome, ".worldpainter", "plugins", "koppainter", USER_FILE_NAME);
        if ((!fallbackUserFile.equals(defaultUserFile)) && (Files.exists(fallbackUserFile))) {
            return (load(fallbackUserFile));
        }

        // 4. Local working directory ./biomes.properties
        Path localFile = Paths.get(USER_FILE_NAME);
        if ((Files.exists(localFile))) {
            return (load(localFile));
        }

        // 5. Fallback to bundled defaults
        return (loadDefault());
    }

    /**
     * Copies the bundled default color definition file to the specified target path.
     *
     * @param targetPath The destination path where the default template should be written.
     */
    public static void exportDefault(Path targetPath) {
        if ((targetPath == null)) {
            throw new MDCCapturingRuntimeException("Target path cannot be null");
        }
        try {
            if ((targetPath.getParent() != null)) {
                Files.createDirectories(targetPath.getParent());
            }
            try (InputStream inputStream = ColorBiomeMap.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
                if ((inputStream == null)) {
                    throw new MDCCapturingRuntimeException("Default color definition resource not found: "
                            + DEFAULT_RESOURCE_PATH);
                }
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Failed to export default color definitions to " + targetPath, exception);
        }
    }

    /**
     * Returns the standard directory where user-definable plugin configurations are stored.
     *
     * <p>On Windows, checks {@code %APPDATA%\WorldPainter\plugins\koppainter} if available,
     * otherwise defaults to {@code ~/.worldpainter/plugins/koppainter}.</p>
     *
     * @return The user config directory path.
     */
    public static Path getDefaultUserDirectory() {
        String appData = System.getenv("APPDATA");
        if ((appData != null) && (!appData.isBlank())) {
            Path winPluginsDir = Paths.get(appData, "WorldPainter", "plugins");
            if ((Files.isDirectory(winPluginsDir))) {
                return (winPluginsDir.resolve("koppainter"));
            }
        }
        String userHome = System.getProperty("user.home", ".");
        return (Paths.get(userHome, ".worldpainter", "plugins", "koppainter"));
    }

    /**
     * Returns the standard path where a user-definable color definition file is looked for.
     *
     * @return The user configuration file path.
     */
    public static Path getDefaultUserFilePath() {
        return (getDefaultUserDirectory().resolve(USER_FILE_NAME));
    }

    /**
     * Parses a hex color string into a 24-bit RGB integer.
     *
     * <p>Accepts 6-digit RRGGBB or 8-digit AARRGGBB hex strings, with optional {@code "#"} or {@code "0x"} prefixes.
     * Letters are case-insensitive.</p>
     *
     * @param hexString The hex string (e.g. {@code "FF0000"}, {@code "#00FF00"}, {@code "0x0000FF"}).
     * @return The 24-bit RGB integer value.
     */
    public static int parseHexColor(String hexString) {
        if ((hexString == null) || (hexString.isBlank())) {
            throw new MDCCapturingRuntimeException("Color string cannot be null or blank");
        }
        String cleaned = hexString.trim();
        if ((cleaned.startsWith("#"))) {
            cleaned = cleaned.substring(1).trim();
        } else if ((cleaned.startsWith("0x")) || (cleaned.startsWith("0X"))) {
            cleaned = cleaned.substring(2).trim();
        }

        if (((cleaned.length() != 6) && (cleaned.length() != 8))) {
            throw new MDCCapturingRuntimeException("Invalid hex color format: \"" + hexString
                    + "\". Expected 6-digit RRGGBB or 8-digit AARRGGBB hex string.");
        }
        if ((!isHexDigits(cleaned))) {
            throw new MDCCapturingRuntimeException("Invalid hex color format: \"" + hexString
                    + "\". Non-hexadecimal characters found.");
        }

        try {
            long value = Long.parseLong(cleaned, 16);
            return ((int) (value & RGB_MASK));
        } catch (NumberFormatException exception) {
            throw new MDCCapturingRuntimeException("Malformed hex color value: \"" + hexString + "\"", exception);
        }
    }

    /**
     * Formats a 24-bit RGB integer into a 6-character uppercase hex string.
     *
     * @param rgb The RGB integer value.
     * @return The 6-character hex string (e.g. {@code "FF0000"}).
     */
    public static String formatHexColor(int rgb) {
        return (String.format("%06X", (rgb & RGB_MASK)));
    }

    /**
     * Exports the specified {@link BufferedImage} to disk as a PNG file.
     *
     * @param image The image to export.
     * @param path The destination path.
     */
    public static void exportAsPng(BufferedImage image, Path path) {
        if ((image == null)) {
            throw new MDCCapturingRuntimeException("Image cannot be null");
        }
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("Destination path cannot be null");
        }
        ImageLoader.saveAsPng(image, path);
    }

    /**
     * Exports the specified {@link BufferedImage} to disk as a PNG file.
     *
     * @param image The image to export.
     * @param file The destination file.
     */
    public static void exportAsPng(BufferedImage image, File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("Destination file cannot be null");
        }
        exportAsPng(image, file.toPath());
    }

    private static boolean isHexDigits(String text) {
        if ((text == null) || (text.isEmpty())) {
            return (false);
        }
        for (int i = 0; (i < text.length()); i++) {
            char c = text.charAt(i);
            if (((c < '0') || (c > '9')) && ((c < 'a') || (c > 'f')) && ((c < 'A') || (c > 'F'))) {
                return (false);
            }
        }
        return (true);
    }

    private final Map<Integer, BiomeEntry> mappings;
    private final Map<Integer, Double> nearestNeighborDistances;
    private boolean neighborDistancesDirty;
    private double colorTolerance;

    /**
     * Default Euclidean RGB distance tolerance (margin of error) for matching slightly-off colors.
     */
    public static final double DEFAULT_COLOR_TOLERANCE = 10.0;

    /**
     * Path to the bundled default color definition properties resource on the classpath.
     */
    public static final String DEFAULT_RESOURCE_PATH = "/com.github.bknackkr.koppainter.biomes.properties";

    /**
     * Default filename for user-defined color definition files.
     */
    public static final String USER_FILE_NAME = "biomes.properties";

    /**
     * System property name allowing users to override the color definition file path.
     */
    public static final String PROPERTY_BIOMES_FILE = "koppainter.biomes.file";

    private static final int RGB_MASK = 0x00FFFFFF;
}
