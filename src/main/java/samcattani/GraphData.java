package samcattani;

import java.util.ArrayList;

public class GraphData { 
    ArrayList<Double> lumVals;
    ArrayList<Double> redVals;
    ArrayList<String> timeStamps;
    double maxLuminance = 0.75;
    double maxRed = 0.75;

    protected GraphData() {
        this.lumVals = new ArrayList<Double>();
        this.redVals = new ArrayList<Double>();
        this.timeStamps = new ArrayList<String>();
    }
 
    void addLumVal(String lumDiffVal, String lumFlash) {
        double lumDiff = Math.abs(Double.parseDouble(lumDiffVal));
        double lumFlashVal = Math.abs(Double.parseDouble(lumFlash.substring(0, lumFlash.length() - 1))) / 100.0;

        lumVals.add(lumDiff * lumFlashVal);

        if (lumDiff * lumFlashVal > maxLuminance) {
            maxLuminance = lumDiff * lumFlashVal;
        }
    }

    void addRedVal(String redDiffVal, String redFlash) {
        double redDiff = Math.abs(Double.parseDouble(redDiffVal)) / 200;
        double redFlashVal = Math.abs(Double.parseDouble(redFlash.substring(0, redFlash.length() - 1))) / 100.0;
        redVals.add(redDiff * redFlashVal);
        if (redDiff * redFlashVal > maxRed) {
            maxRed = redDiff * redFlashVal;
        }
    }

    void addTimeStamp(String timeStamp) {
        timeStamps.add(timeStamp);
    }

    ArrayList<String> getTimeStamps() {
        if (timeStamps.size() <= 8) {
            return timeStamps;
        }

        int begin = timeStamps.size() / 8;
        ArrayList<String> filteredTimeStamps = new ArrayList<String>();

        for (int i = begin; i < timeStamps.size(); i += begin) {
            String timeStamp = timeStamps.get(i);
            String s = timeStamp.contains(".") ? timeStamp.replaceAll("0*$", "").replaceAll("\\.$", "") : timeStamp;
            if (!s.contains(".")) {
                s += ".";
            }

            while (s.substring(s.lastIndexOf("."), s.length()).length() <= 3) {
                s += "0";
            }
            filteredTimeStamps.add(s);
        }

        return filteredTimeStamps;
    }
}
