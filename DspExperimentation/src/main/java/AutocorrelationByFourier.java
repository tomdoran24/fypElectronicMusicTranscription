

import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
}
