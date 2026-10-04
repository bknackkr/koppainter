package com.github.bknackkr.koppainter;

import java.awt.Color;
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
import java.util.LinkedHashMap;
import java.util.Map;
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
        return (mappings.remove((rgb & RGB_MASK)));
    }

    /**
     * Clears all color mappings.
     */
    public void clear() {
        mappings.clear();
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
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("Error reading color definitions from " + sourceDescription, exception);
        }
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
