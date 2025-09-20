package samcattani;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import org.apache.commons.io.FilenameUtils;

import com.opencsv.CSVReader;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

public class App {
    private static final String bodyFontName = "OpenSans-VariableFont_wdth,wght.ttf";
    private static final String codeFontName = "SourceCodePro-VariableFont_wght.ttf";
    private static File bodyFontFile = new File(bodyFontName);
    private static File codeFontFile = new File(codeFontName);
    private static GUI gui;
    private static int lumFailures = 0, patternFailures = 0, redFailures = 0, lumWarnings = 0, redWarnings = 0;
    static ArrayList<ArrayList<Row>> defects;
    static HashMap<String, String> reportData = new HashMap<>();
    static Snapshotter snapshotter;
    static boolean useGui = true;
    static String irisPath = "";
    static String videoPath = "";
    static String pdfPath = "";
    static GraphData graphData;
    static String graphImage = "";
    static int index = 0, lumIndex = 0, redIndex = 0, patternIndex = 0, timeStampIndex = 0, avLumIndex = 0,
            avRedIndex = 0,
            lumFlashIndex = 0, redFlashIndex = 0;

    public static void main(String[] args) throws Exception {
        handleArgs(args);

        if (useGui) {
            gui = new GUI(() -> generateReportAction(), irisPath, videoPath, pdfPath);
        } else {
            if (Errors.checkCLErrors(irisPath, videoPath, pdfPath)) {
                generateReport();
            } else {

            }
        }
    }

