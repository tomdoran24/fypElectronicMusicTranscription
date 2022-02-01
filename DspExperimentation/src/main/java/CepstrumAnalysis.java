import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CepstrumAnalysis {

    static final int ARRAY_CAPACITY_FFT = 65536;

    public static List<Double> Cepstrum(double[] signal) {
        // FFT
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
        double[] subSignal = Arrays.copyOf(signal, ARRAY_CAPACITY_FFT);
        /*Complex[] comSig = new Complex[ARRAY_CAPACITY_FFT];
        for(int i = 0; i<65535;i++) {
            comSig[i] = new Complex(signal[i]);
        }*/
        Complex[] fftResult = fft.transform(subSignal, TransformType.FORWARD);

        // Compute logarithm of result
        Complex[] logResult = new Complex[ARRAY_CAPACITY_FFT];
        for(int i = 0; i< logResult.length; i++) {
            logResult[i] = new Complex(Math.log(fftResult[i].getReal()), fftResult[i].getImaginary());
        }

        // Inverse FFT
        Complex[] ifftResult = fft.transform(subSignal, TransformType.INVERSE);

        List<Double> result = new ArrayList<>(ARRAY_CAPACITY_FFT);
        for(Complex x : ifftResult) {
            result.add(x.abs());
        }
        return result;
    }
}
