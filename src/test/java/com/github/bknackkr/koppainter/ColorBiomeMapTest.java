package com.github.bknackkr.koppainter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ColorBiomeMap}.
 */
public class ColorBiomeMapTest {
    /**
     * Default constructor for test suite.
     */
    public ColorBiomeMapTest() {
    }

    /**
     * Verifies that the default bundled color definition file is loaded and contains the requested example mapping.
     */
    @Test
    public void testDefaultMappingsLoaded() {
        ColorBiomeMap map = ColorBiomeMap.loadDefault();
        assertFalse(map.isEmpty());
        assertTrue((map.size() >= 25));

        // Verify the user's specific requested mapping: FF0000 = desert
        BiomeEntry desert = map.getBiome(0xFF0000);
        assertNotNull(desert);
        assertEquals("minecraft:desert", desert.getModernId());
        assertEquals(2, desert.getId());
        assertEquals("Desert", desert.getName());

        // Verify lookup with AWT Color and string hex
        assertEquals(desert, map.getBiome(new Color(255, 0, 0)));
        assertEquals(desert, map.getBiome("FF0000"));
        assertEquals(desert, map.getBiome("#ff0000"));
        assertEquals(desert, map.getBiome("0xFF0000"));

        // Verify alpha channel masking (0xFFFF0000 has 255 alpha)
        assertEquals(desert, map.getBiome(0xFFFF0000));
    }

    /**
     * Verifies hex color parsing utility methods.
     */
    @Test
    public void testHexColorParsing() {
        assertEquals(0xFF0000, ColorBiomeMap.parseHexColor("FF0000"));
        assertEquals(0xFF0000, ColorBiomeMap.parseHexColor("#FF0000"));
        assertEquals(0xFF0000, ColorBiomeMap.parseHexColor("0xFF0000"));
        assertEquals(0xFF0000, ColorBiomeMap.parseHexColor("0Xff0000"));
        assertEquals(0x00FF00, ColorBiomeMap.parseHexColor("00FF00"));
        assertEquals(0xFF0000, ColorBiomeMap.parseHexColor("FFFF0000"));
        assertEquals("FF0000", ColorBiomeMap.formatHexColor(0xFF0000));
        assertEquals("00FF00", ColorBiomeMap.formatHexColor(0x00FF00));
    }

    /**
     * Verifies Euclidean nearest color biome matching.
     */
    @Test
    public void testNearestBiomeMatching() {
        ColorBiomeMap map = new ColorBiomeMap();
        map.put("FF0000", "desert");
        map.put("00FF00", "plains");
        map.put("0000FF", "ocean");

        // Exactly matches
        assertEquals("minecraft:desert", map.findNearestBiome(0xFF0000).getModernId());

        // Near-red (250, 5, 2) should match desert
        BiomeEntry nearRed = map.findNearestBiome(new Color(250, 5, 2));
        assertNotNull(nearRed);
        assertEquals("minecraft:desert", nearRed.getModernId());

        // Near-green (10, 240, 15) should match plains
        BiomeEntry nearGreen = map.findNearestBiome(new Color(10, 240, 15));
        assertNotNull(nearGreen);
        assertEquals("minecraft:plains", nearGreen.getModernId());
    }

    /**
     * Verifies parsing of user-definable definition files with various syntax styles and comments.
     */
    @Test
    public void testLoadCustomDefinitionFromReader() {
        String content = """
                # Custom Color-to-Biome Definition
                ! Another comment style
                // Third comment style
                
                FF0000 = desert
                #00FF00: plains
                0x0000FF = ocean # Inline comment explaining blue is ocean
                FF123456: minecraft:dark_forest
                """;

        ColorBiomeMap map = ColorBiomeMap.load(new StringReader(content));
        assertEquals(4, map.size());

        assertEquals("minecraft:desert", map.getBiome("FF0000").getModernId());
        assertEquals("minecraft:plains", map.getBiome("00FF00").getModernId());
        assertEquals("minecraft:ocean", map.getBiome("0000FF").getModernId());
        assertEquals("minecraft:dark_forest", map.getBiome("123456").getModernId());
    }

