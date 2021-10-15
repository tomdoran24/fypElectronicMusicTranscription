import com.github.psambit9791.jdsp.signal.Generate;
import com.github.psambit9791.wavfile.WavFileException;
import org.apache.commons.math3.util.ArithmeticUtils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException {

        Generate gp = new Generate(0, 1, 44000);
        double[] signalArraySineWave = gp.generateSineWave(440);

        // import file
        File file = new File("/Users/tomdoran/Desktop/FYP WAV files/piano_tone_659-25.wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        double samplingFreq = audioStream.getFormat().getSampleRate();
        double samplingPeriod = 1 / samplingFreq;

        double[] signal = WavUtilities.getSingleChannelFromSignal(WavUtilities.getWavSignalListFromFile(file), 0);

        List<Double> result = new LinkedList<>();
        AutocorrelationByFourier.runAutocorrellationByFourier(signal);

        for(Double d : result) {
            System.out.println();
        }
    }



    private static double periodToFrequency(int periodInSamples, double samplingPeriod) {
        double period = periodInSamples * samplingPeriod;
        double freq = 1 / period;
        return freq;
    }

    private static int calculatePeakDistance(List<Double> autocorrelationResult) {
        // calculate distance between first & last peak in autocorrelation
        // 1st peak will always be 0 (perfect correlation)
        int inc = 1;
        // while correlation is decreasing walk through the array
        while(correlationDecreasing(autocorrelationResult.get(inc), autocorrelationResult.get(inc-1))) {
            if(inc == autocorrelationResult.size()-1) {
                break;
            }
            inc++;
        }
        // when starts to increase/level out we are approaching another peak - so now just walk until not increasing
        while(!correlationDecreasing(autocorrelationResult.get(inc), autocorrelationResult.get(inc-1))) {
            if(inc == autocorrelationResult.size()-1) {
                break;
            }
            inc++;
        }
        // inc-1 will now be equal to the distance between the first 2 peaks
        return inc-1;
    }

    private static int calculatePeakDistance(List<Double> autocorrelationResult, int lookahead) {
        // calculate distance between first & last peak in autocorrelation
        // 1st peak will always be 0 (perfect correlation)
        int inc = 1;
        // while correlation is decreasing walk through the array

        while(correlationDecreasingOnAverageWithLookahead(autocorrelationResult.subList(inc, lookahead))) {

            if(inc == autocorrelationResult.size()-lookahead) {
                break;
            }
            inc++;
        }
        while(!correlationDecreasingOnAverageWithLookahead(autocorrelationResult.subList(inc, lookahead))) {

            if(inc == autocorrelationResult.size()-lookahead) {
                break;
            }
            inc++;
        }
        // inc-1 will now be equal to the distance between the first 2 peaks
        return inc-1;
    }

    private static boolean correlationDecreasing(Double n, Double nMinus1) {
        if(n < nMinus1) {
            return true;
        }
        return false;
    }

    private static boolean correlationDecreasingOnAverageWithLookahead(List<Double> autocorrelationSubArray) {
        // get highest value
        Double topValue = autocorrelationSubArray.get(0) > autocorrelationSubArray.get(1) ? autocorrelationSubArray.get(0) : autocorrelationSubArray.get(1);
        // iterate through remaining sub array to see if value generally stays up or down
        int valuesAbove = 0;
        int valuesBelow = 0;
        for(int i = 2; i < autocorrelationSubArray.size(); i++) {
            if(autocorrelationSubArray.get(i) > topValue) {
                valuesAbove++;
            } else {
                valuesBelow++;
            }
        }
        // if there are equal or more values above, correlation is levelling out
        return valuesBelow > valuesAbove;
    }

    private static List<Double> pruneAutocorrelationResult(List<Double> autocorrelationResult) {
        autocorrelationResult.removeIf(entry -> autocorrelationResult.indexOf(entry) % 2 != 0);
        return autocorrelationResult;
    }
}