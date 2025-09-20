package samcattani;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Graph {
    private final GraphData graphData;

    private static final int GRAPH_WIDTH  = 700;
    private static final int GRAPH_HEIGHT = 560;
    private static final int PANEL_WIDTH  = 700;
    private static final int PANEL_HEIGHT = 630;

    // Root container that holds the chart + legend (no top-level window)
    private final JPanel root;

    public Graph(GraphData graphData) {
        this.graphData = graphData;
        this.root = buildPanel(graphData);
        // Size & layout so printing works off-screen
        this.root.setSize(PANEL_WIDTH, PANEL_HEIGHT);
        this.root.doLayout();
    }

    private JPanel buildPanel(GraphData data) {
        JPanel p = new JPanel(); // default FlowLayout is fine for your legend
        p.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        GraphImage chart = new GraphImage(data);
        chart.setPreferredSize(new Dimension(GRAPH_WIDTH, GRAPH_HEIGHT));
        p.add(chart);

        JLabel blue = new JLabel("--------");
        blue.setForeground(Color.BLUE);
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.TRACKING, -0.3);
        blue.setFont(blue.getFont().deriveFont(attrs));
        p.add(blue);

        p.add(new JLabel("Average Luminance"));
        p.add(new JLabel("            ")); // spacer

        JLabel red = new JLabel("- - - -");
        red.setForeground(Color.RED);
        p.add(red);
        p.add(new JLabel("Average Red"));

        return p;
    }

    /** Render the assembled panel to a PNG on disk (headless-safe). */
    public Path saveAsImage(Path outputPath) throws IOException {
        BufferedImage bi = new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            // Nice-to-have: better text/line quality
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Print all child components into the off-screen image
            root.printAll(g);
        } finally {
            g.dispose();
        }
        ImageIO.write(bi, "png", outputPath.toFile());
        return outputPath;
    }


    /** Back-compat wrapper for existing callers (returns the file path as String). */
    public String saveAsImage() {
        Path out = Paths.get("image.png");
        try {
            saveAsImage(out);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write graph image", e);
        }
    }

    /** Optional helper for interactive runs (NOT headless). */
    public void showFrame() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("Cannot show a frame in headless mode");
        }
        JFrame f = new JFrame("IRIS Graph");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.getContentPane().add(root);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}