package com.github.bknackkr.koppainter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;

/**
 * Utility class for making the Maven project version number available at runtime.
 */
public final class Version {
    private Version() {
        // Utility class; prevent instantiation
    }

    /**
     * The version of the plugin as determined by Maven build-time filtering.
     */
    public static final String VERSION;

    private static final String PROPERTIES_FILE = "/com.github.bknackkr.koppainter.properties";
    private static final String PROPERTY_VERSION = "com.github.bknackkr.koppainter.version";

    static {
        Properties versionProperties = new Properties();
        try (InputStream inputStream = Version.class.getResourceAsStream(PROPERTIES_FILE)) {
            if ((inputStream == null)) {
                throw new MDCCapturingRuntimeException("Plugin properties resource not found: " + PROPERTIES_FILE);
            }
            versionProperties.load(inputStream);
            String loadedVersion = versionProperties.getProperty(PROPERTY_VERSION);
            if ((loadedVersion == null)) {
                throw new MDCCapturingRuntimeException("Version property not defined in " + PROPERTIES_FILE);
            }
            VERSION = loadedVersion;
        } catch (IOException exception) {
            throw new MDCCapturingRuntimeException("I/O error loading version number from classpath", exception);
        }
    }
}