    private static void generateReportAction() {
        try {
            gui.resetErrors();

            irisPath = gui.getHTMLFileText();
            pdfPath = gui.getPDFFileText();
            videoPath = gui.getVideoFileText();

            if (Errors.checkGUIErrors(irisPath, videoPath, pdfPath, gui)) {
                gui.resetWithVals(irisPath, videoPath, pdfPath);
                return;
            }

            gui.resetView();

            gui.showRunning();
            generateReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleArgs(String[] args) {
        for (String arg : args) {
            if (!arg.contains("=")) {
                continue;
            }
            switch (arg.substring(0, arg.indexOf("="))) {
                case "useGui":
                    useGui = Boolean.parseBoolean(arg.substring(arg.indexOf("=") + 1, arg.length()));
                    break;
                case "videoPath":
                    videoPath = arg.substring(arg.indexOf("=") + 1, arg.length());
                    break;
                case "irisPath":
                    irisPath = arg.substring(arg.indexOf("=") + 1, arg.length());
                    break;
                case "pdfName":
                    pdfPath = arg.substring(arg.indexOf("=") + 1, arg.length());
                    break;
                default:
                    System.out.println(
                            "Unrecognized arg: " + arg.substring(0, arg.indexOf("=")) + ". Ignoring this arg.");
                    break;
            }
        }

        if (!useGui && (pdfPath.isEmpty() || irisPath.isEmpty() || videoPath.isEmpty())) {
            getArgsNoGui();
        }
    }

    private static void getArgsNoGui() {
        Scanner scanner = new Scanner(System.in);

        if (irisPath.isEmpty()) {
            System.out.println("Enter path to IRIS executable");
            irisPath = scanner.nextLine();
        }

        if (videoPath.isEmpty()) {
            System.out.println("Enter path to video");
            videoPath = scanner.nextLine();
        }

        if (pdfPath.isEmpty()) {
            System.out.println("Enter file to save PDF");
            pdfPath = scanner.nextLine();
        }

        scanner.close();

    }

    private static void generateReport() {
        // try {
        String irisPathOnly = Paths.get(irisPath).getParent().toString();

        String videoName = "";

        try {
            videoName = copyVideoToIris(videoPath, irisPathOnly);
        } catch (IOException e) {
            showError("There was an issue copying the video to the TestVideos folder");
            e.printStackTrace();
            return;
        }

        if (!useGui) {
            System.out.println("Running IRIS on video file");
        }

        try {
        runIRIS(irisPathOnly, irisPath.substring(irisPath.lastIndexOf("/") + 1,
        irisPath.length()));
        } catch (IOException | InterruptedException e) {
        showError("There was an error running the IRIS program");
        e.printStackTrace();
        return;
        }

        try {
            snapshotter = new Snapshotter((Paths.get(irisPathOnly, "TestVideos", videoName)).toString());
        } catch (org.bytedeco.javacv.FFmpegFrameGrabber.Exception e) {
            showError("There was an issue setting up the image capture tool.");
            e.printStackTrace();
            return;
        }

        defects = new ArrayList<ArrayList<Row>>();
        if (!useGui) {
            System.out.println("Parsing IRIS data");
        }

        try {
            setRows(irisPathOnly, videoName, defects);
        } catch (IOException e) {
            showError("There was an issue reading the results.");
            e.printStackTrace();
            return;
        }

        if (!useGui) {
            System.out.println("Making graph");
        }

        synchronized (graphImage) {
            // do some code here
            Graph g = new Graph(graphData);
            graphImage = g.saveAsImage();
        }

        setFirstPageData(videoName);

        if (!useGui) {
            System.out.println("Generating PDF");
        }

        try {
            generatePDF(getHTMLFromTemplate());
        } catch (IOException e) {
            showError("There was an issue generating the PDF.");
            e.printStackTrace();
            return;
        }

        snapshotter.stop();

        if (!useGui) {
            System.out.println("Report generated! Find report at " + pdfPath);
        } else {
            gui.showSuccess(pdfPath);
        }

        openFile(pdfPath);
    }

    private static void showError(String message) {
        if (!useGui) {
            System.out.println(message);
        } else {
            gui.resetWithValsAndError(irisPath, videoPath, pdfPath, message);
        }
    }

    private static void runIRIS(String irisPath, String irisFileExName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(irisPath));
        processBuilder.command("./" + irisFileExName);
        Process p = processBuilder.start();
        p.waitFor();
    }

    private static String copyVideoToIris(String videoFileName, String irisPath) throws IOException {
        if (!useGui) {
            System.out.println("Copying video to IRIS folder");
        }

        Path src = Paths.get(videoFileName);
        String name = src.getFileName().toString();

        // ../IRIS/bin/build/<os>-release/example/TestVideos
        Path testVideosDir = Paths.get(irisPath, "TestVideos");
        Files.createDirectories(testVideosDir);

        Path dst = testVideosDir.resolve(name);

        // Copy (overwrite if an old copy exists)
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        // return just the filename; callers join it with TestVideos later
        return name;
    }

    private static void setRows(String irisPath, String videoName, ArrayList<ArrayList<Row>> defects)
            throws IOException, FileNotFoundException {
        String resultsPath = Paths.get(irisPath, "Results", videoName).toString();
        File f = getLastModified(resultsPath);

        System.out.println(f.getAbsolutePath());

        if (!useGui) {
            System.out.println("Building IRIS report");
        }

        FileReader filereader = new FileReader(f);
        CSVReader csvReader = new CSVReader(filereader);
        String[] nextRecord;
        ArrayList<Row> rows = new ArrayList<Row>();
        boolean lastFailure = false, init = true;
        graphData = new GraphData();

        // we are going to read data line by line
        while ((nextRecord = csvReader.readNext()) != null) {
            Row row = new Row();
            StringBuilder redFlashVal = new StringBuilder(""), redDiffVal = new StringBuilder(""),
                    lumDiffVal = new StringBuilder(""), lumFlashVal = new StringBuilder("");

            for (String cell : nextRecord) {
                cell = cell.trim();
                if (init) {
                    setIndex(cell);
                } else {
                    parseCell(cell, row, redFlashVal, redDiffVal, lumDiffVal, lumFlashVal);
                }
                index++;
            }

            if (!init) {
                graphData.addRedVal(redDiffVal.toString(), redFlashVal.toString());
                graphData.addLumVal(lumDiffVal.toString(), lumFlashVal.toString());
                graphData.addTimeStamp(row.getTimeStamp());

                if (!row.passes()) {
                    if (lastFailure) {
                        if (rows.size() % 4 == 0) {
                            row.setImage(snapshotter.grabImage(row.timeStamp), false);
                        }
                        rows.add(row);
                    } else {
                        rows = new ArrayList<Row>();
                        defects.add(rows);
                        row.setImage(snapshotter.grabImage(row.timeStamp), false);
                        rows.add(row);
                        lastFailure = true;
                    }
                } else {
                    lastFailure = false;
                }
            }

            init = false;
            index = 0;
        }

        addImageRowBuffer();

        csvReader.close();
    }

    private static void setIndex(String cell) {
        switch (cell) {
            case "TimeStamp":
                timeStampIndex = index;
                break;
            case "LuminanceFrameResult":
                lumIndex = index;
                break;
            case "RedFrameResult":
                redIndex = index;
                break;
            case "PatternFrameResult":
                patternIndex = index;
                break;
            case "AverageLuminanceDiffAcc":
                avLumIndex = index;
                break;
            case "AverageRedDiffAcc":
                avRedIndex = index;
                break;
            case "FlashAreaLuminance":
                lumFlashIndex = index;
                break;
            case "FlashAreaRed":
                redFlashIndex = index;
                break;
        }
    }

    private static void parseCell(String cell, Row row, StringBuilder redFlashVal, StringBuilder redDiffVal,
            StringBuilder lumDiffVal, StringBuilder lumFlashVal) {
        if (index == timeStampIndex) {
            row.setTimeStamp(cell);
        } else if (index == lumIndex) {
            row.setLuminanceStringResult(cell);
            if (Integer.parseInt(cell) == 1) {
                lumWarnings++;
            } else if (Integer.parseInt(cell) != 0) {
                lumFailures++;
            }
        } else if (index == patternIndex) {
            row.setPatternFrameResult(cell);
            if (Integer.parseInt(cell) != 0) {
                patternFailures++;
            }
        } else if (index == redIndex) {
            row.setRedFrameResult(cell);
            if (Integer.parseInt(cell) == 1) {
                redWarnings++;
            } else if (Integer.parseInt(cell) != 0) {
                redFailures++;
            }
        } else if (index == avLumIndex) {
            lumDiffVal.append(cell);
        } else if (index == avRedIndex) {
            redDiffVal.append(cell);
        } else if (index == redFlashIndex) {
            redFlashVal.append(cell);
        } else if (index == lumFlashIndex) {
            lumFlashVal.append(cell);
        }

    }

    private static void addImageRowBuffer() {
        for (ArrayList<Row> defect : defects) {
            int numExtraRows = 0;

            for (int i = 1; i <= 4; i++) {
                if (defect.get(defect.size() - i).hasScreenshot) {
                    numExtraRows = 4 - i;
                    break;
                }
            }

            for (int i = 0; i < numExtraRows; i++) {
                defect.add(new Row());
            }
        }
    }

    private static File getLastModified(String directoryFilePath) {
        // https://stackoverflow.com/questions/285955/java-get-the-newest-file-in-a-directory
        File directory = new File(directoryFilePath);
        File[] files = directory.listFiles(File::isFile);
        long lastModifiedTime = Long.MIN_VALUE;
        File chosenFile = null;

        if (files != null) {
            for (File file : files) {
                if (file.lastModified() > lastModifiedTime
                        && FilenameUtils.getExtension(file.getPath().toString()).equals("csv")) {
                    chosenFile = file;
                    lastModifiedTime = file.lastModified();
                }
            }
        }

        return chosenFile;
    }

    private static void setFirstPageData(String videoName) {
        String failed = lumFailures > 0 || patternFailures > 0 || redFailures > 0 ? "Failed"
                : lumWarnings > 0 || redWarnings > 0 ? "Warning" : "Passed";

        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' h:mma");
        String formattedDate = myDateObj.format(myFormatObj);

        reportData.put("videoName", videoName);
        reportData.put("framesPerSec", snapshotter.getFramesPerSec());
        reportData.put("time", snapshotter.getVideoLength());
        reportData.put("totalFrames", snapshotter.getTotalFrames());
        reportData.put("formattedDate", formattedDate);
        reportData.put("failed", failed);
        reportData.put("lumFailures", "" + lumFailures);
        reportData.put("redFailures", "" + redFailures);
        reportData.put("patternFailures", "" + patternFailures);
    }

    private static String getHTMLFromTemplate() {
        if (!useGui) {
            System.out.println("Formatting IRIS report");
        }
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver(Thread
                .currentThread().getContextClassLoader());
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        templateResolver.setOrder(1);

        Context context = new Context();
        for (String s : reportData.keySet()) {
            context.setVariable(s, reportData.getOrDefault(s, ""));
        }
        context.setVariable("defects", defects);
        context.setVariable("imageWidth", snapshotter.getImageWidth());
        String s = "file:///" + System.getProperty("user.dir").replaceAll(" ", "%20") + "/image.png";
        context.setVariable("graph", Paths.get(s).normalize().toString());
        context.setVariable(bodyFontName, context);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine.process("template", context);
    }

    private static void generatePDF(String html) throws IOException, FileNotFoundException {
        convertFontResourcesToFiles();

        if (!useGui) {
            System.out.println("Building PDF");
        }

        FileOutputStream os = new FileOutputStream(pdfPath);
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.usePdfUaAccessbility(true);
        builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_3_U);
        builder.useFont(bodyFontFile, "BodyFont");
        builder.useFont(codeFontFile, "CodeFont");
        builder.withHtmlContent(html, "");
        builder.toStream(os);
        builder.run();

        bodyFontFile.delete();
        codeFontFile.delete();
    }

    private static void convertFontResourcesToFiles() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        copyInputStreamToFile(classloader.getResourceAsStream(bodyFontName), bodyFontFile);
        copyInputStreamToFile(classloader.getResourceAsStream(codeFontName), codeFontFile);
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        // https://mkyong.com/java/how-to-convert-inputstream-to-file-in-java/
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[8192];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    private static void openFile(String pdfFileName) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        Desktop desktop = Desktop.getDesktop();

        // let's try to open PDF file
        File file = new File(pdfFileName);
        if (file.exists()) {
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
