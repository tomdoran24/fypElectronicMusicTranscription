import java.util.*;

/**
 * Class for detecting the position & duration of notes in a signal array.
 */
public class NoteDuration {

    private static int TRANSIENT_PASSED_THRESHOLD = 100;
    private static int LOOKAHEAD = 2000;
    private static double SENSITIVITY_THRESHOLD;     // higher value is more sensitive to peaks
    private static int PEAK_WIDTH_THRESHOLD = 8000;         // 90 ms at 44100hz
    private static int SILENCE_WIDTH_THRESHOLD = 8000;      // account for reverb & tail
    private static int SILENCE_LOOKAHEAD = 1000;

    /**
     * Method for calculating the starting positions of notes in a signal array.
     *
     * @param originalSignal signal array
     * @param samplingPeriod sampling period of WAV file
     * @return list of note indices
     */
    public static List<Integer> calculateNoteStartingIndices(double[] originalSignal, double samplingPeriod) {

        // remove any silence (at the start or between notes)
        double[] signal = trim(originalSignal);
        int firstTransient = findFirstTransient(signal);
        // set sensitivity threshold
        int nextPeakWithLookahead = trendingUp(signal, firstTransient);
        int peakN = nextPeakWithLookahead;

        // if there are further peaks then signal is trending up
        if(nextPeakWithLookahead != firstTransient) {
            // if trending up, walk to other side of the peak
            int peakN_1 = trendingUp(signal, peakN);
            while (peakN != peakN_1) {
                peakN = peakN_1;
                peakN_1 = trendingUp(signal, peakN);
            }
        }
        // once while terminates peakN_1 == peakN must be true so peakN is last highest
        SENSITIVITY_THRESHOLD = signal[peakN] * 0.9999;
        // go through rest of signal & find similar peaks
        List<Integer> peakIndexes = findPeaks(signal, peakN);

        // once found peaks of signal (size of peakIndexes should == # of notes), calculate start indices & return
        List<Integer> startIndicesOfNotes = new ArrayList<>();
        for(Integer peakIndex : peakIndexes) {
            // find beginning of note
            startIndicesOfNotes.add(findBeginningOfPeak(peakIndex));
        }
        return startIndicesOfNotes;
    }

    /**
     * Method for calculating the length of notes in the piece in seconds.
     *
     * @param startIndicesOfNotes list of indices of notes in the signal array
     * @param indicesOfSilence list of indices of silence in the signal array
     * @param signal signal array
     * @param samplingPeriod sampling period of the WAV file
     * @return list of lengths of notes in seconds
     */
    public static List<Double> calculateNoteLengthInSeconds(List<Integer> startIndicesOfNotes, List<Integer> indicesOfSilence, double[] signal, double samplingPeriod) {
        // calculate time between each note
        List<Double> noteLengthsInSecs = new ArrayList<>();
        for(int i = 0; i < startIndicesOfNotes.size(); i++) {
            if(i != startIndicesOfNotes.size()-1) {
                int nextNoteIndex = startIndicesOfNotes.get(i+1);
                // if there is a silence before next note, end at silence
                if(indicesOfSilence.size() > 0 && indicesOfSilence.get(0) < nextNoteIndex) {
                    noteLengthsInSecs.add((indicesOfSilence.get(0) - startIndicesOfNotes.get(i)) * samplingPeriod);
                } else {
                    // otherwise, cut off at next note
                    noteLengthsInSecs.add((nextNoteIndex - startIndicesOfNotes.get(i)) * samplingPeriod);
                }
                indicesOfSilence.removeIf(index -> index < (nextNoteIndex + PEAK_WIDTH_THRESHOLD/2));
            } else {
                // for last detected note, end at silence if it ends before the end of signal
                if(indicesOfSilence.size() > 0 && indicesOfSilence.get(0) < signal.length) {
                    noteLengthsInSecs.add((indicesOfSilence.get(0) - startIndicesOfNotes.get(i)) * samplingPeriod);
                } else {
                    // otherwise, extend to the end of the signal
                    noteLengthsInSecs.add((signal.length - startIndicesOfNotes.get(i)) * samplingPeriod);
                }
            }
        }
        // run FFTs on each section
        return noteLengthsInSecs;
    }

