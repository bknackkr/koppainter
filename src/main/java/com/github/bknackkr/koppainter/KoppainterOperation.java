package com.github.bknackkr.koppainter;

import java.awt.Window;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
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
            KoppainterDialog dialog = new KoppainterDialog(parent, plugin.getColorBiomeMap());
            dialog.setVisible(true);
            if ((dialog.isConfirmed())) {
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

    private void applyBiomes(KoppainterDialog dialog) {
        Dimension dimension = getDimension();
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

        int width = climate.getWidth();
        int height = climate.getHeight();
        int originX = (-(width / 2));
        int originY = (-(height / 2));

        for (int y = 0; (y < height); y++) {
            int worldY = (originY + y);
            for (int x = 0; (x < width); x++) {
                int worldX = (originX + x);
                int rgb = (climate.getRGB(x, y) & 0x00FFFFFF);
                BiomeEntry biome = map.getBiome(rgb);
                int biomeId = (((biome != null) && (biome.getId() >= 0)) ? biome.getId() : defaultBiomeId);

                Tile tile = dimension.getTile((worldX >> 7), (worldY >> 7));
                if ((tile != null)) {
                    tile.setLayerValue(Biome.INSTANCE, (worldX & 127), (worldY & 127), biomeId);
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
