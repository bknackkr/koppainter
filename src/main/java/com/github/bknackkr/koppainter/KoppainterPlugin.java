package com.github.bknackkr.koppainter;

import java.util.Collections;
import java.util.List;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.WPContext;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.OperationProvider;

import static com.github.bknackkr.koppainter.Version.VERSION;

/**
 * Main plugin class for Köppainter.
 *
 * <p>Registered in {@code org.pepsoft.worldpainter.plugins} and instantiated by WorldPainter.
 */
public class KoppainterPlugin extends AbstractPlugin implements OperationProvider {
    /**
     * Default constructor required by WorldPainter's plugin loader.
     */
    public KoppainterPlugin() {
        super(NAME, VERSION);
        colorBiomeMap = ColorBiomeMap.loadDefault();
    }

    /**
     * Initialises the plugin when WorldPainter starts up and the application context becomes available.
     *
     * @param context The WorldPainter application context.
     */
    @Override
    public void init(WPContext context) {
        // Load user-defined biomes.properties if available, falling back to bundled defaults
        colorBiomeMap = ColorBiomeMap.loadUserOrDefault(null);
    }

    /**
     * Returns the list of custom operations provided by this plugin for the WorldPainter Tools panel.
     *
     * @return The list of operations.
     */
    @Override
    public List<Operation> getOperations() {
        return (OPERATIONS);
    }

    /**
     * Returns the active color-to-biome mapping configuration.
     *
     * @return The active color-to-biome mapping.
     */
    public ColorBiomeMap getColorBiomeMap() {
        return (colorBiomeMap);
    }

    /**
     * Sets the active color-to-biome mapping configuration.
     *
     * @param colorBiomeMap The new color-to-biome mapping.
     */
    public void setColorBiomeMap(ColorBiomeMap colorBiomeMap) {
        if ((colorBiomeMap == null)) {
            throw new MDCCapturingRuntimeException("ColorBiomeMap cannot be null");
        }
        this.colorBiomeMap = colorBiomeMap;
    }

    private ColorBiomeMap colorBiomeMap;

    /**
     * The human-readable display name of the plugin.
     */
    public static final String NAME = "Köppainter";

    private static final List<Operation> OPERATIONS = Collections.emptyList();
}