    /**
     * Method to find further peaks in a signal given its first peak.
     *
     * @param signal signal array
     * @param peakN first peak of signal
     * @return list of other peaks in the signal
     */
    private static List<Integer> findPeaks(double[] signal, Integer peakN) {
        List<Integer> peakIndexes = new LinkedList<>();
        peakIndexes.add(peakN);
        int peaksFound = 0;
        int startIndex = peakN;
        while(startIndex < signal.length) {
            double[] nextSliceOfArray;
            // if you find a peak skip forward peak_width_threshold indices
            if(peakIndexes.size() > peaksFound) {
                // set start index to skip over peaks, if reached the end of the signal just use last peak value
                startIndex = (peakIndexes.get(peaksFound) + PEAK_WIDTH_THRESHOLD) < signal.length ? (peakIndexes.get(peaksFound) + PEAK_WIDTH_THRESHOLD) : peakIndexes.get(peaksFound);

                // if start index has been set to skip peaks & this is within the signal with lookahead - cut array to start index + LOOKAHEAD
                if (startIndex == (peakIndexes.get(peaksFound) + PEAK_WIDTH_THRESHOLD) && startIndex + LOOKAHEAD < signal.length) {
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, startIndex + LOOKAHEAD);
                } else {
                    // otherwise, we have reached the end of the signal (or within 90ms of it, so physical capabilities mean there's probably no more notes)
                    break;
                }
                peaksFound++;
            } else {
                // otherwise, just increment through normally - cutting array into slices
                // of size == LOOKAHEAD
                if (startIndex + LOOKAHEAD < signal.length) {
                    startIndex+=LOOKAHEAD;
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, startIndex + LOOKAHEAD);
                } else {
                    break;
                }
            }
            // use peak value found as a metric to measure other peaks
            double peakValue = signal[peakN];
            for (int i = 0; i < nextSliceOfArray.length; i++) {
                double value = nextSliceOfArray[i];
                if (value >= peakValue - SENSITIVITY_THRESHOLD) {
                    peakIndexes.add(startIndex+i);
                    break;
                }
            }
        }
        return peakIndexes;
    }

    /**
     * Method to 'walk back down' first signal array peak to find the index of the transient.
     *
     * @param peakIndex index of first peak
     * @return index of transient
     */
    private static Integer findBeginningOfPeak(Integer peakIndex) {
        if((peakIndex-(PEAK_WIDTH_THRESHOLD/2)) > 0) {
            return peakIndex - (PEAK_WIDTH_THRESHOLD/2);
        } else {
            return 0;
        }
    }

    /**
     * Method to trim silence from the beginning of signal array.
     *
     * @param originalSignal signal array
     * @return signal array without silence at the beginning
     */
    private static double[] trim(double[] originalSignal) {
        // first trim any silence from the beginning of signal
        int inc = 0;
        while(originalSignal[inc] == 0 && inc < originalSignal.length) {
            inc++;
        }
        return Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
    }

    /**
     * Method to detect if a signal continues to increase in amplitude using lookahead.
     *
     * @param signal signal array
     * @param firstPeak index of first peak in signal
     * @return new peak if signal trending up, first peak if not
     */
    private static int trendingUp(double[] signal, int firstPeak) {
        double firstPeakValue = signal[firstPeak];
        double peakValue = signal[firstPeak];
        signal = Arrays.copyOfRange(signal, firstPeak, signal.length);

        int innerInc = 0;
        int peakIndex = 0;
        while(innerInc < LOOKAHEAD) {
            if(signal[innerInc] > peakValue) {
                peakValue = signal[innerInc];
                peakIndex = innerInc;
            }
            innerInc++;
        }
        // if it surpasses first peak in the next X samples it means it is trending upwards
        if(peakValue != firstPeakValue) {
            return firstPeak + peakIndex;
        }
        // if end of array has been reached there must be no higher peaks
        return firstPeak;
    }

    /**
     * Method to find the first peak in a signal.
     *
     * @param signal signal array
     * @return index of first peak
     */
    private static int findFirstTransient(double[] signal) {
        int trnsnt = -1;
        double topValue = 0;
        int transientPassedCounter = 0;
        for(int i = 0; i<signal.length; i++) {
            if(signal[i] > topValue && signal[i] > 0) {
                trnsnt = i;
                topValue = signal[i];
                transientPassedCounter = 0;
            }
            if(signal[i] < topValue && signal[i] > 0) {
                transientPassedCounter++;
            }
            if(transientPassedCounter > TRANSIENT_PASSED_THRESHOLD) {
                break;
            }
        }
        return trnsnt;
    }

    /**
     * Method for finding the indices of silence in a signal array.
     *
     * @param signal signal array
     * @return list of indices of silence
     */
    public static List<Integer> calculateSilenceIndices(double[] signal) {
        signal = trim(signal);
        List<Integer> silenceIndices = new ArrayList<>();
        for(int i = 0; i < signal.length; i++) {
            double n = signal[i];
            // if we have 10 samples of silence assume there is a silence
            if(n == 0) {
                // step while silence
                int lookaheadInc = 0;
                while(i < signal.length && signal[i] == 0 && lookaheadInc < SILENCE_LOOKAHEAD) {
                    lookaheadInc++;
                    i++;
                }
                if(lookaheadInc == SILENCE_LOOKAHEAD) {
                    silenceIndices.add(i - SILENCE_WIDTH_THRESHOLD);
                }
            }
        }
        return silenceIndices;
    }
}
