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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;
import org.pepsoft.util.mdc.MDCCapturingRuntimeException;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.WorldPainterDialog;
import org.pepsoft.worldpainter.biomeschemes.StaticBiomeInfo;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

/**
 * Modal dialog for selecting a lossless climate map image, configuring a default biome fallback, and previewing the
 * resulting Minecraft biomes.
 */
public class KoppainterDialog extends WorldPainterDialog {
    /**
     * Constructs a new {@code KoppainterDialog} with default color mappings.
     *
     * @param parent The parent window relative to which this dialog is displayed.
     */
    public KoppainterDialog(Window parent) {
        this(parent, ColorBiomeMap.loadUserOrDefault(null), null);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with the specified color mappings.
     *
     * @param parent The parent window.
     * @param colorBiomeMap The color-to-biome mappings to use for conversion.
     */
    public KoppainterDialog(Window parent, ColorBiomeMap colorBiomeMap) {
        this(parent, colorBiomeMap, null);
    }

    /**
     * Constructs a new {@code KoppainterDialog} with specified mappings and an optional preselected image file.
     *
     * @param parent The parent window.
     * @param colorBiomeMap The color-to-biome mappings to use.
     * @param preselectedFile An optional preselected image file, or {@code null}.
     */
    public KoppainterDialog(Window parent, ColorBiomeMap colorBiomeMap, File preselectedFile) {
        super(parent);
        this.colorBiomeMap = ((colorBiomeMap != null) ? colorBiomeMap : ColorBiomeMap.loadUserOrDefault(null));

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
        return (!isCancelled());
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
     * Confirms the dialog if a valid image has been loaded, or displays an alert prompting the user to select one.
     */
    @Override
    public void ok() {
        if ((climateImage == null)) {
            JOptionPane.showMessageDialog(this,
                    "Please select a valid climate image file (PNG, BMP, TIFF, or TGA) before proceeding.",
                    "No Image Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        super.ok();
    }

    /**
     * Sets the title of the dialog window, gracefully handling uninitialized WorldPainter configuration.
     *
     * @param title The dialog title text.
     */
    @Override
    public void setTitle(String title) {
        try {
            super.setTitle(title);
        } catch (NullPointerException exception) {
            // Configuration.getInstance() may be null in headless or test environments where WorldPainter
            // GUI is not running. Bypass WorldPainterDialog and set title directly on java.awt.Dialog.
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
            JOptionPane.showMessageDialog(this,
                    "Failed to load image:\n" + exception.getMessage(),
                    "Error Loading Image", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePreview() {
        if ((climateImage == null)) {
            previewPanel.setImage(null);
            labelStats.setText("No climate image loaded.");
            return;
        }

        BiomeEntry defaultBiome = getDefaultBiome();
        int width = climateImage.getWidth();
        int height = climateImage.getHeight();

        biomePreviewImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Set<Integer> uniqueColors = new HashSet<>();
        Set<BiomeEntry> biomesFound = new HashSet<>();
        int unmappedPixels = 0;
        int mappedPixels = 0;

        for (int y = 0; (y < height); y++) {
            for (int x = 0; (x < width); x++) {
                int rgb = (climateImage.getRGB(x, y) & 0x00FFFFFF);
                uniqueColors.add(rgb);

                BiomeEntry biome = colorBiomeMap.getBiome(rgb);
                if ((biome == null)) {
                    biome = defaultBiome;
                    unmappedPixels++;
                } else {
                    mappedPixels++;
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
        if ((unmappedPixels > 0)) {
            statsText += " | Unmapped pixels: " + unmappedPixels
                    + " (defaulted to " + defaultBiome.getName() + ")";
            labelStats.setForeground(new Color(0xB8, 0x62, 0x00));
        } else {
            statsText += " | All colors mapped successfully";
            labelStats.setForeground(new Color(0x00, 0x66, 0x00));
        }
        labelStats.setText(statsText);
    }

    private void initComponents() {
        setTitle("Import Köppen Climate Map");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        // Section 2: Default Biome
        // ---------------------------------------------------------------------
        JPanel defaultBiomeSection = new JPanel(new GridBagLayout());
        defaultBiomeSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2. Default Biome (For Unmapped Colors)"),
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
                "Pixels with colors not defined in the color definition file will be assigned this biome.");
        labelDefaultExplanation.setFont(labelDefaultExplanation.getFont().deriveFont(Font.PLAIN, 11.0f));
        labelDefaultExplanation.setForeground(Color.GRAY);
        gbcBiome.gridx = 1;
        gbcBiome.gridy = 1;
        gbcBiome.anchor = GridBagConstraints.WEST;
        defaultBiomeSection.add(labelDefaultExplanation, gbcBiome);

        mainPanel.add(defaultBiomeSection);
        mainPanel.add(Box.createVerticalStrut(8));

        // ---------------------------------------------------------------------
        // Section 3: Preview
        // ---------------------------------------------------------------------
        JPanel previewSection = new JPanel(new BorderLayout(4, 4));
        previewSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("3. Biome Preview"),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)));

        JPanel previewControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        radioShowBiomes = new JRadioButton("Minecraft Biomes", true);
        radioShowOriginal = new JRadioButton("Original Image", false);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(radioShowBiomes);
        viewGroup.add(radioShowOriginal);

        radioShowBiomes.addActionListener(e -> updatePreviewDisplayMode());
        radioShowOriginal.addActionListener(e -> updatePreviewDisplayMode());

        previewControls.add(new JLabel("View:"));
        previewControls.add(radioShowBiomes);
        previewControls.add(radioShowOriginal);
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
        // Section 4: Action Buttons (OK / Cancel)
        // ---------------------------------------------------------------------
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(e -> cancel());
        buttonPanel.add(buttonCancel);

        buttonOk = new JButton("OK");
        buttonOk.addActionListener(e -> ok());
        buttonPanel.add(buttonOk);

        add(buttonPanel, BorderLayout.SOUTH);
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

    /**
     * Determines the representative display color for the given biome entry in the preview window.
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
     * @param args Command-line arguments; optional first argument is an image file path to preload.
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

    private BufferedImage climateImage;
    private BufferedImage biomePreviewImage;
    private File selectedFile;

    private JTextField fieldFilePath;
    private JButton buttonBrowse;
    private JLabel labelImageInfo;
    private JComboBox<BiomeEntry> comboDefaultBiome;
    private JRadioButton radioShowBiomes;
    private JRadioButton radioShowOriginal;
    private PreviewPanel previewPanel;
    private JLabel labelStats;
    private JButton buttonOk;
    private JButton buttonCancel;

    private static final ColourScheme COLOUR_SCHEME = new HardcodedColourScheme();

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

    private static final long serialVersionUID = 1L;
}
