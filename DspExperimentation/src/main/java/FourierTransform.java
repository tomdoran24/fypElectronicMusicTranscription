import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.*;

/**
 * Class for performing Fourier Transform using FastFourierTransformer.
 */
public class FourierTransform {

    /**
     * Method to run FFT on an audio signal array.
     *
     * @param signal signal array
     * @return Fourier transform output as list of Double
     */
    public static List<Double> runFFT(double[] signal) {
        // take FFT of signal
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] subSignal = Arrays.copyOf(signal, 65536);
        Complex[] fftResult = fft.transform(subSignal, TransformType.FORWARD);
        Complex[] conjResult = new Complex[fftResult.length];

        // take complex conjugate
        for(int i=0;i< fftResult.length;i++) {
            conjResult[i] = fftResult[i].conjugate();
        }

        // multiple conjugate with original value
        Complex[] convolution = new Complex[conjResult.length];
        for(int i=0;i<conjResult.length;i++) {
            convolution[i] = conjResult[i].multiply(fftResult[i]);
        }

        // remove imaginary from result & return
        List<Double> convolutionAbsolute = new ArrayList<>();
        for(Complex complex : conjResult) {
            convolutionAbsolute.add(complex.abs());
        }
        return convolutionAbsolute;
    }

    /**
     * Method for extracting dominant frequencies & other significant frequencies
     * present in a Fourier Transform output.
     *
     * @param fourierResult fourier transform output
     * @param sampleRate sample rate of WAV file
     * @return Map with list of dominant frequencies as key & other significant frequencies as value
     */
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
}
