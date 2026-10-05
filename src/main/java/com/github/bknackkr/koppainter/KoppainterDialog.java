package com.github.bknackkr.koppainter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

/**
 * Modal dialog for selecting a lossless climate map image, configuring a
 * default biome fallback, and previewing the
 * resulting Minecraft biomes.
 */
public class KoppainterDialog extends WorldPainterDialog {
    /**
     * Constructs a new {@code KoppainterDialog} with default color mappings.
     *
     * @param parent The parent window relative to which this dialog is displayed.
     */
    public KoppainterDialog(Window parent) {
        this(parent, ColorBiomeMap.loadUserOrDefault(null), (org.pepsoft.worldpainter.Dimension) null, null);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with the specified color mappings.
     *
     * @param parent        The parent window.
     * @param colorBiomeMap The color-to-biome mappings to use for conversion.
     */
    public KoppainterDialog(Window parent, ColorBiomeMap colorBiomeMap) {
        this(parent, colorBiomeMap, (org.pepsoft.worldpainter.Dimension) null, null);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with specified mappings and an
     * optional preselected image file.
     *
     * @param parent          The parent window.
     * @param colorBiomeMap   The color-to-biome mappings to use.
     * @param preselectedFile An optional preselected image file, or {@code null}.
     */
    public KoppainterDialog(Window parent, ColorBiomeMap colorBiomeMap, File preselectedFile) {
        this(parent, colorBiomeMap, null, preselectedFile);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with specified mappings and a
     * target WorldPainter dimension.
     *
     * @param parent        The parent window.
     * @param colorBiomeMap The color-to-biome mappings to use.
     * @param dimension     The target WorldPainter dimension to modify.
     */
    public KoppainterDialog(
            Window parent,
            ColorBiomeMap colorBiomeMap,
            org.pepsoft.worldpainter.Dimension dimension) {
        this(parent, colorBiomeMap, dimension, null);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with specified mappings, a target
     * dimension, and an optional image.
     *
     * @param parent          The parent window.
     * @param colorBiomeMap   The color-to-biome mappings to use.
     * @param dimension       The target WorldPainter dimension to modify.
     * @param preselectedFile An optional preselected image file, or {@code null}.
     */
    public KoppainterDialog(
            Window parent,
            ColorBiomeMap colorBiomeMap,
            org.pepsoft.worldpainter.Dimension dimension,
            File preselectedFile) {
        super(parent);
        this.colorBiomeMap = ((colorBiomeMap != null) ? colorBiomeMap : ColorBiomeMap.loadUserOrDefault(null));
        this.dimension = dimension;

        initComponents();

        if ((preselectedFile != null) && preselectedFile.isFile()) {
            loadSelectedFile(preselectedFile);
        }

        scaleToUI();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Returns whether the user confirmed the dialog by pressing the OK button.
     *
     * @return {@code true} if confirmed, {@code false} if cancelled.
     */
    public boolean isConfirmed() {
        return (confirmed);
    }

    /**
     * Returns whether the biomes have been successfully applied to the WorldPainter
     * dimension.
     *
     * @return {@code true} if biomes have been applied, {@code false} otherwise.
     */
    public boolean isApplied() {
        return (applied);
    }

    /**
     * Returns whether the asynchronous biome application task is currently
     * executing.
     *
     * @return {@code true} if biome application is in progress, {@code false}
     *         otherwise.
     */
    public boolean isApplying() {
        return (applying);
    }

    /**
     * Returns the loaded source climate image.
     *
     * @return The source climate image, or {@code null} if none was loaded.
     */
    public BufferedImage getClimateImage() {
        return (climateImage);
    }

    /**
     * Returns the translated biome preview image.
     *
     * @return The biome map preview image, or {@code null} if none was generated.
     */
    public BufferedImage getBiomePreviewImage() {
        return (biomePreviewImage);
    }

    /**
     * Returns the selected climate image file on disk.
     *
     * @return The image file, or {@code null} if none was selected.
     */
    public File getSelectedFile() {
        return (selectedFile);
    }

    /**
     * Returns the selected default fallback biome for unmapped colors.
     *
     * @return The default biome entry.
     */
    public BiomeEntry getDefaultBiome() {
        return ((BiomeEntry) comboDefaultBiome.getSelectedItem());
    }

    /**
     * Returns the active color-to-biome map used by this dialog.
     *
     * @return The color biome map.
     */
    public ColorBiomeMap getColorBiomeMap() {
        return (colorBiomeMap);
    }

    /**
     * Returns the selected color tolerance (margin of error) for matching
     * slightly-off colors.
     *
     * @return The color tolerance value in Euclidean RGB distance.
     */
    public double getColorTolerance() {
        if ((spinnerTolerance == null)) {
            return (ColorBiomeMap.DEFAULT_COLOR_TOLERANCE);
        }
        return (((Number) spinnerTolerance.getValue()).doubleValue());
    }

    /**
     * Sets the color tolerance (margin of error) for matching slightly-off colors.
     *
     * @param tolerance The color tolerance value.
     */
    public void setColorTolerance(double tolerance) {
        if ((spinnerTolerance != null)) {
            spinnerTolerance.setValue((int) Math.round(tolerance));
        }
    }

    /**
     * Checks whether nearest-color matching is restricted to the 4 orthogonal
     * adjacent pixels.
     *
     * @return {@code true} if adjacent-only mode is enabled, {@code false}
     *         otherwise.
     */
    public boolean isAdjacentOnly() {
        return ((checkAdjacentOnly != null) && checkAdjacentOnly.isSelected());
    }

    /**
     * Sets whether nearest-color matching is restricted to the 4 orthogonal
     * adjacent pixels.
     *
     * @param adjacentOnly {@code true} to enable adjacent-only mode, {@code false}
     *                     otherwise.
     */
    public void setAdjacentOnly(boolean adjacentOnly) {
        if ((checkAdjacentOnly != null)) {
            checkAdjacentOnly.setSelected(adjacentOnly);
        }
    }

    /**
     * Exports the generated Minecraft biome preview map to disk as a PNG file.
     *
     * @param file The destination PNG file.
     */
    public void exportBiomeMapAsPng(File file) {
        if ((file == null)) {
            throw new MDCCapturingRuntimeException("Destination file cannot be null");
        }
        if ((biomePreviewImage == null)) {
            throw new MDCCapturingRuntimeException("No generated biome map image available to export");
        }
        ImageLoader.saveAsPng(biomePreviewImage, file);
    }

    /**
     * Exports the generated Minecraft biome preview map to disk as a PNG file.
     *
     * @param path The destination PNG path.
     */
    public void exportBiomeMapAsPng(Path path) {
        if ((path == null)) {
            throw new MDCCapturingRuntimeException("Destination path cannot be null");
        }
        exportBiomeMapAsPng(path.toFile());
    }

    /**
     * Returns the target WorldPainter dimension.
     *
     * @return The dimension, or {@code null} if none was specified.
     */
    public org.pepsoft.worldpainter.Dimension getDimension() {
        return (dimension);
    }

    /**
     * Sets the target WorldPainter dimension.
     *
     * @param dimension The dimension to apply biomes to.
     */
    public void setDimension(org.pepsoft.worldpainter.Dimension dimension) {
        this.dimension = dimension;
    }

    /**
     * Returns the progress bar embedded in this dialog.
     *
     * @return The progress bar.
     */
    public JProgressBar getProgressBar() {
        return (progressBar);
    }

    /**
     * Returns the action progress label embedded in this dialog.
     *
     * @return The progress action label.
     */
    public JLabel getProgressLabel() {
        return (labelProgressAction);
    }

    /**
     * Returns the persistent progress dialog displayed if this dialog is closed
     * while an action is running.
     *
     * @return The persistent progress dialog, or {@code null} if none is currently
     *         active.
     */
    public JDialog getPersistentProgressDialog() {
        return (persistentProgressDialog);
    }

    /**
     * Waits for the asynchronous biome application task to finish if currently
     * running.
     *
     * @throws InterruptedException If the current thread is interrupted while
     *                              waiting.
     */
    public void waitForApply() throws InterruptedException {
        Thread thread = applyThread;
        if ((thread != null)) {
            thread.join();
            if ((!javax.swing.SwingUtilities.isEventDispatchThread())) {
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() -> {
                        // Flushes any pending EDT invocations such as finishApplyBiomes
                    });
                } catch (java.lang.reflect.InvocationTargetException ignored) {
                    // Safe to ignore empty runnable
                }
            }
        }
    }

    /**
     * Confirms the dialog if a valid image has been loaded, or displays an alert
     * prompting the user to select one.
     */
    @Override
    public void ok() {
        if ((applying)) {
            return;
        }

        if ((climateImage == null)) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid climate image file (PNG, BMP, TIFF, or TGA) before proceeding.",
                    "No Image Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if ((dimension == null)) {
            confirmed = true;
            super.ok();
            return;
        }

        startApplyBiomesTask();
    }

    /**
     * Cancels the dialog or detaches to a persistent progress dialog if an
     * application task is already in progress.
     */
    @Override
    public void cancel() {
        if (applying) {
            handleWindowClosing();
            return;
        }
        confirmed = false;
        super.cancel();
    }

    /**
     * Sets the title of the dialog window, gracefully handling uninitialized
     * WorldPainter configuration.
     *
     * @param title The dialog title text.
     */
    @Override
    public void setTitle(String title) {
        try {
            super.setTitle(title);
        } catch (NullPointerException exception) {
            // Configuration.getInstance() may be null in headless or test environments
            // where WorldPainter
            // GUI is not running. Bypass WorldPainterDialog and set title directly on
            // java.awt.Dialog.
            try {
                java.lang.reflect.Field titleField = java.awt.Dialog.class.getDeclaredField("title");
                titleField.setAccessible(true);
                titleField.set(this, title);
            } catch (ReflectiveOperationException ignored) {
                // If reflection is restricted, safely ignore fallback title assignment
            }
        }
    }

    private void selectImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Climate Map Image (Lossless)");
        fileChooser.setAcceptAllFileFilterUsed(false);

        // Add filter for all supported lossless formats
        fileChooser.addChoosableFileFilter(new LosslessImageFilter(
                "Supported Lossless Formats (*.png, *.bmp, *.tif, *.tiff, *.tga)",
                "png", "bmp", "tif", "tiff", "tga"));
        fileChooser.addChoosableFileFilter(new LosslessImageFilter("PNG Images (*.png)", "png"));
        fileChooser.addChoosableFileFilter(new LosslessImageFilter("Bitmap Images (*.bmp)", "bmp"));
        fileChooser.addChoosableFileFilter(new LosslessImageFilter("TIFF Images (*.tif, *.tiff)", "tif", "tiff"));
        fileChooser.addChoosableFileFilter(new LosslessImageFilter("Truevision TGA Images (*.tga)", "tga"));

        if ((selectedFile != null) && selectedFile.getParentFile().exists()) {
            fileChooser.setCurrentDirectory(selectedFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(this);
        if ((result == JFileChooser.APPROVE_OPTION)) {
            File chosen = fileChooser.getSelectedFile();
            loadSelectedFile(chosen);
        }
    }

    private void loadSelectedFile(File file) {
        if ((file == null) || (!file.exists())) {
            return;
        }
        updateProgress(0, "Loading image: " + file.getName() + "...");
        try {
            BufferedImage loaded = ImageLoader.load(file);
            selectedFile = file;
            climateImage = loaded;
            fieldFilePath.setText(file.getAbsolutePath());
            String ext = ImageLoader.getExtension(file).toUpperCase();
            labelImageInfo.setText("Loaded: " + file.getName() + " (" + loaded.getWidth() + " × "
                    + loaded.getHeight() + ", " + ext + ")");
            labelImageInfo.setForeground(new Color(0x00, 0x7A, 0x00));
            updatePreview();
        } catch (MDCCapturingRuntimeException exception) {
            updateProgress(0, "Failed to load image");
            JOptionPane.showMessageDialog(this,
                    "Failed to load image:\n" + exception.getMessage(),
                    "Error Loading Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePreview() {
        if ((climateImage == null)) {
            previewPanel.setImage(null);
            labelStats.setText("No climate image loaded.");
            updateProgress(0, "Ready");
            return;
        }

        updateProgress(0, "Generating biome preview...");

        BiomeEntry defaultBiome = getDefaultBiome();
        double tolerance = getColorTolerance();
        boolean adjacentOnly = isAdjacentOnly();
        int width = climateImage.getWidth();
        int height = climateImage.getHeight();

        biomePreviewImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Set<Integer> uniqueColors = new HashSet<>();
        Set<BiomeEntry> biomesFound = new HashSet<>();
        int exactPixels = 0;
        int snappedPixels = 0;
        int unmappedPixels = 0;

        Map<Integer, BiomeEntry> resolutionCache = new HashMap<>();

        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                int rgb = (climateImage.getRGB(x, y) & 0x00FFFFFF);
                uniqueColors.add(rgb);

                BiomeEntry biome;
                if ((colorBiomeMap.hasColor(rgb))) {
                    biome = colorBiomeMap.getBiome(rgb);
                    exactPixels++;
                } else if (adjacentOnly) {
                    biome = null;
                    if ((tolerance > 0.0)) {
                        biome = colorBiomeMap.findNearestAdjacentBiome(climateImage, x, y, tolerance);
                    }
                    if ((biome != null)) {
                        snappedPixels++;
                    } else {
                        biome = defaultBiome;
                        unmappedPixels++;
                    }
                } else {
                    biome = resolutionCache.computeIfAbsent(rgb, c -> {
                        if ((tolerance > 0.0)) {
                            return (colorBiomeMap.findNearestBiome(c, tolerance));
                        }
                        return (null);
                    });
                    if ((biome != null)) {
                        snappedPixels++;
                    } else {
                        biome = defaultBiome;
                        unmappedPixels++;
                    }
                }

                biomesFound.add(biome);
                biomePreviewImage.setRGB(x, y, getBiomeColor(biome));
            }
        }

        boolean showOriginal = radioShowOriginal.isSelected();
        previewPanel.setImage((showOriginal ? climateImage : biomePreviewImage));

        String statsText = "Size: " + width + " × " + height
                + " | Unique colors: " + uniqueColors.size()
                + " | Biomes: " + biomesFound.size();
        if ((snappedPixels > 0)) {
            statsText += " | Coastline mixed: " + snappedPixels
                    + (adjacentOnly ? " corrected (adjacent)" : " corrected");
        }
        if ((unmappedPixels > 0)) {
            statsText += " | Unmapped: " + unmappedPixels
                    + " (defaulted to " + defaultBiome.getName() + ")";
            labelStats.setForeground(new Color(0xB8, 0x62, 0x00));
        } else {
            statsText += " | All colors mapped successfully";
            labelStats.setForeground(new Color(0x00, 0x66, 0x00));
        }
        labelStats.setText(statsText);
        updateProgress(100, "Preview ready (" + width + " × " + height + ")");
    }

    private void initComponents() {
        setTitle("Import Climate Map");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
        setLayout(new BorderLayout(8, 8));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // ---------------------------------------------------------------------
        // Section 1: Climate Image Input
        // ---------------------------------------------------------------------
        JPanel imageSection = new JPanel(new GridBagLayout());
        imageSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("1. Climate Image Input (PNG, BMP, TIFF, TGA)"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        JLabel labelFilePrompt = new JLabel("Climate map image:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        imageSection.add(labelFilePrompt, gbc);

        fieldFilePath = new JTextField(28);
        fieldFilePath.setEditable(false);
        fieldFilePath.setToolTipText("Path to the selected lossless climate map image");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        imageSection.add(fieldFilePath, gbc);

        buttonBrowse = new JButton("Browse...");
        buttonBrowse.setToolTipText("Open file explorer to select a climate map image");
        buttonBrowse.addActionListener(e -> selectImageFile());
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        imageSection.add(buttonBrowse, gbc);

        labelImageInfo = new JLabel("No image loaded (PNG, BMP, TIFF, and TGA formats supported)");
        labelImageInfo.setFont(labelImageInfo.getFont().deriveFont(Font.ITALIC, 11.0f));
        labelImageInfo.setForeground(Color.GRAY);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        imageSection.add(labelImageInfo, gbc);

        mainPanel.add(imageSection);
        mainPanel.add(Box.createVerticalStrut(8));

        // ---------------------------------------------------------------------
        // Section 2: Default Biome & Tolerance
        // ---------------------------------------------------------------------
        JPanel defaultBiomeSection = new JPanel(new GridBagLayout());
        defaultBiomeSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2. Biome Mapping & Coastline Tolerance"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        GridBagConstraints gbcBiome = new GridBagConstraints();
        gbcBiome.insets = new Insets(4, 4, 4, 4);

        JLabel labelDefaultPrompt = new JLabel("Fallback biome:");
        gbcBiome.gridx = 0;
        gbcBiome.gridy = 0;
        gbcBiome.anchor = GridBagConstraints.WEST;
        defaultBiomeSection.add(labelDefaultPrompt, gbcBiome);

        List<BiomeEntry> biomes = BiomeResolver.getAllStandardBiomes();
        comboDefaultBiome = new JComboBox<>(biomes.toArray(new BiomeEntry[0]));
        comboDefaultBiome.setRenderer(new BiomeComboBoxRenderer());

        // Default to Ocean if available
        BiomeEntry ocean = BiomeResolver.resolve("minecraft:ocean");
        comboDefaultBiome.setSelectedItem(ocean);
        comboDefaultBiome.addItemListener(e -> {
            if ((e.getStateChange() == ItemEvent.SELECTED)) {
                updatePreview();
            }
        });

        gbcBiome.gridx = 1;
        gbcBiome.gridy = 0;
        gbcBiome.fill = GridBagConstraints.HORIZONTAL;
        gbcBiome.weightx = 1.0;
        defaultBiomeSection.add(comboDefaultBiome, gbcBiome);

        JLabel labelDefaultExplanation = new JLabel(
                "Pixels with colors not defined or outside the tolerance margin will be assigned this biome.");
        labelDefaultExplanation.setFont(labelDefaultExplanation.getFont().deriveFont(Font.PLAIN, 11.0f));
        labelDefaultExplanation.setForeground(Color.GRAY);
        gbcBiome.gridx = 1;
        gbcBiome.gridy = 1;
        gbcBiome.anchor = GridBagConstraints.WEST;
        defaultBiomeSection.add(labelDefaultExplanation, gbcBiome);

        JLabel labelTolerancePrompt = new JLabel("Color tolerance:");
        gbcBiome.gridx = 0;
        gbcBiome.gridy = 2;
        gbcBiome.anchor = GridBagConstraints.WEST;
        gbcBiome.fill = GridBagConstraints.NONE;
        gbcBiome.weightx = 0.0;
        defaultBiomeSection.add(labelTolerancePrompt, gbcBiome);

        spinnerTolerance = new JSpinner(new SpinnerNumberModel(
                (int) Math.round(colorBiomeMap.getColorTolerance()), 0, 255, 1));
        spinnerTolerance.setToolTipText(
                "Margin of error in Euclidean RGB distance for converting mixed coastline colors (0 = exact only, max 255)");
        spinnerTolerance.addChangeListener(e -> updatePreview());

        JPanel toleranceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toleranceRow.add(spinnerTolerance);
        JLabel labelToleranceHint = new JLabel(
                "(Nearest-color margin of error in Euclidean RGB distance, 0–255)");
        labelToleranceHint.setFont(labelToleranceHint.getFont().deriveFont(Font.ITALIC, 11.0f));
        labelToleranceHint.setForeground(Color.GRAY);
        toleranceRow.add(labelToleranceHint);

        gbcBiome.gridx = 1;
        gbcBiome.gridy = 2;
        gbcBiome.fill = GridBagConstraints.HORIZONTAL;
        gbcBiome.weightx = 1.0;
        defaultBiomeSection.add(toleranceRow, gbcBiome);

        checkAdjacentOnly = new JCheckBox("Adjacent only", false);
        checkAdjacentOnly.setToolTipText(
                "Restricts nearest-color matching to indexed colors physically adjacent (4-connected) to each pixel");
        checkAdjacentOnly.addActionListener(e -> updatePreview());

        JPanel adjacentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        adjacentRow.add(checkAdjacentOnly);
        JLabel labelAdjacentHint = new JLabel(
                "(Checks 4 adjacent pixels for indexed colors; prevents false matches to gray polar biomes)");
        labelAdjacentHint.setFont(labelAdjacentHint.getFont().deriveFont(Font.ITALIC, 11.0f));
        labelAdjacentHint.setForeground(Color.GRAY);
        adjacentRow.add(labelAdjacentHint);

        gbcBiome.gridx = 1;
        gbcBiome.gridy = 3;
        gbcBiome.anchor = GridBagConstraints.WEST;
        gbcBiome.fill = GridBagConstraints.HORIZONTAL;
        gbcBiome.weightx = 1.0;
        defaultBiomeSection.add(adjacentRow, gbcBiome);

        mainPanel.add(defaultBiomeSection);
        mainPanel.add(Box.createVerticalStrut(8));

        // ---------------------------------------------------------------------
        // Section 3: Preview
        // ---------------------------------------------------------------------
        JPanel previewSection = new JPanel(new BorderLayout(4, 4));
        previewSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("3. Biome Preview"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        JPanel previewControls = new JPanel(new BorderLayout());
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        radioShowBiomes = new JRadioButton("Minecraft Biomes", true);
        radioShowOriginal = new JRadioButton("Original Image", false);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(radioShowBiomes);
        viewGroup.add(radioShowOriginal);

        radioShowBiomes.addActionListener(e -> updatePreviewDisplayMode());
        radioShowOriginal.addActionListener(e -> updatePreviewDisplayMode());

        leftControls.add(new JLabel("View:"));
        leftControls.add(radioShowBiomes);
        leftControls.add(radioShowOriginal);
        previewControls.add(leftControls, BorderLayout.WEST);

        buttonExportPng = new JButton("Export PNG...");
        buttonExportPng.setToolTipText("Export the generated Minecraft biome preview map as a PNG image file");
        buttonExportPng.addActionListener(e -> promptExportBiomeMap());
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        rightControls.add(buttonExportPng);
        previewControls.add(rightControls, BorderLayout.EAST);

        previewSection.add(previewControls, BorderLayout.NORTH);

        previewPanel = new PreviewPanel();
        previewPanel.setPreferredSize(new Dimension(540, 280));
        previewPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        previewSection.add(previewPanel, BorderLayout.CENTER);

        labelStats = new JLabel("No climate image loaded.");
        labelStats.setFont(labelStats.getFont().deriveFont(Font.PLAIN, 11.0f));
        labelStats.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        previewSection.add(labelStats, BorderLayout.SOUTH);

        mainPanel.add(previewSection);
        add(mainPanel, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Section 4: Progress Bar & Action Buttons
        // ---------------------------------------------------------------------
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 4));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        JPanel progressPanel = new JPanel(new BorderLayout(4, 2));
        labelProgressAction = new JLabel("Ready");
        labelProgressAction.setFont(labelProgressAction.getFont().deriveFont(Font.PLAIN, 11.0f));

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setPreferredSize(new Dimension(280, 20));

        progressPanel.add(labelProgressAction, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(progressPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(e -> cancel());
        buttonPanel.add(buttonCancel);

        buttonOk = new JButton("OK");
        buttonOk.addActionListener(e -> ok());
        buttonPanel.add(buttonOk);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(buttonOk);
    }

    private void updatePreviewDisplayMode() {
        if ((climateImage == null)) {
            return;
        }
        if (radioShowOriginal.isSelected()) {
            previewPanel.setImage(climateImage);
        } else {
            previewPanel.setImage(biomePreviewImage);
        }
    }

    private void promptExportBiomeMap() {
        if ((biomePreviewImage == null)) {
            JOptionPane.showMessageDialog(this,
                    "No biome map has been generated yet. Please load a climate map image first.",
                    "No Biome Map", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Biome Map as PNG");
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new LosslessImageFilter("PNG Images (*.png)", "png"));

        // Prepopulate default file name based on original image file name
        String suggestedName = "biome-map.png";
        if ((selectedFile != null)) {
            String baseName = selectedFile.getName();
            int dot = baseName.lastIndexOf('.');
            if ((dot != -1)) {
                baseName = baseName.substring(0, dot);
            }
            suggestedName = (baseName + "-biomes.png");
            if (selectedFile.getParentFile().exists()) {
                fileChooser.setCurrentDirectory(selectedFile.getParentFile());
            }
        }
        fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory(), suggestedName));

        int result = fileChooser.showSaveDialog(this);
        if ((result == JFileChooser.APPROVE_OPTION)) {
            File chosen = fileChooser.getSelectedFile();
            if ((chosen != null)) {
                if ((!chosen.getName().toLowerCase().endsWith(".png"))) {
                    chosen = new File(chosen.getParentFile(), chosen.getName() + ".png");
                }
                if (chosen.exists()) {
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "File \"" + chosen.getName() + "\" already exists. Do you want to replace it?",
                            "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if ((confirm != JOptionPane.YES_OPTION)) {
                        return;
                    }
                }
                try {
                    exportBiomeMapAsPng(chosen);
                    JOptionPane.showMessageDialog(this,
                            "Biome map successfully exported to:\n" + chosen.getAbsolutePath(),
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception exception) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to export biome map:\n" + exception.getMessage(),
                            "Export Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void startApplyBiomesTask() {
        applying = true;
        confirmed = true;

        if ((buttonOk != null)) {
            buttonOk.setEnabled(false);
        }
        if ((buttonBrowse != null)) {
            buttonBrowse.setEnabled(false);
        }
        if ((comboDefaultBiome != null)) {
            comboDefaultBiome.setEnabled(false);
        }
        if ((spinnerTolerance != null)) {
            spinnerTolerance.setEnabled(false);
        }
        if ((checkAdjacentOnly != null)) {
            checkAdjacentOnly.setEnabled(false);
        }
        if ((buttonExportPng != null)) {
            buttonExportPng.setEnabled(false);
        }
        if ((buttonCancel != null)) {
            buttonCancel.setText("Close");
        }

        updateProgress(0, "Starting biome application...");

        final BiomeEntry defaultBiome = getDefaultBiome();
        final int defaultBiomeId = (((defaultBiome != null) && (defaultBiome.getId() >= 0)) ? defaultBiome.getId() : 0);
        final double tolerance = getColorTolerance();
        final boolean adjacentOnly = isAdjacentOnly();

        // Run biome conversion on a worker thread so the UI thread remains responsive
        // and repaints progress
        applyThread = new Thread(() -> {
            Throwable error = null;
            try {
                KoppainterOperation.applyBiomes(
                        dimension,
                        climateImage,
                        colorBiomeMap,
                        defaultBiomeId,
                        tolerance,
                        adjacentOnly,
                        (percent, message) -> updateProgress(percent, message));
            } catch (Throwable t) {
                error = t;
            }
            final Throwable finalError = error;
            javax.swing.SwingUtilities.invokeLater(() -> finishApplyBiomes(finalError));
        }, "Koppainter-ApplyBiomes");
        applyThread.start();
    }

    private void updateProgress(int percent, String message) {
        Runnable updateRunnable = () -> {
            if ((progressBar != null)) {
                progressBar.setValue(percent);
                progressBar.setString(percent + "%");
            }
            if ((labelProgressAction != null)) {
                labelProgressAction.setText(message);
            }
            if ((persistentProgressBar != null)) {
                persistentProgressBar.setValue(percent);
                persistentProgressBar.setString(percent + "%");
            }
            if ((persistentActionLabel != null)) {
                persistentActionLabel.setText(message);
            }
        };

        if ((javax.swing.SwingUtilities.isEventDispatchThread())) {
            updateRunnable.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(updateRunnable);
        }
    }

    private void finishApplyBiomes(Throwable error) {
        applying = false;
        if ((persistentProgressDialog != null)) {
            persistentProgressDialog.dispose();
            persistentProgressDialog = null;
        }

        if ((error != null)) {
            updateProgress(0, "Error applying biomes");
            if ((buttonOk != null)) {
                buttonOk.setEnabled(true);
            }
            if ((buttonBrowse != null)) {
                buttonBrowse.setEnabled(true);
            }
            if ((comboDefaultBiome != null)) {
                comboDefaultBiome.setEnabled(true);
            }
            if ((spinnerTolerance != null)) {
                spinnerTolerance.setEnabled(true);
            }
            if ((checkAdjacentOnly != null)) {
                checkAdjacentOnly.setEnabled(true);
            }
            if ((buttonExportPng != null)) {
                buttonExportPng.setEnabled(true);
            }
            if ((buttonCancel != null)) {
                buttonCancel.setText("Cancel");
            }

            if ((!GraphicsEnvironment.isHeadless())) {
                JOptionPane.showMessageDialog(this,
                        "Failed to apply biomes:\n" + error.getMessage(),
                        "Error Applying Biomes", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            applied = true;
            updateProgress(100, "Biomes applied successfully");
            super.ok();
        }
    }

    private void handleWindowClosing() {
        if ((applying)) {
            // Main dialog is being closed while an operation is in progress.
            // Hide the main dialog window and open a persistent progress dialog so the user
            // can continue
            // tracking progress until completion.
            setVisible(false);
            showPersistentProgressDialog();
        } else {
            cancel();
        }
    }

    private void showPersistentProgressDialog() {
        if ((GraphicsEnvironment.isHeadless())) {
            return;
        }
        if ((persistentProgressDialog == null)) {
            Window owner = getOwner();
            persistentProgressDialog = new JDialog(owner, "Applying Biomes - Köppainter",
                    java.awt.Dialog.ModalityType.MODELESS);
            persistentProgressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

            String initialAction = (((labelProgressAction != null) && (labelProgressAction.getText() != null))
                    ? labelProgressAction.getText()
                    : "Applying biomes...");
            persistentActionLabel = new JLabel(initialAction);
            persistentActionLabel.setFont(persistentActionLabel.getFont().deriveFont(Font.PLAIN, 12.0f));

            persistentProgressBar = new JProgressBar(0, 100);
            int currentVal = ((progressBar != null) ? progressBar.getValue() : 0);
            String currentStr = ((progressBar != null) ? progressBar.getString() : "");
            persistentProgressBar.setValue(currentVal);
            persistentProgressBar.setStringPainted(true);
            persistentProgressBar.setString(currentStr);
            persistentProgressBar.setPreferredSize(new Dimension(340, 24));

            JLabel noteLabel = new JLabel("Köppainter is applying biomes to the WorldPainter map in the background.");
            noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC, 11.0f));
            noteLabel.setForeground(Color.GRAY);

            panel.add(persistentActionLabel, BorderLayout.NORTH);
            panel.add(persistentProgressBar, BorderLayout.CENTER);
            panel.add(noteLabel, BorderLayout.SOUTH);

            persistentProgressDialog.setContentPane(panel);
            persistentProgressDialog.pack();
            persistentProgressDialog.setLocationRelativeTo(this);
        } else {
            if (((labelProgressAction != null) && (persistentActionLabel != null))) {
                persistentActionLabel.setText(labelProgressAction.getText());
            }
            if (((progressBar != null) && (persistentProgressBar != null))) {
                persistentProgressBar.setValue(progressBar.getValue());
                persistentProgressBar.setString(progressBar.getString());
            }
        }
        persistentProgressDialog.setVisible(true);
    }

    /**
     * Determines the representative display color for the given biome entry in the
     * preview window.
     *
     * @param biome The biome entry.
     * @return The 24-bit RGB preview color.
     */
    public static int getBiomeColor(BiomeEntry biome) {
        if ((biome == null)) {
            return (0x000000);
        }
        int id = biome.getId();
        if (((id >= 0) && (id < 256))) {
            try {
                return (StaticBiomeInfo.INSTANCE.getColour(id, COLOUR_SCHEME));
            } catch (Throwable ignored) {
                // If WorldPainter colour scheme fails, fall back to algorithm below
            }
        }
        // Custom or unallocated biome fallback: deterministic distinct pastel color
        int hash = Math.abs(biome.getModernId().hashCode());
        int r = (60 + (hash % 160));
        int g = (60 + ((hash / 160) % 160));
        int b = (60 + ((hash / 25600) % 160));
        return (((r << 16) | (g << 8) | b));
    }

    /**
     * Standalone entry point allowing direct testing and previewing of the dialog.
     *
     * @param args Command-line arguments; optional first argument is an image file
     *             path to preload.
     */
    public static void main(String[] args) {
        if ((org.pepsoft.worldpainter.Configuration.getInstance() == null)) {
            org.pepsoft.worldpainter.Configuration.setInstance(new org.pepsoft.worldpainter.Configuration());
        }
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep default Look & Feel if system L&F is unavailable
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            File preload = (((args.length > 0) && (args[0] != null)) ? new File(args[0]) : null);
            KoppainterDialog dialog = new KoppainterDialog(null, ColorBiomeMap.loadUserOrDefault(null), preload);
            dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setVisible(true);
        });
    }

    private final ColorBiomeMap colorBiomeMap;
    private org.pepsoft.worldpainter.Dimension dimension;
    private boolean confirmed;
    private boolean applied;
    private volatile boolean applying;
    private Thread applyThread;

    private BufferedImage climateImage;
    private BufferedImage biomePreviewImage;
    private File selectedFile;

    private JTextField fieldFilePath;
    private JButton buttonBrowse;
    private JLabel labelImageInfo;
    private JComboBox<BiomeEntry> comboDefaultBiome;
    private JSpinner spinnerTolerance;
    private JCheckBox checkAdjacentOnly;
    private JRadioButton radioShowBiomes;
    private JRadioButton radioShowOriginal;
    private PreviewPanel previewPanel;
    private JLabel labelStats;
    private JLabel labelProgressAction;
    private JProgressBar progressBar;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JButton buttonExportPng;
    private JDialog persistentProgressDialog;
    private JProgressBar persistentProgressBar;
    private JLabel persistentActionLabel;

    private static final ColourScheme COLOUR_SCHEME = new HardcodedColourScheme();
    private static final long serialVersionUID = 1L;

    private static class BiomeComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if ((value instanceof BiomeEntry biome)) {
                setText(biome.getName() + " (" + biome.getModernId() + ")");
                setIcon(createBiomeIcon(getBiomeColor(biome)));
            }
            return (this);
        }

        private static Icon createBiomeIcon(int rgb) {
            BufferedImage image = new BufferedImage(14, 14, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            try {
                g2.setColor(new Color((rgb & 0xFFFFFF)));
                g2.fillRect(0, 0, 14, 14);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(0, 0, 13, 13);
            } finally {
                g2.dispose();
            }
            return (new ImageIcon(image));
        }

        private static final long serialVersionUID = 1L;
    }

    private static class PreviewPanel extends JPanel {
        /**
         * Updates the image displayed in the preview panel.
         *
         * @param image The image to display, or {@code null}.
         */
        public void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int panelWidth = getWidth();
                int panelHeight = getHeight();

                if ((image == null)) {
                    g2.setColor(new Color(0x2B, 0x2B, 0x2B));
                    g2.fillRect(0, 0, panelWidth, panelHeight);
                    g2.setColor(Color.LIGHT_GRAY);
                    String message = "No climate image loaded. Click 'Browse...' above to select an image.";
                    FontMetrics fm = g2.getFontMetrics();
                    int x = ((panelWidth - fm.stringWidth(message)) / 2);
                    int y = (((panelHeight - fm.getHeight()) / 2) + fm.getAscent());
                    g2.drawString(message, Math.max(10, x), y);
                    return;
                }

                // Render subtle checkerboard for background
                paintCheckerboard(g2, panelWidth, panelHeight);

                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int imgWidth = image.getWidth();
                int imgHeight = image.getHeight();

                double scaleX = ((double) panelWidth / imgWidth);
                double scaleY = ((double) panelHeight / imgHeight);
                double scale = Math.min(scaleX, scaleY);

                int drawWidth = Math.max(1, (int) Math.round((imgWidth * scale)));
                int drawHeight = Math.max(1, (int) Math.round((imgHeight * scale)));
                int drawX = ((panelWidth - drawWidth) / 2);
                int drawY = ((panelHeight - drawHeight) / 2);

                g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(drawX, drawY, drawWidth, drawHeight);
            } finally {
                g2.dispose();
            }
        }

        private static void paintCheckerboard(Graphics2D g2, int width, int height) {
            int size = 16;
            Color light = new Color(0x2E, 0x2E, 0x2E);
            Color dark = new Color(0x22, 0x22, 0x22);
            for (int y = 0; (y < height); y += size) {
                for (int x = 0; (x < width); x += size) {
                    boolean even = ((((x / size) + (y / size)) % 2) == 0);
                    g2.setColor((even ? light : dark));
                    g2.fillRect(x, y, size, size);
                }
            }
        }

        private BufferedImage image;
        private static final long serialVersionUID = 1L;
    }

    private static class LosslessImageFilter extends FileFilter {
        public LosslessImageFilter(String description, String... extensions) {
            this.description = description;
            this.extensions = extensions;
        }

        @Override
        public boolean accept(File f) {
            if ((f == null)) {
                return (false);
            }
            if (f.isDirectory()) {
                return (true);
            }
            String name = f.getName().toLowerCase();
            for (String ext : extensions) {
                if (name.endsWith("." + ext.toLowerCase())) {
                    return (true);
                }
            }
            return (false);
        }

        @Override
        public String getDescription() {
            return (description);
        }

        private final String description;
        private final String[] extensions;
    }
}
