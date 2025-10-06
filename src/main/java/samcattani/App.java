package samcattani;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;

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
    static int screenshotEvery = 1;  // default: take a screenshot on every row
    static ArrayList<ArrayList<Row>> defects;
    static HashMap<String, String> reportData = new HashMap<>();
    static Snapshotter snapshotter;
    static boolean useGui = true;
    static String irisPath = "";
    static String videoPath = "";
    static String pdfPath = "";
    static String htmlPath = "";
    static boolean useLastCSV = false;
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
            if (Errors.checkCLErrors(irisPath, videoPath, pdfPath, htmlPath)) {
                if (checkCliArgs()) {
                    generateReport();
                }
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

            if (Errors.checkGUIErrors(irisPath, videoPath, pdfPath, htmlPath, gui)) {
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
            int eq = arg.indexOf('=');
            if (eq <= 0) continue;
            String key = arg.substring(0, eq).trim();
            String val = arg.substring(eq + 1).trim();

            switch (key) {
                case "useGui":
                    useGui = Boolean.parseBoolean(val);
                    break;
                case "videoPath":
                    videoPath = val;
                    break;
                case "irisPath":
                    irisPath = val;
                    break;
                case "pdfName":
                    pdfPath = val;
                    break;
                case "htmlName":
                    htmlPath = val;
                    break;
                case "useLastCSV":
                    useLastCSV = Boolean.parseBoolean(val);
                    break;
                case "screenshotEvery":
                    try {
                        int n = Integer.parseInt(val);
                        screenshotEvery = (n < 1) ? 1 : n; // guard: min 1
                    } catch (NumberFormatException ignore) {
                        screenshotEvery = 1;
                    }
                    break;
                default:
                    System.out.println(
                            "Unrecognized arg: " + key + ". Ignoring this arg.");
                    break;
            }
        }

        if (!useGui && (irisPath.isEmpty() || videoPath.isEmpty() || (pdfPath.isEmpty() && htmlPath.isEmpty()))) {
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

        if (pdfPath.isEmpty() && htmlPath.isEmpty()) {
            System.out.println("Enter file to save PDF (leave blank if you only want HTML):");
            pdfPath = scanner.nextLine();
            if (pdfPath.isEmpty()) {
                System.out.println("Enter file to save HTML:");
                htmlPath = scanner.nextLine();
            }
        }

        scanner.close();

    }

    private static boolean checkCliArgs() {
        boolean ok = Errors.checkEmptyAndExists(irisPath, "IRIS file")
                && (useLastCSV || Errors.checkEmptyAndExists(videoPath, "video file"));
        if (!ok) {
            return false;
        }
        if (pdfPath.isEmpty() && htmlPath.isEmpty()) {
            System.out.println("You must provide at least one output: pdfName or htmlName");
            return false;
        }
        return true;
    }

    private static void generateReport() {
        // try {
        String irisPathOnly = Paths.get(irisPath).getParent().toString();

        String videoName = "";
        String html;

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

        if (!useLastCSV) {
            try {
                runIRIS(irisPathOnly, irisPath.substring(irisPath.lastIndexOf("/") + 1,
                        irisPath.length()));
            } catch (IOException | InterruptedException e) {
                showError("There was an error running the IRIS program");
                e.printStackTrace();
                return;
            }
        }

        System.out.println("IRIS run completed. Grabbing snapshots");

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
            Graph g = new Graph(graphData);
            String lumPng = g.saveAsImage(Graph.Series.LUMINANCE);
            String redPng = g.saveAsImage(Graph.Series.RED);
        }

        setFirstPageData(videoName);

        boolean wantHtml = (htmlPath != null && !htmlPath.isEmpty());
        boolean wantPdf = (pdfPath != null && !pdfPath.isEmpty());
        if (wantHtml && !useGui) {
            System.out.println("Generating HTML");
        }

        String htmlForPdf  = null;
        String htmlForHtml = null;

        if (wantPdf) {
            htmlForPdf = getHTMLFromTemplate(false);           // keep file: URLs for PDF
        }

        if (wantHtml) {
            // Temporarily inline row images, render template, then restore
            var backups = inlineRowImagesForHtml();
            try {
                htmlForHtml = getHTMLFromTemplate(true);       // data: URLs for HTML
            } finally {
                restoreRowImages(backups);
            }
        }


        if (wantHtml) {
            try {
                generateHTML(htmlForHtml);
            } catch (IOException e) {
                showError("There was an issue generating the HTML file.");
                e.printStackTrace();
                return;
            }
        }

        if (wantPdf && !useGui) {
            System.out.println("Generating PDF");
        }
        if (wantPdf) {
            try {
                generatePDF(htmlForPdf);
            } catch (IOException e) {
                showError("There was an issue generating the PDF file.");
                e.printStackTrace();
                return;
            }
        }

        snapshotter.stop();

        if (!useGui) {
            if (wantPdf) {
                System.out.println("PDF report:  " + pdfPath);
            }
            if (wantHtml) {
                System.out.println("HTML report: " + htmlPath);
            }
        } else if (wantPdf) {
            gui.showSuccess(pdfPath);
        }

        // Try to open something helpful
        if (wantPdf) {
            openFile(pdfPath);
        } else if (wantHtml) {
            openFile(htmlPath);
        }

        // Remove the copied video from TestVideos
        deleteCopiedVideoQuietly(irisPathOnly, videoName);

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
        System.out.println("Running IRIS Command: " + irisFileExName);
        System.out.println("IRIS Path: " + irisPath);
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

    private static void deleteCopiedVideoQuietly(String irisPathOnly, String videoName) {
        if (videoName == null || videoName.isEmpty()) return;
        try {
            Path p = Paths.get(irisPathOnly, "TestVideos", videoName);
            Files.deleteIfExists(p);
            if (!useGui) {
                System.out.println("Deleted copied video: " + p);
            }
        } catch (IOException e) {
            // Non-fatal: warn and continue
            if (!useGui) {
                System.err.println("Warning: couldn't delete copied video: " + e.getMessage());
            }
        }
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
                        if (rows.size() % screenshotEvery == 0) {
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

            for (int i = 1; i <= screenshotEvery; i++) {
                if (defect.get(defect.size() - i).hasScreenshot) {
                    numExtraRows = screenshotEvery - i;
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
        reportData.put("screenshotEvery", "" + screenshotEvery);
    }

    private static String getHTMLFromTemplate(boolean inlineImages) {
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
        context.setVariable("screenshotEvery", screenshotEvery);
        
        // Tell the template which “mode” we’re rendering
        context.setVariable("docClass", inlineImages ? "isWebPage" : "isPDF");

        Path graphPng = Paths.get(System.getProperty("user.dir"), "image.png");
        if (inlineImages) {
            try {
                context.setVariable("graphLum", fileToDataUri(lumPng, "image/png"));
                context.setVariable("graphRed", fileToDataUri(redPng, "image/png"));
            } catch (IOException e) {
                // If inlining fails, fall back to file URL
                String s = "file:///" + System.getProperty("user.dir").replaceAll(" ", "%20") + "/image.png";
                context.setVariable("graph", Paths.get(s).normalize().toString());
            }
        } else {
            String s = "file:///" + System.getProperty("user.dir").replaceAll(" ", "%20") + "/image.png";
            context.setVariable("graph", Paths.get(s).normalize().toString());
        }
        context.setVariable(bodyFontName, context);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine.process("template", context);
    }

    private static void generateHTML(String html) throws IOException {
        Path out = Paths.get(htmlPath).toAbsolutePath();
        Files.createDirectories(out.getParent());
        Files.writeString(out, html, StandardCharsets.UTF_8);
        // copy webfonts next to the HTML so browsers can load them
        writeFontsAlongsideHTML(out.getParent());
    }

    private static void writeFontsAlongsideHTML(Path dir) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        copyInputStreamToFile(cl.getResourceAsStream(bodyFontName), dir.resolve(bodyFontName).toFile());
        copyInputStreamToFile(cl.getResourceAsStream(codeFontName), dir.resolve(codeFontName).toFile());
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

    private static void openFile(String pathStr) {
        if (pathStr == null || pathStr.isEmpty()) return;
        openFile(Paths.get(pathStr));
    }

    private static void openFile(Path path) {
        if (path == null) return;
        try {
            if (!Files.exists(path)) {
                if (!useGui) System.err.println("openFile: not found: " + path);
                return;
            }

            // 1) Try Desktop API first
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop d = Desktop.getDesktop();
                    if (d.isSupported(Desktop.Action.OPEN)) {
                        d.open(path.toFile());
                        return;
                    }
                }
            } catch (UnsupportedOperationException | IOException | SecurityException ignored) {
                // fall through to OS-specific commands
            }

            // 2) OS-specific fallback
            final String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            List<List<String>> candidates = new ArrayList<>();

            if (os.contains("mac")) {
                candidates.add(Arrays.asList("open", path.toString()));
            } else if (os.contains("win")) {
                // 'start' needs to run via cmd; empty title "" required when path may contain spaces
                candidates.add(Arrays.asList("cmd", "/c", "start", "", path.toString()));
            } else {
                // Linux/BSD: try common launchers in order
                candidates.add(Arrays.asList("xdg-open", path.toString()));
                candidates.add(Arrays.asList("gio", "open", path.toString()));
                candidates.add(Arrays.asList("kde-open", path.toString()));
                candidates.add(Arrays.asList("gnome-open", path.toString()));
            }

            IOException lastErr = null;
            for (List<String> cmd : candidates) {
                try {
                    new ProcessBuilder(cmd).redirectErrorStream(true).start();
                    return;
                } catch (IOException e) {
                    lastErr = e; // try next
                }
            }
            if (lastErr != null) throw lastErr;

        } catch (IOException e) {
            if (!useGui) {
                System.err.println("openFile: failed to open " + path + " — " + e.getMessage());
            }
        }
    }

    private static String fileToDataUri(Path p, String mime) throws IOException {
        byte[] bytes = Files.readAllBytes(p);
        String b64 = Base64.getEncoder().encodeToString(bytes);
        return "data:" + mime + ";base64," + b64;
    }

    private static Path decodeFileUrlToPath(String maybeFileUrl) {
        try {
            if (maybeFileUrl != null && maybeFileUrl.startsWith("file:")) {
                return Paths.get(URI.create(maybeFileUrl));
            }
        } catch (IllegalArgumentException ignore) {
        }
        // fall back: treat as plain path in CWD
        return Paths.get(maybeFileUrl);
    }

    /**
     * Temporarily replace Row.image file URLs with data URLs. Returns a list of
     * (Row, originalImage) to restore.
     */
    private static List<Object[]> inlineRowImagesForHtml() {
        List<Object[]> backups = new ArrayList<>();
        for (var defect : defects) {
            for (var row : defect) {
                try {
                    // Rows without screenshots usually have null/empty image
                    String img = row.getImage(); // Row has a getter (used by Thymeleaf as row.image)
                    if (img == null || img.isBlank()) {
                        continue;
                    }
                    Path p = decodeFileUrlToPath(img);
                    if (p == null) {
                        continue;
                    }
                    if (!Files.exists(p)) {
                        continue;
                    }
                    String data = fileToDataUri(p, "image/jpg");
                    backups.add(new Object[]{row, img});
                    row.setImage(data, false);
                } catch (Exception ignore) {
                    // If any particular snapshot fails to inline, skip it.
                }
            }
        }
        return backups;
    }

    private static void restoreRowImages(List<Object[]> backups) {
        for (Object[] pair : backups) {
            Row r = (Row) pair[0];
            String original = (String) pair[1];
            try {
                r.setImage(original, false);
            } catch (Exception ignore) {
            }
        }
    }

}
