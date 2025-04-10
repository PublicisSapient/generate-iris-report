package samcattani;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;

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

    Snapshotter(String videoName) throws FFmpegFrameGrabber.Exception {
        frameGrabber = new FFmpegFrameGrabber(videoName);
        start(videoName);
    }

    void start(String videoName) {
        converter = new Java2DFrameConverter();
        photosToDelete = new ArrayList<String>();

        try {
            frameGrabber.start();
            frameGrabber.grab();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    void stop() {
        for (String s : photosToDelete) {
            File f = new File(s);
            f.delete();
        }

        try {
            frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }

        try {
            frameGrabber.close();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }

        converter.close();
    }

    String grabImage(String timeStamp) {
        Integer numHours = Integer.parseInt(timeStamp.substring(0, 2));
        Integer numMins = Integer.parseInt(timeStamp.substring(3, 5));
        Integer numSecs = Integer.parseInt(timeStamp.substring(6, 8));
        double frac  = 0;

        if (timeStamp.length() >= 9) {
            Integer numFractionSec = Integer.parseInt(timeStamp.substring(9, timeStamp.length()));
            frac = (numFractionSec / 1000000.0);
        }
        int fps = (int) frameGrabber.getFrameRate();
        int frameNumber = fps * (numHours * 3600 + numMins * 60 + numSecs)
                + (int) Math.round(frac * fps);

        Frame frame = null;

        try {
            frameGrabber.setFrameNumber(frameNumber);
            frame = frameGrabber.grabImage();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }

        if (frame != null && frame.image != null) {
            BufferedImage bi = converter.convert(frame);
            try {
                ImageIO.write(bi, "png", new File("./img" + timeStamp + ".png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (imageWidth == 142) {
                imageWidth = (int) Math.round(80.0 * ((double) bi.getWidth() / (double) bi.getHeight()));
            }
        }

        String s = "file:///" + System.getProperty("user.dir").replaceAll(" ", "%20") + "/img" + timeStamp
                + ".png";
        String imagePath = Paths.get(s).normalize().toString();
        photosToDelete.add(System.getProperty("user.dir") + "/img" + timeStamp
                + ".png");
        photosToDelete.add(Paths.get(s).normalize().toString());

        return imagePath;
    }

    String getFramesPerSec() {
        return Math.round(frameGrabber.getVideoFrameRate()) + "";
    }

    String getTotalFrames() {
        return "" + frameGrabber.getLengthInVideoFrames();
    }

    String getVideoLength() {
        // seconds of vid:
        int seconds = (int) Math.floor(frameGrabber.getLengthInVideoFrames() / frameGrabber.getVideoFrameRate());
        int hours = 0;
        int mins = 0;

        if (seconds >= 3600) {
            hours = (int) Math.floor(seconds / 3600);
            seconds = seconds % 3600;
        }

        if (seconds >= 60) {
            mins = (int) Math.floor(mins / 60);
            seconds = seconds % 60;
        }

        // leftover, this is ms
        int leftover = (int) Math.floor(frameGrabber.getLengthInVideoFrames() % frameGrabber.getVideoFrameRate());
        double ms = (leftover / frameGrabber.getVideoFrameRate());
        DecimalFormat df = new DecimalFormat(".###");

        return ((hours < 10) ? "0" + hours : "" + hours) + ":" +
                ((mins < 10) ? "0" + mins : "" + mins) + ":"
                + ((seconds < 10) ? "0" + seconds : "" + seconds) + df.format(ms);
    }

    String getImageWidth() {
        return "" + imageWidth + "px";
    }
}
