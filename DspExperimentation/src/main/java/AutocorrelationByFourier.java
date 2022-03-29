import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of Autocorrelation by Fourier Transform.
 */
public class AutocorrelationByFourier {

    /**
     * Algorithm implementing Autocorrelation by Fourier Transform, calculates the
     * convolution of the signal by running a Fourier Transform, then taking the
     * complex conjugate of this result & multiplying the two together.
     *
     * @param signal
     * @return
     */
    public static List<Double> runAutocorrellationByFourier(double[] signal) {

        // take FFT of signal
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] subSignal = Arrays.copyOf(signal, 65536);
        Complex[] fftResult = fft.transform(subSignal, TransformType.FORWARD);

        // take complex conjugate of FFT (invert sign of imaginary part)
        Complex[] conjResult = new Complex[fftResult.length];
        for(int i=0;i< fftResult.length;i++) {
            conjResult[i] = fftResult[i].conjugate();
        }

        // multiply FFT & complex conjugate (convolution)
        Complex[] convolution = new Complex[conjResult.length];
        for(int i=0;i<conjResult.length;i++) {
            convolution[i] = conjResult[i].multiply(fftResult[i]);
        }

        // remove imaginary part of result for ease of use in code & return
        List<Double> convolutionAbsolute = new ArrayList<>();
        for(Complex complex : conjResult) {
            convolutionAbsolute.add(complex.abs());
        }
        return convolutionAbsolute;
    }
}
