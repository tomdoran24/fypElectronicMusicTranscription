import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Autocorrelation {

    // get signal array
    // pick a window size dependent on frequency rate (just use 3000 for simplicity to start)
    // loop through shift values n, comparing all window values using pearson correlation
    // build autocorrelation array with these values

    public static List<Double> runAutocorrelation(double[] signal) {
        List<Double> autocorrelation = new ArrayList<>();
        PearsonsCorrelation pearsonsCorrelation = new PearsonsCorrelation();

        int windowSize = 3000;        // replace me with calculated size

        // for all shift values
        for(int i = 0; i<windowSize; i++) {
            for(int j = 0; j<windowSize; j++) {
                autocorrelation.add(
                        pearsonsCorrelation.correlation(
                                Arrays.copyOfRange(signal, i, windowSize+i),
                                Arrays.copyOfRange(signal, j, windowSize+j)
                    )
                );
            }
        }

        return autocorrelation;
    }
}
