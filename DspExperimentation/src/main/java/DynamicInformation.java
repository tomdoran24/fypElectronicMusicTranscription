import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Class for calculating the dynamic information of a signal e.g. changes in Amplitude, rather than
 * changes in frequency.
 */
public class DynamicInformation {

    private static int TRANSIENT_PASSED_THRESHOLD = 100;
    private static int LOOKAHEAD = 2000;
    private static double SENSITIVITY_THRESHOLD = 0.01;     // higher value is less sensitive
    private static int PEAK_WIDTH_THRESHOLD = 8000;         // 90 ms at 44100hz
    private static int SILENCE_WIDTH_THRESHOLD = 8000;      // account for reverb & tail
    private static int SILENCE_LOOKAHEAD = 1000;

    /**
     * Method to return the indices where each note in the sequence starts
     * @param originalSignal signal array
     * @param samplingPeriod sampling period of signal
     * @return list of indices where notes start
     */
    public static List<Integer> calculateNoteStartingIndices(double[] originalSignal, double samplingPeriod) {

        // remove any silence (at the start or between notes)
        double[] signal = trim(originalSignal);
        int lengthOfSilence = originalSignal.length - signal.length;
        // find first transient in signal
        int firstTransient = findFirstTransient(signal);
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
        // go through rest of signal & find similar peaks to peakN
        List<Integer> peakIndexes = findPeaks(signal, peakN);

        // found peaks of signal (size of peakIndexes should == # of notes)
        // find beginning of note
        List<Integer> startIndicesOfNotes = new ArrayList<>();
        for(Integer peakIndex : peakIndexes) {
            // for first, use silence
            if(peakIndexes.indexOf(peakIndex) == 0) {
                startIndicesOfNotes.add(lengthOfSilence + 1);
            } else {
                // otherwise, just subtract from peak
                startIndicesOfNotes.add(findBeginningOfPeak(signal, peakIndex));
            }
        }

        return startIndicesOfNotes;
    }

    /**
     * Method for calculating the length of notes in seconds given arrays representing the indices of notes
     * & silence.
     * @param startIndicesOfNotes array of indices of notes in signal array
     * @param indicesOfSilence array of indices of silence in signal array
     * @param signal signal array
     * @param samplingPeriod sampling period of signal
     * @return list of note lengths in seconds 
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
     * Method for finding peaks in a signal of a similar or greater amplitude to a given peak
     * @param signal signal array
     * @param peakN index of peak in array to measure other peaks against
     * @return list of peaks in signal array
     */
    private static List<Integer> findPeaks(double[] signal, Integer peakN) {
        List<Integer> peakIndexes = new LinkedList<>();
        peakIndexes.add(peakN);
        int peaksFound = 0;
        int startIndex = peakN;
        while(startIndex < signal.length) {
            // if you find a peak skip forward peak_width_threshold indices
            double[] nextSliceOfArray;
            if(peakIndexes.size() > peaksFound) {
                startIndex = (peakIndexes.get(peaksFound) + PEAK_WIDTH_THRESHOLD) < signal.length ? (peakIndexes.get(peaksFound) + PEAK_WIDTH_THRESHOLD) : startIndex;
                // if we aren't at the end of the array, cut into slices
                if (startIndex + LOOKAHEAD < signal.length) {
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, startIndex + LOOKAHEAD);
                } else {
                    // otherwise, just cut to the end of the array
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, signal.length);
                }
                peaksFound++;
            } else {
                // otherwise, just increment through normally - cutting array into slices of size == LOOKAHEAD
                if (startIndex + LOOKAHEAD < signal.length) {
                    startIndex+=LOOKAHEAD;
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, startIndex + LOOKAHEAD);
                } else {
                    nextSliceOfArray = Arrays.copyOfRange(signal, startIndex, signal.length);
                    startIndex+=LOOKAHEAD;
                }
            }
            // use peak value as a metric to measure other peaks against
            double peakValue = signal[peakN];
            for (int i = 0; i < nextSliceOfArray.length; i++) {
                double value = nextSliceOfArray[i];
                if ((peakValue - value < SENSITIVITY_THRESHOLD) || value > peakValue) {
                    peakIndexes.add(startIndex+i);
                    break;
                }
            }
        }
        return peakIndexes;
    }

    /**
     * Method for calculating the start of a peak given the peak.
     * @param signal signal array
     * @param peakIndex index of peak
     * @return index where given peak starts
     */
    private static Integer findBeginningOfPeak(double[] signal, Integer peakIndex) {
        if((peakIndex-(PEAK_WIDTH_THRESHOLD/2)) > 0) {
            return peakIndex - (PEAK_WIDTH_THRESHOLD/2);
        } else {
            return 0;
        }
    }

    /**
     * Method to trim silence from the start of a signal.
     * @param originalSignal signal array
     * @return signal array with silence removed
     */
    private static double[] trim(double[] originalSignal) {
        // first trim any silence from the beginning of signal
        int inc = 0;
        while(originalSignal[inc] == 0 && inc < originalSignal.length) {
            inc++;
        }
        //double[] newList = Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
        return Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
    }

    /**
     * Method to calculate increasing volume in a signal
     * @param signal signal array
     * @param firstPeak start of increase in volume
     * @return index of the highest part of peak that starts at firstPeak
     */
    private static int trendingUp(double[] signal, int firstPeak) {
        double firstPeakValue = signal[firstPeak];
        double peakValue = signal[firstPeak];
        signal = Arrays.copyOfRange(signal, firstPeak, signal.length);

        int innerInc = 0;
        int peakIndex = 0;
        while(innerInc < LOOKAHEAD) {
            // this will be biased towards identifying a downwards trend
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
     * Method for finding the first transient in a signal.
     * @param signal signal array
     * @return index of first transient
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
     * Method to calculate periods of silence in the given signal array
     * @param signal signal array
     * @return list of indices of silence in array, e.g. [1,5] if signal has a period of silence from 1 to 5
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
