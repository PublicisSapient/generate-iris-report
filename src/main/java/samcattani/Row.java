package samcattani;

public class Row {
    String luminanceStringResult;
    String redFrameResult;
    String patternFrameResult;
    String timeStamp;
    String image = "none";
    double averageLuminanceResult;
    double averageRedResult;
    boolean hasScreenshot = false;
    boolean hasExtraRow = false;
    String formattedTimeStamp;

    private String[] indexToResult = new String[] {
        "-",
        "Warning",
        "Extended Failure",
        "Failure"
    };

    public Row() {
        this.luminanceStringResult = "-";
        this.redFrameResult = "-";
        this.patternFrameResult = "-";
        this.timeStamp = "-";
        this.image = "none";
        this.averageRedResult = 0;
        this.averageLuminanceResult = 0;
    }

    public Row(int luminanceResult, int redFrameResult, int patternResult, String timeStamp, double averageRedResult, double averageLuminanceResult) {
        this.luminanceStringResult = indexToResult[luminanceResult];
        this.redFrameResult = indexToResult[redFrameResult];
        this.patternFrameResult = patternResult == 0 ? "-" : "Failure";
        this.timeStamp = timeStamp;
        this.image = "none";
        this.averageLuminanceResult = averageLuminanceResult;
        this.averageRedResult = averageRedResult;
    }

    public Row(String luminanceResult, String redFrameResult, String patternResult, String timeStamp, String averageLuminanceResult, String averageRedResult) {
        this.luminanceStringResult = indexToResult[Integer.parseInt(luminanceResult)];
        this.redFrameResult = indexToResult[Integer.parseInt(redFrameResult)];
        this.patternFrameResult = Integer.parseInt(patternResult) == 0 ? "-" : "Failure";
        this.timeStamp = timeStamp;
        this.image = "none";
        this.averageLuminanceResult = Double.parseDouble(averageLuminanceResult);
        this.averageRedResult = Double.parseDouble(averageRedResult);
    }

    public String getLuminanceStringResult() {
        return luminanceStringResult;
    }

    public String getRedFrameResult() {
        return redFrameResult;
    }

    public String getPatternFrameResult() {
        return patternFrameResult;
    }  

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getFormattedTimeStamp() {
        if (timeStamp.equals("-")) {
            return timeStamp;
        }
        String s = timeStamp.contains(".") ? timeStamp.replaceAll("0*$","").replaceAll("\\.$","") : timeStamp;
        if (!s.contains(".")) {
            s += ".";
        }

        while (s.substring(s.lastIndexOf("."), s.length()).length() <= 3) {
            s += "0";
        }
        return s;
    }

    public double getAverageLuminanceResult() {
        return averageLuminanceResult;
    }

    public double getAverageRedResult() {
        return averageRedResult;
    }

    public boolean getHasScreenshot() {
        return hasScreenshot;
    }

    public boolean getHasExtraRow() {
        return hasExtraRow;
    }

    public String getImage() {
        return this.image;
    }

    public void setLuminanceStringResult(String luminanceStringResult) {
        this.luminanceStringResult = indexToResult[Integer.parseInt(luminanceStringResult)];
    }

    public void setRedFrameResult(String redFrameResult) {
        this.redFrameResult = indexToResult[Integer.parseInt(redFrameResult)];
    }

    public void setPatternFrameResult(String patternFrameResult) {
        this.patternFrameResult = Integer.parseInt(patternFrameResult.trim()) == 0 ? "-" : "Failure";
    }

    public boolean passes() {
        return luminanceStringResult.equals("-") && redFrameResult.equals("-") && patternFrameResult.equals("-");
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    void setHasScreenshot(boolean hasScreenshot) {
        this.hasScreenshot = hasScreenshot;
    }

    void setFormattedTimeStamp(String formattedTimeStamp) {
        this.formattedTimeStamp = formattedTimeStamp;
    }

    public void setAverageLuminanceResult(String averageLuminanceResult) {
        this.averageLuminanceResult = Double.parseDouble(averageLuminanceResult);
    }

    public void setAverageRedResult(String averageRedResult) {
        this.averageRedResult = Double.parseDouble(averageRedResult);
    }

    void setImage(String image, boolean extraRow) {
        if (extraRow) {
            this.hasExtraRow = true;
        } else {
            this.image = image;
            this.hasScreenshot = true;
        }
    }

}
