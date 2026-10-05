package com.github.bknackkr.koppainter;

import java.awt.Window;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.operations.AbstractOperation;

/**
 * WorldPainter operation that provides a tool button to open the {@link KoppainterDialog} and convert climate map
 * images into Minecraft biomes.
 */
public class KoppainterOperation extends AbstractOperation {
    /**
     * Constructs a new {@code KoppainterOperation} associated with the plugin instance.
     *
     * @param plugin The parent plugin instance.
     */
    public KoppainterOperation(KoppainterPlugin plugin) {
        super(NAME, DESCRIPTION, "biome");
        if ((plugin == null)) {
            throw new MDCCapturingRuntimeException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    @Override
    public void interrupt() {
        // Modal dialog operation; no continuous painting background state to interrupt
    }

    @Override
    protected void activate() {
        try {
            Window parent = ((getView() != null) ? SwingUtilities.getWindowAncestor(getView()) : null);
            Dimension dimension = getDimension();
            KoppainterDialog dialog = new KoppainterDialog(parent, plugin.getColorBiomeMap(), dimension);
            dialog.setVisible(true);
            if ((dialog.isConfirmed()) && (!dialog.isApplied()) && (!dialog.isApplying())) {
                applyBiomes(dialog);
            }
        } finally {
            try {
                setActive(false);
            } catch (PropertyVetoException ignored) {
                // Operation completed; deactivation veto can be safely ignored
            }
        }
    }

    @Override
    protected void deactivate() {
        // No active background painting state to clean up upon deactivation
    }

    void applyBiomes(KoppainterDialog dialog) {
        Dimension dimension = getDimension();
        if ((dimension == null)) {
            dimension = dialog.getDimension();
        }
        if ((dimension == null)) {
            return;
        }
        BufferedImage climate = dialog.getClimateImage();
        if ((climate == null)) {
            return;
        }
        ColorBiomeMap map = dialog.getColorBiomeMap();
        BiomeEntry defaultBiome = dialog.getDefaultBiome();
        int defaultBiomeId = ((defaultBiome != null) ? defaultBiome.getId() : 0);
        double tolerance = dialog.getColorTolerance();
        boolean adjacentOnly = dialog.isAdjacentOnly();

        applyBiomes(dimension, climate, map, defaultBiomeId, tolerance, adjacentOnly, null);
    }

    static void applyBiomes(
            Dimension dimension,
            BufferedImage climate,
            ColorBiomeMap map,
            int defaultBiomeId,
            double tolerance,
            boolean adjacentOnly) {
        applyBiomes(dimension, climate, map, defaultBiomeId, tolerance, adjacentOnly, null);
    }

    static void applyBiomes(
            Dimension dimension,
            BufferedImage climate,
            ColorBiomeMap map,
            int defaultBiomeId,
            double tolerance,
            boolean adjacentOnly,
            java.util.function.BiConsumer<Integer, String> progressConsumer) {
        int width = climate.getWidth();
        int height = climate.getHeight();
        // Align the biome map origin (0, 0) with the top-left corner of the WorldPainter map
        // so that the full extent of the climate image is mapped across positive coordinates.
        int originX = 0;
        int originY = 0;

        Map<Integer, Integer> biomeIdCache = new HashMap<>();
        int lastPercent = -1;

        for (int y = 0; (y < height); y++) {
            int worldY = (originY + y);
            for (int x = 0; (x < width); x++) {
                int worldX = (originX + x);
                int rgb = (climate.getRGB(x, y) & 0x00FFFFFF);

                int biomeId;
                BiomeEntry biome = map.getBiome(rgb);
                if ((biome != null)) {
                    biomeId = (((biome.getId() >= 0)) ? biome.getId() : defaultBiomeId);
                } else if (adjacentOnly) {
                    if ((tolerance > 0.0)) {
                        biome = map.findNearestAdjacentBiome(climate, x, y, tolerance);
                    }
                    biomeId = (((biome != null) && (biome.getId() >= 0)) ? biome.getId() : defaultBiomeId);
                } else {
                    biomeId = biomeIdCache.computeIfAbsent(rgb, color -> {
                        BiomeEntry b = null;
                        if ((tolerance > 0.0)) {
                            b = map.findNearestBiome(color, tolerance);
                        }
                        return (((b != null) && (b.getId() >= 0)) ? b.getId() : defaultBiomeId);
                    });
                }

                Tile tile = dimension.getTile((worldX >> 7), (worldY >> 7));
                if ((tile != null)) {
                    tile.setLayerValue(Biome.INSTANCE, (worldX & 127), (worldY & 127), biomeId);
                }
            }

            if ((progressConsumer != null)) {
                int percent = (int) ((((y + 1) * 100.0)) / height);
                if (((percent != lastPercent) || (y == (height - 1)))) {
                    progressConsumer.accept(percent, "Applying biomes to map: row " + (y + 1) + " of " + height
                            + " (" + percent + "%)");
                    lastPercent = percent;
                }
            }
        }
    }

    private final KoppainterPlugin plugin;

    /**
     * Human-readable display name of the operation in WorldPainter's tools panel.
     */
    public static final String NAME = "Import Köppen Climate Map";

    /**
     * Description of the operation displayed in WorldPainter's tooltips.
     */
    public static final String DESCRIPTION = "Import a Köppen climate map image and translate colors to Minecraft biomes";
}
