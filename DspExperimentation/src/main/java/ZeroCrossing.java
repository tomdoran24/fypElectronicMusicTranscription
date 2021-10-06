import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZeroCrossing {

    public static Double zeroCrossingAlgorithm(ArrayList<Double> signal, double samplingFreq) {
        // sampling period is the time between 2 consecutive samples in a sound
        double samplingPeriod = 1 / samplingFreq;

        // Zero Crossing (extremely rough & useless for anything other than single sine waves)
        double nMinus1 = 0;
        double nMinus2 = 5;
        ArrayList<Integer> indices = new ArrayList<>();
        // get value every time a peak passes
        for (int i = 0; i < signal.size(); i++) {
            double n = signal.get(i);
            if (crossedZero(n, nMinus1)) {
                indices.add(signal.indexOf(n));
            }
            if (i > 1) {
                nMinus2 = nMinus1;
            }
            nMinus1 = n;
        }
        // choose most common cycle value (samples between repeats)
        Map<Integer, Integer> differenceValues = new HashMap<>();
        for (int i = 0; i < indices.size() - 1; i++) {
            int diff = indices.get(i + 1) - indices.get(i);
            Integer count = differenceValues.get(diff);
            if (count == null && diff != 0) {
                differenceValues.put(diff, 1);
            } else if (count != null && diff != 0) {
                differenceValues.put(diff, count + 1);
            }
        }

        int commonCycle = 0;
        int topCount = 0;
        for (int key : differenceValues.keySet()) {
            if (differenceValues.get(key) > topCount) {
                commonCycle = key;
                topCount = differenceValues.get(key);
            }
        }

        double period = commonCycle * samplingPeriod;
        double freq = 1 / period;

        return freq;
    }

    private static boolean crossedZero(double n, double nMinus1) {
        if(n < 0 && nMinus1 >= 0) {
            return true;
        } else {
            return false;
        }
    }
}
