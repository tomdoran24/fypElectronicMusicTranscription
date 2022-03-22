

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.*;

public class AutocorrelationByFourier {

    public static List<Double> runAutocorrellationByFourier(double[] signal) {

        // take FFT of signal
        // take complex conj
        // multiply two together
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] subSignal = Arrays.copyOf(signal, 65536);
        /*Complex[] comSig = new Complex[65536];
        for(int i = 0; i<65535;i++) {
            comSig[i] = new Complex(signal[i]);
        }*/
        Complex[] fftResult = fft.transform(subSignal, TransformType.FORWARD);
        //Complex[] fftResult = FastFourierTransform.fft(comSig);
        Complex[] conjResult = new Complex[fftResult.length];

        for(int i=0;i< fftResult.length;i++) {
            conjResult[i] = fftResult[i].conjugate();
        }

        Complex[] convolution = new Complex[conjResult.length];
        for(int i=0;i<conjResult.length;i++) {
            convolution[i] = conjResult[i].multiply(fftResult[i]);
        }

        List<Double> convolutionAbsolute = new ArrayList<>();
        for(Complex complex : conjResult) {
            convolutionAbsolute.add(complex.abs());
        }

        return convolutionAbsolute;
    }

    public static Map<List<Double>,List<Double>> extractFourierInformation(List<Double> fourierResult, double sampleRate) {
        double peakValue = 0;
        int peakIndex = 0;
        for(int i = 0; i<fourierResult.size()/2; i++) {
            if(fourierResult.get(i) > peakValue) {
                // new peak has been detected, record peak
                peakValue = fourierResult.get(i);
                peakIndex = i;
            }
        }
        double cutOffValue = (peakValue / 100)*5; // cut off magnitude for other frequencies (90% less than peak)
        double peakCutOffValue = (peakValue / 100)*80;
        List<Integer> peakIndices = new ArrayList<>();
        peakIndices.add(peakIndex);
        List<Integer> otherPeaksIndices = new ArrayList<>();
        for(int i = 0; i<fourierResult.size()/2; i++) {
            if(fourierResult.get(i) >= peakCutOffValue && i != peakIndex) {
                // record multiple peaks to check for more notes played at one time
                peakIndices.add(i);
            } else if(fourierResult.get(i) >= cutOffValue && i != peakIndex) {
                // record peaks above the threshold, do not re record peak
                otherPeaksIndices.add(i);
            }
        }
        // convert all values to frequency
        List<Double> peakFreqs = new ArrayList<>();
        for(int index : peakIndices) {
            peakFreqs.add(((double) (index+1) / fourierResult.size()) * sampleRate);
        }
        List<Double> otherPeaksFreq = new ArrayList<>();
        for(int index : otherPeaksIndices) {
            otherPeaksFreq.add(((double) (index+1) / fourierResult.size()) * sampleRate);
        }
        // return result as a map
        Map<List<Double>,List<Double>> result = new HashMap<>();
        result.put(peakFreqs, otherPeaksFreq);
        return result;
    }

    private static double getFundamentalFreqFromFourier(List<Double> fourierResult, double sampleRate) {
        // will need to walk through (half of) the result & record the highest peak
        double peakValue = 0;
        int peakIndex = 0;
        for (int i = 0; i < fourierResult.size() / 2; i++) {
            if (fourierResult.get(i) > peakValue) {
                peakValue = fourierResult.get(i);
                peakIndex = i;
            }
        }
        // once peak has been found, calculate frequency
        return ((double) (peakIndex + 1) / fourierResult.size()) * sampleRate;
    }

    public static double getFundamentalFreq(List<Double> fourierResult, double sampleRate) {
        // will need to walk through (half of) the result & record the highest peak
        double peakValue = 0;
        int peakIndex = 0;
        for (int i = 0; i < fourierResult.size() / 2; i++) {
            if (fourierResult.get(i) > peakValue) {
                peakValue = fourierResult.get(i);
                peakIndex = i;
            }
        }
        // once peak has been found, calculate frequency
        return ((double) (peakIndex + 1) / fourierResult.size()) * sampleRate;
    }
}
