import java.util.List;

/**
 * Class for recording information about an FFT output.
 */
public class FFTOutputWrapper {

    private List<Double> peakFrequencies;
    private List<Double> otherFrequencies;

    public FFTOutputWrapper(List<Double> peakFrequencies, List<Double> otherFrequencies) {
        this.peakFrequencies = peakFrequencies;
        this.otherFrequencies = otherFrequencies;
    }

    public List<Double> getPeakFrequencies() {
        return peakFrequencies;
    }

    public List<Double> getOtherFrequencies() {
        return otherFrequencies;
    }
}
