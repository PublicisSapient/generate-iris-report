package samcattani;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GUI {
    private JLabel appDescription;
    private JSeparator separator;
    private JLabel irisFileFieldErrorLabel;
    private JLabel irisFileFieldLabel;
    private JButton irisFileField;
    private JFileChooser irisFileFieldSelector;
    private JLabel videoFileFieldErrorLabel;
    private JLabel videoFileFieldLabel;
    private JButton videoFileField;
    private JFileChooser videoFileFieldSelector;
    private JLabel pdfFileFieldErrorLabel;
    private JLabel pdfFileFieldLabel;
    private JTextField pdfFileField;
    private JButton button;
    private JLabel resultLabel;
    private JFrame frame;

    final String irisFieldHint = "Select IRIS executable";
    final String videoFieldHint = "Select Video to Test";
    final String pdfFieldHint = "~/path/to/file/filename.pdf";

    protected GUI(Interface generateReportAction) {
        this.setUpWindow(generateReportAction);
    }

    protected GUI(Interface generateReportAction, String iris, String video, String pdf) {
        this.setUpWindow(generateReportAction);
        if (!iris.isEmpty()) {
            irisFileField.setText(iris);
            irisFileField.setForeground(Color.BLACK);
        }

        if (!video.isEmpty()) {
            videoFileField.setText(video);
            videoFileField.setForeground(Color.BLACK);
        }

        if (!pdf.isEmpty()) {
            pdfFileField.setText(pdf);
            pdfFileField.setForeground(Color.BLACK);
        }
    }

    private void setUpWindow(Interface generateReportAction) {
        frame = new JFrame();
        frame.setTitle("Generate Accessible Report for Video");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        appDescription = new JLabel("<html><br>This mini app can be used to get an accessible PDF report for a video" +
                "<br>Please enter the full path with the path and name of the IRIS executable, the full path of the video you'd like to test, and a path with a file name where you'd like to save a PDF</html>");
        appDescription.setVisible(true);
        frame.add(appDescription);

        separator = new JSeparator();
        separator.setOrientation(JSeparator.HORIZONTAL);
        frame.add(separator);

        irisFileFieldErrorLabel = new JLabel();
        irisFileFieldErrorLabel.setForeground(Color.RED);
        irisFileFieldErrorLabel.setVisible(false);
        frame.add(irisFileFieldErrorLabel);

        irisFileFieldLabel = new JLabel("Path to IRIS executable");
        irisFileFieldLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                irisFileField.doClick();
            }
        });
        frame.add(irisFileFieldLabel);

        irisFileFieldSelector = new JFileChooser();

        irisFileField = new JButton();
        irisFileField.getAccessibleContext().setAccessibleDescription("Select IRIS executable");
        irisFileField.setText(irisFieldHint);
        irisFileField.setHorizontalAlignment(SwingConstants.LEFT);
        irisFileField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFile(irisFileFieldSelector, irisFileField);
            }
        });
        irisFileField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                    setFile(irisFileFieldSelector, irisFileField);
            }
        });
        frame.add(irisFileField);

        videoFileFieldErrorLabel = new JLabel();
        videoFileFieldErrorLabel.setForeground(Color.RED);
        videoFileFieldErrorLabel.setVisible(false);
        frame.add(videoFileFieldErrorLabel);

        videoFileFieldLabel = new JLabel("Path to video");
        videoFileFieldLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                videoFileField.doClick();
            }
        });
        frame.add(videoFileFieldLabel);

        videoFileFieldSelector = new JFileChooser();

        videoFileField = new JButton();
        videoFileField.getAccessibleContext().setAccessibleDescription("Path to video");
        videoFileField.setText(videoFieldHint);
        videoFileField.setHorizontalAlignment(SwingConstants.LEFT);
        videoFileField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setFile(videoFileFieldSelector, videoFileField);
            }
        });
        videoFileField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                setFile(videoFileFieldSelector, videoFileField);
            }
        });
        frame.add(videoFileField);

        pdfFileFieldErrorLabel = new JLabel();
        pdfFileFieldErrorLabel.setForeground(Color.RED);
        pdfFileFieldErrorLabel.setVisible(false);
        frame.add(pdfFileFieldErrorLabel);

        pdfFileFieldLabel = new JLabel("File path to save PDF");
        pdfFileFieldLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                pdfFileField.grabFocus();
            }
        });
        frame.add(pdfFileFieldLabel);

        pdfFileField = new JTextField();
        pdfFileField.getAccessibleContext().setAccessibleDescription("PDF filename");
        pdfFileField.setText(pdfFieldHint);
        pdfFileField.setForeground(Color.LIGHT_GRAY);
        pdfFileField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (pdfFileField.getText().equals(pdfFieldHint)) {
                    pdfFileField.setForeground(Color.BLACK);
                    pdfFileField.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (pdfFileField.getText().equals("")) {
                    pdfFileField.setText(pdfFieldHint);
                    pdfFileField.setForeground(Color.LIGHT_GRAY);
                }
            }
        });
        frame.add(pdfFileField);

        button = new JButton(" Generate Accessible PDF Report");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                generateReportAction.generateReportAction();
            }
        });
        button.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    generateReportAction.generateReportAction();
                }
            }
        });

        frame.add(button);

        resultLabel = new JLabel();
        resultLabel.setVisible(false);
        frame.add(resultLabel);

        frame.setSize(800, 400);
        frame.setMinimumSize(new Dimension(800, 400));
        frame.setLayout(null);
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                makeResponsive();
            }
        });
        frame.setVisible(true);
        frame.requestFocus();
    }

    private void setFile(JFileChooser fileChooser, JButton fileButton) {
        int returnVal = fileChooser.showOpenDialog(frame);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            fileButton.setText(file.toPath().toString());
        }
    }

    private void makeResponsive() {
        Dimension newSize = frame.getContentPane().getSize();
        double width = newSize.getWidth();
        double height = newSize.getHeight();

        appDescription.setBounds(25, 0, (int) (width - 50), (int) (0.3 * height));
        separator.setBounds(25,
                (int) ((appDescription.getBounds().y + appDescription.getBounds().height) + 0.0147 * height),
                (int) (width - 50),
                (int) (0.044 * height));
        irisFileFieldErrorLabel.setBounds((int) (0.1 * width),
                (int) ((separator.getBounds().y + separator.getBounds().height) + 0.0147 * height),
                (int) (width * 0.8),
                (int) (0.044 * height));
        irisFileFieldLabel.setBounds((int) (0.1 * width),
                (int) ((irisFileFieldErrorLabel.getBounds().y + irisFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.2),
                (int) (0.0735 * height));
        irisFileField.setBounds((int) (0.3 * width),
                (int) ((irisFileFieldErrorLabel.getBounds().y + irisFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.65),
                (int) (0.0735 * height));
        videoFileFieldErrorLabel.setBounds((int) (0.1 * width),
                (int) ((irisFileField.getBounds().y + irisFileField.getBounds().height) + 0.0147 * height),
                (int) (width * 0.8),
                (int) (0.044 * height));
        videoFileFieldLabel.setBounds((int) (0.1 * width),
                (int) ((videoFileFieldErrorLabel.getBounds().y + videoFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.2),
                (int) (0.0735 * height));
        videoFileField.setBounds((int) (0.3 * width),
                (int) ((videoFileFieldErrorLabel.getBounds().y + videoFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.65),
                (int) (0.0735 * height));
        pdfFileFieldErrorLabel.setBounds((int) (0.1 * width),
                (int) ((videoFileField.getBounds().y + videoFileField.getBounds().height) + 0.0147 * height),
                (int) (width * 0.8),
                (int) (0.044 * height));
        pdfFileFieldLabel.setBounds((int) (0.1 * width),
                (int) ((pdfFileFieldErrorLabel.getBounds().y + pdfFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.2),
                (int) (0.0735 * height));
        pdfFileField.setBounds((int) (0.3 * width),
                (int) ((pdfFileFieldErrorLabel.getBounds().y + pdfFileFieldErrorLabel.getBounds().height)),
                (int) (width * 0.65),
                (int) (0.0735 * height));
        button.setBounds((int) (0.3 * width),
                (int) ((pdfFileField.getBounds().y + pdfFileField.getBounds().height) + 0.0294 * height),
                (int) (width * 0.647),
                (int) (0.147 * height));
        resultLabel.setBounds((int) (0.34 * width),
                (int) ((button.getBounds().y + button.getBounds().height) + 0.0147 * height),
                (int) (width * 0.647),
                (int) (0.073529 * height));

        Font original = appDescription.getFont();
        Font resized = original.deriveFont((float) (height * 0.035));
        appDescription.setFont(resized);
        separator.setFont(resized);
        irisFileFieldErrorLabel.setFont(resized);
        irisFileFieldLabel.setFont(resized);
        irisFileField.setFont(resized);
        pdfFileFieldErrorLabel.setFont(resized);
        pdfFileFieldLabel.setFont(resized);
        pdfFileField.setFont(resized);
        videoFileFieldErrorLabel.setFont(resized);
        videoFileFieldLabel.setFont(resized);
        videoFileField.setFont(resized);
        button.setFont(resized);
        resultLabel.setFont(resized);
    }

    protected void setHTMLError(String s) {
        setHTMLErrorText(s);
        setHTMLErrorVisible(true);
        irisFileField.getAccessibleContext().setAccessibleDescription(s + " , Path to IRIS file");
    }

    protected void setPDFError(String s) {
        setPDFErrorText(s);
        setPDFErrorVisible(true);
        pdfFileField.getAccessibleContext().setAccessibleDescription(s + " , Path to PDF file");
    }

    protected void setVideoError(String s) {
        setVideoErrorText(s);
        // setViErrorVisible(true);
        videoFileFieldErrorLabel.setVisible(true);
        videoFileField.getAccessibleContext().setAccessibleDescription(s + " , Path to Video file");
    }

    protected void setResultError(String s) {
        resultLabel.setText(s);
        setResultVisible(true);
        setResultColor(Color.RED);
    }

    protected void setHTMLErrorText(String s) {
        irisFileFieldErrorLabel.setText(s);
    }

    protected void setPDFErrorText(String s) {
        pdfFileFieldErrorLabel.setText(s);
    }

    protected void setVideoErrorText(String s) {
        videoFileFieldErrorLabel.setText(s);
    }


    protected void setHTMLErrorVisible(Boolean b) {
        irisFileFieldErrorLabel.setVisible(b);
    }

    protected void setPDFErrorVisible(Boolean b) {
        pdfFileFieldErrorLabel.setVisible(b);
    }

    protected void setResultText(String s) {
        resultLabel.setText(s);
    }

    protected void setResultVisible(Boolean b) {
        resultLabel.setVisible(b);
    }

    protected void setResultColor(Color c) {
        resultLabel.setForeground(c);
    }

    protected String getHTMLFileText() {
        return irisFileField.getText();
    }

    protected String getPDFFileText() {
        return pdfFileField.getText();
    }

    protected String getVideoFileText() {
        return videoFileField.getText();
    }

    protected void setHTMLFileText(String s) {
        irisFileField.setText(s);
    }

    protected void setPDFFileText(String s) {
        pdfFileField.setText(s);
    }

    protected void setVideoFileText(String s) {
        videoFileField.setText(s);
    }

    protected boolean isHTMLErrorVisible() {
        return irisFileFieldErrorLabel.isVisible();
    }

    protected boolean isPDFErrorVisible() {
        return pdfFileFieldErrorLabel.isVisible();
    }

    protected void grabirisFileFieldFocus() {
        irisFileField.grabFocus();
    }

    protected void grabPDFFileFieldFocus() {
        pdfFileField.grabFocus();
    }

    protected void setButtonVisible(boolean b) {
        button.setVisible(b);
    }

    protected void resetView() {
        setHTMLErrorVisible(false);
        setPDFErrorVisible(false);

        setResultColor(Color.BLACK);
        setResultVisible(false);
    }

    protected void showRunning() {
        setHTMLFileText("");
        setPDFFileText("");
        setVideoFileText("");
        setButtonVisible(false);
    }

    protected void showSuccess(String pdfFileName) {
        setButtonVisible(true);
        setResultColor(Color.BLACK);
        setResultVisible(true);
        setResultText("Successfully generated PDF, can be found at" + pdfFileName);
        irisFileField.setText(irisFieldHint);
        videoFileField.setText(videoFieldHint);
        pdfFileField.setText(pdfFieldHint);
        pdfFileField.setForeground(Color.LIGHT_GRAY);
    }

    protected void resetWithValsAndError(String iris, String video, String pdf, String error) {
        setButtonVisible(true);
        irisFileField.setText(iris);
        videoFileField.setText(video);
        pdfFileField.setText(pdf);
        setHTMLError(error);
    }

    protected void resetWithVals(String iris, String video, String pdf) {
        setButtonVisible(true);
        irisFileField.setText(iris);
        videoFileField.setText(video);
        pdfFileField.setText(pdf);
    }

    void resetErrors() {
        irisFileFieldErrorLabel.setVisible(false);
        pdfFileFieldErrorLabel.setVisible(false);
        videoFileFieldErrorLabel.setVisible(false);
        irisFileFieldErrorLabel.setText("");
        pdfFileFieldErrorLabel.setText("");
        videoFileFieldErrorLabel.setText("");
    }
}
