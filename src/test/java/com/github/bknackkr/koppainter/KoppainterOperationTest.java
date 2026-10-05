package com.github.bknackkr.koppainter;

import java.awt.image.BufferedImage;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoppainterOperationTest {
    @Test
    void testApplyBiomesOriginAlignedWithTopLeftCorner() {
        KoppainterPlugin plugin = new KoppainterPlugin();
        KoppainterOperation operation = new KoppainterOperation(plugin);

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

        int imageWidth = 10;
        int imageHeight = 10;
        BufferedImage climate = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

        int redRgb = 0xFF0000;
        int blueRgb = 0x0000FF;
        climate.setRGB(0, 0, redRgb);
        climate.setRGB(9, 9, blueRgb);

        ColorBiomeMap map = new ColorBiomeMap(Map.of(
                redRgb, new BiomeEntry(2, "minecraft:desert", "Desert"),
                blueRgb, new BiomeEntry(0, "minecraft:ocean", "Ocean")
        ));

        operation.applyBiomes(dimension, climate, map, 0, 0.0, false);

        // Biome origin (0, 0) should be placed at world (0, 0), which is in tile (0, 0) at (0, 0)
        assertEquals((2), (tile00.getLayerValue(Biome.INSTANCE, 0, 0)));
        // The bottom-right pixel (9, 9) should be placed at world (9, 9), in tile (0, 0) at (9, 9)
        assertEquals((0), (tile00.getLayerValue(Biome.INSTANCE, 9, 9)));
    }

    @Test
    void testOperationMetadata() {
        KoppainterPlugin plugin = new KoppainterPlugin();
        KoppainterOperation operation = new KoppainterOperation(plugin);

        assertEquals(("Import Köppen Climate Map"), (KoppainterOperation.NAME));
        assertNotNull((KoppainterOperation.DESCRIPTION));
        assertNotNull((operation.getName()));
    }

    @Test
    void testApplyBiomesProgressReporting() {
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
        dimension.addTile(new Tile(0, 0, 0, 256));

        BufferedImage climate = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ColorBiomeMap map = new ColorBiomeMap(Map.of());

        java.util.List<Integer> progressUpdates = new java.util.ArrayList<>();
        java.util.List<String> messageUpdates = new java.util.ArrayList<>();

        KoppainterOperation.applyBiomes(dimension, climate, map, 0, 0.0, false, (percent, msg) -> {
            progressUpdates.add(percent);
            messageUpdates.add(msg);
        });

        assertFalse((progressUpdates.isEmpty()));
        assertEquals((100), (progressUpdates.get((progressUpdates.size() - 1))));
        assertTrue((messageUpdates.get((messageUpdates.size() - 1)).contains("row 10 of 10")));
    }
}
