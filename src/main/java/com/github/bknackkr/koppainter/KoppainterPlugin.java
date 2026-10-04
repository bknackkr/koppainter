package com.github.bknackkr.koppainter;

import java.util.Collections;
import java.util.List;
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
    }

    /**
     * Initialises the plugin when WorldPainter starts up and the application context becomes available.
     *
     * @param context The WorldPainter application context.
     */
    @Override
    public void init(WPContext context) {
        // Context initialisation will be wired here as features are added
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
     * The human-readable display name of the plugin.
     */
    public static final String NAME = "Köppainter";

    private static final List<Operation> OPERATIONS = Collections.emptyList();
}