    /**
     * Verifies saving and re-loading round trip to ensure serialization fidelity.
     *
     * @param tempDir Temporary directory provided by JUnit Jupiter.
     * @throws Exception If an unexpected error occurs.
     */
    @Test
    public void testSaveAndReloadRoundTrip(@TempDir Path tempDir) throws Exception {
        ColorBiomeMap original = new ColorBiomeMap();
        original.put("FF0000", "desert");
        original.put("00FF00", "plains");
        original.put("0000FF", "ocean");

        Path filePath = tempDir.resolve("test-biomes.properties");
        original.save(filePath);

        assertTrue(Files.exists(filePath));
        ColorBiomeMap reloaded = ColorBiomeMap.load(filePath);
        assertEquals(original.size(), reloaded.size());
        assertEquals(original.getMappings(), reloaded.getMappings());
    }

    /**
     * Verifies exporting the default definition file to disk.
     *
     * @param tempDir Temporary directory provided by JUnit Jupiter.
     * @throws Exception If an unexpected error occurs.
     */
    @Test
    public void testExportDefault(@TempDir Path tempDir) throws Exception {
        Path exportPath = tempDir.resolve("exported-biomes.properties");
        ColorBiomeMap.exportDefault(exportPath);

        assertTrue(Files.exists(exportPath));
        ColorBiomeMap exportedMap = ColorBiomeMap.load(exportPath);
        assertFalse(exportedMap.isEmpty());
        assertEquals("minecraft:desert", exportedMap.getBiome("FF0000").getModernId());
    }

    /**
     * Verifies error handling for malformed color definition lines and inputs.
     */
    @Test
    public void testInvalidInputHandling() {
        ColorBiomeMap map = new ColorBiomeMap();

        // Null checks
        assertThrows(MDCCapturingRuntimeException.class, () -> map.put(null, "desert"));
        assertThrows(MDCCapturingRuntimeException.class, () -> map.put("FF0000", null));
        assertThrows(MDCCapturingRuntimeException.class, () -> map.getBiome((Color) null));
        assertThrows(MDCCapturingRuntimeException.class, () -> map.hasColor((Color) null));

        // Malformed hex in reader
        String invalidHex = "NOT_HEX = desert\n";
        assertThrows(MDCCapturingRuntimeException.class, () -> ColorBiomeMap.load(new StringReader(invalidHex)));

        // Missing delimiter in reader
        String missingDelimiter = "FF0000 desert\n";
        assertThrows(MDCCapturingRuntimeException.class, () -> ColorBiomeMap.load(new StringReader(missingDelimiter)));

        // Empty color key
        String emptyKey = "= desert\n";
        assertThrows(MDCCapturingRuntimeException.class, () -> ColorBiomeMap.load(new StringReader(emptyKey)));

        // Empty biome value
        String emptyValue = "FF0000 =\n";
        assertThrows(MDCCapturingRuntimeException.class, () -> ColorBiomeMap.load(new StringReader(emptyValue)));
    }

    /**
     * Verifies that findNearestBiome with tolerance snaps slightly-off colors within tolerance,
     * but returns null for way-off colors.
     */
    @Test
    public void testNearestBiomeWithTolerance() {
        ColorBiomeMap map = new ColorBiomeMap();
        map.put("FF0000", "desert");
        map.put("00FF00", "plains");
        map.put("0000FF", "ocean");

        // Exact match should match with any non-negative tolerance
        assertEquals("minecraft:desert", map.findNearestBiome(0xFF0000, 10.0).getModernId());
        assertEquals(0xFF0000, map.findNearestColor(0xFF0000, 10.0));

        // Slightly-off color: (252, 2, 1) -> distance to FF0000 is sqrt(9 + 4 + 1) = sqrt(14) ~ 3.74
        int slightlyOffDesert = 0xFC0201;
        BiomeEntry matchedBiome = map.findNearestBiome(slightlyOffDesert, 10.0);
        assertNotNull(matchedBiome);
        assertEquals("minecraft:desert", matchedBiome.getModernId());
        assertEquals(0xFF0000, map.findNearestColor(slightlyOffDesert, 10.0));

        // When tolerance is too small (e.g. 2.0), distance ~3.74 is rejected
        assertNull(map.findNearestBiome(slightlyOffDesert, 2.0));
        assertNull(map.findNearestColor(slightlyOffDesert, 2.0));

        // Color that is way off (e.g. gray 0x808080) is rejected at tolerance 10
        assertNull(map.findNearestBiome(0x808080, 10.0));
        assertNull(map.findNearestColor(0x808080, 10.0));

        // Zero tolerance should only match exact color
        assertEquals("minecraft:desert", map.findNearestBiome(0xFF0000, 0.0).getModernId());
        assertNull(map.findNearestBiome(slightlyOffDesert, 0.0));
    }

