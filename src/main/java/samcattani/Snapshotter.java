package samcattani;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

public class Snapshotter {
    FFmpegFrameGrabber frameGrabber;
    Java2DFrameConverter converter;
    ArrayList<String> photosToDelete;
    int imageWidth = 142;

    // Where we put images (default: ./tmp under current working dir)
    private final Path outputDir;

    public Snapshotter(String videoName) throws FFmpegFrameGrabber.Exception {
        this.outputDir = defaultOutputDir();
        ensureDir(outputDir);
        frameGrabber = new FFmpegFrameGrabber(videoName);
        start(videoName);
    }

    void start(String videoName) {
        converter = new Java2DFrameConverter();
        photosToDelete = new ArrayList<>();
        try {
            frameGrabber.start();
            frameGrabber.grab(); // prime
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    void stop() {
        // delete only real filesystem paths we created (not "file://..." URIs)
        for (String s : photosToDelete) {
            try {
                Files.deleteIfExists(Paths.get(s));
            } catch (Exception ignore) {}
        }
        try { frameGrabber.stop(); } catch (FFmpegFrameGrabber.Exception e) { e.printStackTrace(); }
        try { frameGrabber.close(); } catch (FrameGrabber.Exception e) { e.printStackTrace(); }
        if (converter != null) converter.close();
    }

    private static BufferedImage scaleToWidth(BufferedImage src, int targetW) {
        if (src == null) return null;
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW <= 0 || srcH <= 0) return src;

        if (srcW == targetW) return src; // nothing to do

        double scale = targetW / (double) srcW;
        int targetH = Math.max(1, (int) Math.round(srcH * scale));

        // Use a compatible type; fall back to ARGB if unknown
        int type = src.getType();
        if (type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        BufferedImage dst = new BufferedImage(targetW, targetH, type);
        java.awt.Graphics2D g2 = dst.createGraphics();
        // Good quality scaling
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(src, 0, 0, targetW, targetH, null);
        g2.dispose();
        return dst;
    }

    String grabImage(String timeStamp) {
        // Parse HH:MM:SS[.fraction] to microseconds and seek by timestamp
        long micros = parseTimestampToMicros(timeStamp);
        Frame frame = null;
        try {
            // Seeking by timestamp is more robust than by frame number across VFR files
            frameGrabber.setTimestamp(micros);
            frame = frameGrabber.grabImage();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }

        if (frame != null && frame.image != null) {
            BufferedImage bi = converter.convert(frame);
            try {
                String safeStamp = sanitizeForWindows(timeStamp);
                Path out = outputDir.resolve("img" + safeStamp + ".jpg");
                BufferedImage scaled = scaleToWidth(bi, 300);
                ImageIO.write(scaled, "jpg", out.toFile());

                // compute width once based on first grabbed frame, preserving your logic
                if (imageWidth == 142) {
                    imageWidth = (int) Math.round(80.0 * ((double) bi.getWidth() / (double) bi.getHeight()));
                }

                photosToDelete.add(out.toString());
                // Return a proper file:// URL (works on Windows, macOS, Linux)
                return out.toUri().toString();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // If we got here, something failed; return an empty string to keep caller contract
        return "";
    }

    String getFramesPerSec() {
        return Math.round(frameGrabber.getVideoFrameRate()) + "";
    }

    String getTotalFrames() {
        return "" + frameGrabber.getLengthInVideoFrames();
    }

    String getVideoLength() {
        // More reliable: compute from total frames & fps already used by your code
        int totalFrames = frameGrabber.getLengthInVideoFrames();
        double fps = Math.max(1.0, frameGrabber.getVideoFrameRate());
        int seconds = (int) Math.floor(totalFrames / fps);

        int hours = 0, mins = 0;
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds = seconds % 3600;
        }
        if (seconds >= 60) {
            mins = seconds / 60;        // FIX: use seconds, not mins
            seconds = seconds % 60;
        }

        // leftover frames -> fractional seconds
        int leftover = (int) Math.floor(totalFrames % fps);
        double frac = leftover / fps;
        DecimalFormat df = new DecimalFormat(".###");

        return ((hours < 10) ? "0" + hours : "" + hours) + ":" +
               ((mins  < 10) ? "0" + mins  : "" + mins ) + ":" +
               ((seconds < 10) ? "0" + seconds : "" + seconds) + df.format(frac);
    }

    String getImageWidth() {
        return "" + imageWidth + "px";
    }

    // --------------------
    // Helpers (portable)
    // --------------------
    private static Path defaultOutputDir() {
        // Allow overrides via system property / env, else ./tmp
        String prop = System.getProperty("gir.tmpDir");
        if (prop != null && !prop.isBlank()) return Paths.get(prop).toAbsolutePath().normalize();
        String env = System.getenv("GIR_TMP_DIR");
        if (env != null && !env.isBlank()) return Paths.get(env).toAbsolutePath().normalize();
        return Paths.get(System.getProperty("user.dir"), "tmp").toAbsolutePath().normalize();
    }

    private static void ensureDir(Path p) {
        try { Files.createDirectories(p); } catch (IOException e) { throw new RuntimeException(e); }
    }

    // Accepts "HH:MM:SS" or "HH:MM:SS.fraction" (any number of fraction digits)
    private static long parseTimestampToMicros(String ts) {
        try {
            int h = Integer.parseInt(ts.substring(0, 2));
            int m = Integer.parseInt(ts.substring(3, 5));
            int s = Integer.parseInt(ts.substring(6, 8));
            long base = ((h * 3600L) + (m * 60L) + s) * 1_000_000L;

            long frac = 0L;
            if (ts.length() > 8) {
                // grab everything after the first non-digit following SS
                String tail = ts.substring(8);               // e.g. ".123456" or ":123456" in some inputs
                String digits = tail.replaceAll("[^0-9]", ""); // keep only digits
                if (!digits.isEmpty()) {
                    // scale to microseconds based on digit count
                    int n = digits.length();
                    long val = Long.parseLong(digits);
                    // convert fractional seconds digits to micros (e.g., "123" -> 123_000 if millis)
                    if (n >= 6) frac = Long.parseLong(digits.substring(0, 6));
                    else {
                        frac = (long) Math.round(val * Math.pow(10, 6 - n));
                    }
                }
            }
            return base + frac;
        } catch (Exception e) {
            // fall back to 0 if parse fails
            return 0L;
        }
    }

    // Windows-safe file/folder name (also fine on Unix)
    private static final Pattern INVALID = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static String sanitizeForWindows(String s) {
        // Replace invalid chars (like ':' in timestamps) with '-'
        String x = INVALID.matcher(s).replaceAll("-").replaceAll("[. ]+$", "");
        if (x.isEmpty()) x = "untitled";
        // Avoid reserved device names
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
