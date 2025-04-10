package samcattani;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.awt.Point;

import javax.swing.JPanel;

public class GraphImage extends JPanel {
    GraphData graphData;
    private static final int BORDER_GAP = 50;
    private double maxScore;
    private static final int Y_HATCH_CNT = 8;

    private static final int GRAPH_POINT_WIDTH = 5;
    private static final int GRAPH_HEIGHT2 = 500;

    public GraphImage(GraphData graphData) {
        this.graphData = graphData;
        maxScore = Math.max(graphData.maxLuminance, graphData.maxRed);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double xScale = ((double) getWidth() - 2 * BORDER_GAP) / (graphData.lumVals.size() - 1);
        double yScale = ((double) GRAPH_HEIGHT2 - 2 * BORDER_GAP) / (maxScore);

        List<Point> graphPoints = new ArrayList<Point>();
        List<Point> graphPointsRed = new ArrayList<Point>();

        for (int i = 0; i < graphData.lumVals.size(); i++) {
            int x1 = (int) (i * xScale + BORDER_GAP);
            int y1 = (int) ((maxScore - graphData.lumVals.get(i)) * yScale + BORDER_GAP);
            graphPoints.add(new Point(x1, y1));
        }

        for (int i = 0; i < graphData.redVals.size(); i++) {
            int x1 = (int) (i * xScale + BORDER_GAP);
            int y1 = (int) ((maxScore - graphData.redVals.get(i)) * yScale + BORDER_GAP);
            graphPointsRed.add(new Point(x1, y1));
        }
        g2.setStroke(new BasicStroke(2f));
        // create x and y axes
        g2.drawLine(BORDER_GAP, GRAPH_HEIGHT2 - BORDER_GAP, BORDER_GAP, BORDER_GAP);
        g2.drawLine(BORDER_GAP, GRAPH_HEIGHT2 - BORDER_GAP, getWidth() - BORDER_GAP, GRAPH_HEIGHT2 - BORDER_GAP);

        Double yLabel = maxScore / 8;
        // create hatch marks for y axis.
        for (int i = 0; i < Y_HATCH_CNT; i++) {
            int x0 = BORDER_GAP;
            int x1 = GRAPH_POINT_WIDTH + BORDER_GAP;
            int y0 = GRAPH_HEIGHT2 - (((i + 1) * (GRAPH_HEIGHT2 - BORDER_GAP * 2)) / Y_HATCH_CNT + BORDER_GAP);
            int y1 = y0;
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x0, y0, x1, y1);
            String yLabelString = ((Double) (Math.round(yLabel * 100.0) / 100.0)).toString();
            if (yLabelString.equals("2.0")) {
                drawRotate(g2, x0 - 3, y0 + 5, 0, "2.0+");
            } else {
                drawRotate(g2, x0 - 3, y0 + 5, 0, yLabelString);
            }
            yLabel += maxScore / 8;
        }

        ArrayList<String> filteredTimeStamps = graphData.getTimeStamps();

        // and for x axis
        for (int i = 0; i < filteredTimeStamps.size(); i++) {
            int x0 = (i + 1) * (getWidth() - BORDER_GAP * 2) / (filteredTimeStamps.size()) + BORDER_GAP;
            int x1 = x0;
            int y0 = GRAPH_HEIGHT2 - BORDER_GAP;
            int y1 = y0 - GRAPH_POINT_WIDTH - 10;
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(x0, y0, x1, y1);
            if (i != filteredTimeStamps.size() - 1) {
                drawRotate(g2, x0 + 3, y0 + 10, 315, filteredTimeStamps.get(i));
            }
        }

        g2.setColor(Color.RED);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < graphPointsRed.size() - 1; i++) {
            int x1 = graphPointsRed.get(i).x;
            int y1 = graphPointsRed.get(i).y;
            int x2 = graphPointsRed.get(i + 1).x;
            int y2 = graphPointsRed.get(i + 1).y;
            drawDashedLine(g2, x1, y1, x2, y2);
        }

        g2.setColor(Color.BLUE);
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < graphPoints.size() - 1; i++) {
            int x1 = graphPoints.get(i).x;
            int y1 = graphPoints.get(i).y;
            int x2 = graphPoints.get(i + 1).x;
            int y2 = graphPoints.get(i + 1).y;
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    int getYHatchCount() {
        return Y_HATCH_CNT;
    }

    public static void drawRotate(Graphics2D g2d, double x, double y, int angle, String text) {
        g2d.translate((float) x, (float) y);
        g2d.rotate(Math.toRadians(angle));
        g2d.setFont(new Font("plain", Font.PLAIN, 12));
        FontMetrics fontMetrics = g2d.getFontMetrics();
        g2d.drawString(text, (int) 0 - fontMetrics.stringWidth(text), (int) 0);
        g2d.rotate(-Math.toRadians(angle));
        g2d.translate(-(float) x, -(float) y);
    }

    public void drawDashedLine(Graphics g, int x1, int y1, int x2, int y2) {
        Graphics2D g2d = (Graphics2D) g.create();

        float dash1[] = { 10.0f, 5.0f };
        BasicStroke dashed = new BasicStroke(1.0f,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10.0f, dash1, 0.0f);
        g2d.setStroke(dashed);
        g2d.setColor(Color.RED);
        g2d.drawLine(x1, y1, x2, y2);
        g2d.dispose();
    }
}