    /**
     * Verifies that the dynamic neighbor-distance limit prevents similar indexed colors from
     * being confused or misidentified, even when a large global tolerance is specified.
     */
    @Test
    public void testDynamicNeighborClampingPreventsMisidentification() {
        ColorBiomeMap map = new ColorBiomeMap();
        // Two colors separated by distance 20:
        // Color A: (100, 100, 100) -> 0x646464
        // Color B: (100, 100, 120) -> 0x646478
        map.put("646464", "desert");
        map.put("646478", "ocean");

        double neighborDistA = map.getNearestNeighborDistance(0x646464);
        double neighborDistB = map.getNearestNeighborDistance(0x646478);
        assertEquals(20.0, neighborDistA, 0.001);
        assertEquals(20.0, neighborDistB, 0.001);

        // Effective tolerance should be clamped to strictly less than 10.0 (half of 20)
        double effTolerance = map.getEffectiveTolerance(0x646464, 30.0);
        assertTrue((effTolerance < 10.0));
        assertTrue((effTolerance >= 9.999));

        // Exact colors should always resolve to themselves
        assertEquals("minecraft:desert", map.findNearestBiome(0x646464, 30.0).getModernId());
        assertEquals("minecraft:ocean", map.findNearestBiome(0x646478, 30.0).getModernId());

        // A color 4 units away from A: (100, 100, 96) -> distance 4 (< 10)
        int nearA = 0x646460;
        assertEquals("minecraft:desert", map.findNearestBiome(nearA, 30.0).getModernId());

        // A color 12 units away from A: (100, 100, 88) -> distance 12 (> 10)
        // Without clamping, a global tolerance of 30 would accept it, but clamping MUST reject it
        int tooFarFromA = 0x646458;
        assertNull(map.findNearestBiome(tooFarFromA, 30.0));
        assertNull(map.findNearestColor(tooFarFromA, 30.0));

        // Exact midpoint: (100, 100, 110) -> distance 10 to both A and B, rejected
        int midpoint = 0x64646E;
        assertNull(map.findNearestBiome(midpoint, 30.0));
        assertNull(map.findNearestColor(midpoint, 30.0));
    }

    /**
     * Verifies that convertToIndexedColors snaps slightly-off colors to nearest indexed colors,
     * while leaving way-off colors and exact colors intact.
     */
    @Test
    public void testConvertToIndexedColors() {
        ColorBiomeMap map = new ColorBiomeMap();
        map.put("FF0000", "desert");
        map.put("00FF00", "plains");

        BufferedImage image = new BufferedImage(3, 1, BufferedImage.TYPE_INT_RGB);
        // Pixel 0: Exact red (FF0000)
        image.setRGB(0, 0, 0xFF0000);
        // Pixel 1: Slightly-off red (FE0101) -> distance ~ 2.45
        image.setRGB(1, 0, 0xFE0101);
        // Pixel 2: Way-off color (0000FF blue) -> distance > 255
        image.setRGB(2, 0, 0x0000FF);

        BufferedImage converted = map.convertToIndexedColors(image, 10.0);
        assertNotNull(converted);
        assertEquals(0xFF0000, (converted.getRGB(0, 0) & 0x00FFFFFF));
        assertEquals(0xFF0000, (converted.getRGB(1, 0) & 0x00FFFFFF));
        assertEquals(0x0000FF, (converted.getRGB(2, 0) & 0x00FFFFFF));
    }
}
