

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

public class AutocorrelationByFourier {

    public static List<Double> runAutocorrellationByFourier(double[] signal) {

        // take FFT of signal
        // take complex conj
        // multiply two together
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] comSig = new Complex[3004];
        for(int i = 0; i<3004;i++) {
            comSig[i] = new Complex(signal[i]);
        }
        Complex[] fftResult = fft.transform(signal, TransformType.FORWARD);
        Complex[] conjResult = new Complex[fftResult.length];

        for(int i=0;i< fftResult.length;i++) {
            conjResult[i] = fftResult[i].conjugate();
        }

        Complex[] overallComplexResult = new Complex[conjResult.length];
        for(int i=0;i<conjResult.length;i++) {
            overallComplexResult[i] = conjResult[i].multiply(fftResult[i]);
        }

        List<Double> overallAbsoluteResult = new ArrayList<>();
        for(Complex complex : overallComplexResult) {
            overallAbsoluteResult.add(complex.abs());
        }

        return overallAbsoluteResult;
    }
}
