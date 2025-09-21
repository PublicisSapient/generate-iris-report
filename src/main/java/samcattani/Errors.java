package samcattani;

import java.io.File;

public class Errors {

    static boolean checkGUIErrors(String irisFile, String videoFile, String pdfFile, String htmlFile, GUI gui) {
        return checkEmptyAndExists(irisFile, "IRIS file", gui) && checkEmptyAndExists(videoFile, "video file", gui) && (checkEmpty(pdfFile, "PDF Name", gui) || checkEmpty(htmlFile, "HTML Name", gui));
    }

    static boolean checkCLErrors(String irisFile, String videoFile, String pdfFile, String htmlFile) {
        return checkEmptyAndExists(irisFile, "IRIS file") && checkEmptyAndExists(videoFile, "video file") && (checkEmpty(pdfFile, "PDF Name") || checkEmpty(htmlFile, "HTML Name"));
    }

    static boolean checkEmptyAndExists(String s, String fieldName) {
        if (s.isEmpty()) {
            System.out.println("You must provide value for " + fieldName);
            return false;
        } else if (!(new File(s).isFile())) {
            System.out.println(fieldName + " is not a file. Please ensure that the file exists and the path is correct.");
            return false;
        }

        return true;
    }

    static boolean checkEmpty(String s, String fieldName) {
        if (s.isEmpty()) {
            System.out.println("There is no value for " + fieldName);
            return false;
        }

        return true;
    }

    static boolean checkEmptyAndExists(String s, String fieldName, GUI gui) {
        String error = "";

        if (s.isEmpty()) {
            error = "You must provide value for " + fieldName;
        } else if (!(new File(s).isFile())) {
            error = fieldName + " is not a file. Please ensure that the file exists and the path is correct.";
        }

        if (fieldName.equals("IRIS file")) {
            gui.setHTMLError(error);
        } else {
            gui.setVideoError(error);
        }

        return error.isEmpty();
    }

    static boolean checkEmpty(String s, String fieldName, GUI gui) {
        if (s.isEmpty()) {
            gui.setPDFError("You must provide value for " + fieldName);
            return false;
        }

        return true;
    }

}
