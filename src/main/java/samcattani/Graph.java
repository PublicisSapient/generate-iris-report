package samcattani;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Graph extends JFrame {
    GraphData graphData;
    final int GRAPH_WIDTH = 700;
    final int GRAPH_HEIGHT = 560;
    final int PANEL_WIDTH = 700;
    final int PANEL_HEIGHT = 630;

    JFrame frame;
    GraphImage panel;
    JPanel p;

    protected Graph(GraphData graphData) {
        super();
        this.graphData = graphData;

        p = new JPanel();
        p.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        panel = new GraphImage(graphData);
        panel.setPreferredSize(new Dimension(GRAPH_WIDTH, GRAPH_HEIGHT)); 
        p.add(panel);


        JLabel blue = new JLabel("--------");
        blue.setBackground(Color.BLUE);
        blue.setForeground(Color.BLUE);
        Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
        attributes.put(TextAttribute.TRACKING, -0.3);
        blue.setFont(blue.getFont().deriveFont(attributes));
        p.add(blue);

        JLabel luminance = new JLabel("Average Luminance");
        p.add(luminance);

        JLabel spacer = new JLabel("            ");
        p.add(spacer);

        JLabel red = new JLabel("- - - -");
        red.setBackground(Color.RED);
        red.setForeground(Color.RED);
        p.add(red);

        JLabel redLabel = new JLabel("Average Red");
        p.add(redLabel);

        getContentPane().add(p);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    String saveAsImage() {
        BufferedImage bi = new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bi.createGraphics();
        p.print(graphics);
        File outputfile = new File("image.png");
        try {
            ImageIO.write(bi, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        graphics.dispose();
        dispose();
        return outputfile.toPath().toString();
    }
}
