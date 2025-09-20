package samcattani;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.regex.Pattern;

public class Snapshotter {
    private final Path videoPath;
    private final Path outputRoot;      // e.g., <repo>/tmp
    private final Path thumbnailsDir;   // e.g., <repo>/tmp/thumbnails/<video-base>

    // --- Back-compat constructor (keeps current call sites working) ---
    public Snapshotter(String videoPathStr) throws IOException {
        this(Paths.get(videoPathStr), defaultOutputRoot());
    }

    // --- New, explicit constructor (lets callers pick the output dir) ---
    public Snapshotter(Path videoPath, Path outputRoot) throws IOException {
        this.videoPath = videoPath.toAbsolutePath().normalize();
        this.outputRoot = outputRoot.toAbsolutePath().normalize();

        String base = stripExtension(this.videoPath.getFileName().toString());
        String safeBase = sanitizeForWindows(base);
        this.thumbnailsDir = this.outputRoot.resolve(Paths.get("thumbnails", safeBase));

        Files.createDirectories(this.thumbnailsDir);
    }

    // Grab N evenly spaced snapshots across the video. Adjust as needed.
    public void generateSnapshots(int count) throws Exception {
        try (FFmpegFrameGrabber g = new FFmpegFrameGrabber(videoPath.toString());
             Java2DFrameConverter conv = new Java2DFrameConverter()) {

            g.start();

            long frames = g.getLengthInFrames();
            if (frames <= 0) frames = Math.max(1, Math.round(g.getLengthInTime() / Math.max(1, g.getFrameRate())));

            long step = Math.max(1, frames / Math.max(1, count));

            for (long i = 0, grabbed = 0; i < frames && grabbed < count; i += step, grabbed++) {
                g.setFrameNumber(Math.toIntExact(i));
                Frame f = g.grabImage();
                if (f == null) continue;

                BufferedImage bi = conv.convert(f);
                if (bi == null) continue;

                Path out = thumbnailsDir.resolve(String.format(Locale.ROOT, "thumb-%06d.png", grabbed + 1));
                ImageIO.write(bi, "png", out.toFile());
            }

            g.stop(); // try-with-resources closes as well; explicit stop keeps JavaCV happy
        }
    }

    public Path getThumbnailsDir() { return thumbnailsDir; }
    public Path getOutputRoot() { return outputRoot; }

    // ------------------------
    // Helpers (cross-platform)
    // ------------------------
    private static Path defaultOutputRoot() {
        String prop = System.getProperty("gir.tmpDir");
        if (prop != null && !prop.isBlank()) return Paths.get(prop);
        String env = System.getenv("GIR_TMP_DIR");
        if (env != null && !env.isBlank()) return Paths.get(env);
        // default: <current working directory>/tmp
        return Paths.get(System.getProperty("user.dir"), "tmp");
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot <= 0) ? name : name.substring(0, dot);
    }

    // Windows-safe file/folder name (also fine on Linux/macOS)
    private static final Pattern INVALID = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static String sanitizeForWindows(String s) {
        String x = INVALID.matcher(s).replaceAll("_").replaceAll("[. ]+$", "");
        if (x.isEmpty()) x = "untitled";
        String upper = x.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "CON": case "PRN": case "AUX": case "NUL":
            case "COM1": case "COM2": case "COM3": case "COM4":
            case "COM5": case "COM6": case "COM7": case "COM8": case "COM9":
            case "LPT1": case "LPT2": case "LPT3": case "LPT4":
            case "LPT5": case "LPT6": case "LPT7": case "LPT8": case "LPT9":
                return "_" + x;
            default:
                return x;
        }
    }
}
