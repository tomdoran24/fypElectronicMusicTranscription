import java.time.Duration;
import java.util.Arrays;

public class NoteDuration {

    public static double calculate(double[] originalSignal, double samplingPeriod) {
        double[] signal = trim(originalSignal);
        int silenceIndex = -1;
        double nMinusOne = -1;
        double nMinusTwo = -1;
        for(int i=0; i<signal.length; i++) {
            double n = signal[i];
            // if we have 3 samples of silence in a row, assume note is finished
            if(n == 0 && nMinusOne == 0 && nMinusTwo == 0) {
                silenceIndex = i-3;
                break;
            }

            // set variables
            if(i > 0) {
                nMinusOne = signal[i-1];
            }
            if(i > 1) {
                nMinusTwo = signal[i-2];
            }
        }

        // calculate duration
        if(silenceIndex != -1) {
            return (silenceIndex + (originalSignal.length - signal.length)) * samplingPeriod;
        } else {
            // log error
            return 0;
        }
    }

    private static double[] trim(double[] originalSignal) {
        int inc = 0;
        while(originalSignal[inc] == 0 && inc < originalSignal.length) {
            inc++;
        }
        return Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
    }
}
