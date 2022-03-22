import java.time.Duration;
import java.util.*;

public class NoteDuration {

    private static int TRANSIENT_PASSED_THRESHOLD = 100;
    private static int LOOKAHEAD = 2000;
    private static double SENSITIVITY_THRESHOLD = 1;     // higher value is more sensitive to peaks
    private static int PEAK_WIDTH_THRESHOLD = 8000;         // 90 ms at 44100hz
    private static int SILENCE_WIDTH_THRESHOLD = 8000;      // account for reverb & tail
    private static int SILENCE_LOOKAHEAD = 1000;

    public static List<Integer> calculateNoteStartingIndices(double[] originalSignal, double samplingPeriod) {

        // remove any silence (at the start or between notes)
        // TO DO: go through array & copy in any stretches of silence
        double[] signal = trim(originalSignal);
        int lengthOfSilence = originalSignal.length - signal.length;
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
        // peak with lookahead

        // go through rest of signal & find similar peaks
        System.out.println(signal[peakN]);
        List<Integer> peakIndexes = findPeaks(signal, peakN);

        // look for periods of silence in signal

        // found peaks of signal (size of peakIndexes should == # of notes)
        // find beginning of note
        List<Integer> startIndicesOfNotes = new ArrayList<>();
        for(Integer peakIndex : peakIndexes) {
            // for first, use silence
            if(peakIndexes.indexOf(peakIndex) == 0) {
                startIndicesOfNotes.add(lengthOfSilence + 1);
            } else {
                startIndicesOfNotes.add(findBeginningOfPeak(signal, peakIndex));
            }
        }

        return startIndicesOfNotes;
    }

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
                if ((peakValue - value < SENSITIVITY_THRESHOLD) || value > peakValue) {
                    peakIndexes.add(startIndex+i);
                    break;
                }
            }
        }
        return peakIndexes;
    }

    private static Integer findBeginningOfPeak(double[] signal, Integer peakIndex) {
        if((peakIndex-(PEAK_WIDTH_THRESHOLD/2)) > 0) {
            return peakIndex - (PEAK_WIDTH_THRESHOLD/2);
        } else {
            return 0;
        }
    }

    private static double[] trim(double[] originalSignal) {
        // first trim any silence from the beginning of signal
        int inc = 0;
        while(originalSignal[inc] == 0 && inc < originalSignal.length) {
            inc++;
        }
        //double[] newList = Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
        return Arrays.copyOfRange(originalSignal,inc,originalSignal.length);
    }

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

    private static int trendingDown(double[] signal, int firstPeak) {
        double peakValue = signal[firstPeak];
        int startOfArray = firstPeak+1; // copy from next index after peak
        signal = Arrays.copyOfRange(signal, startOfArray, signal.length);
        double newPeak = 0.0;
        int innerInc = 0;
        int newPeakIndex = 0;
        int peakIndex = 0;
        while(innerInc < LOOKAHEAD) {
            // calculates top value in next X samples (if peak not surpassed)
            if(signal[innerInc] > newPeak) {
                newPeak = signal[innerInc];
                newPeakIndex = innerInc;
            }
            innerInc++;
        }
        // if this is very next value get average then get the closest value & return the index of this
        if(newPeakIndex == 0) {
            int avInc = 0;
            double avValue = 0;
            // step through any silence or -0 values
            while(avInc < signal.length && signal[avInc] <= 0) {
                avInc++;
            }
            int div = 0;
            while(avInc < signal.length && signal[avInc] > 0) {
                avValue+=signal[avInc];
                avInc++;
                div++;
            }
            // THIS IS TENDING TO PICK DIV/2 - SAFE TO SIMPLIFY TO THIS???????
            double averageValue = avValue/div;
            double diff = Integer.MAX_VALUE;
            int revInc = div-1;
            int closestValueIndex = revInc;
            while(revInc >= 0 && signal[revInc] > 0) {
                double currentDif = Math.abs(averageValue - signal[revInc]);
                if(currentDif < diff) {
                    diff = currentDif;
                    closestValueIndex = revInc;
                }
                revInc--;
            }
            return startOfArray+closestValueIndex;
        } else {
            return startOfArray+newPeakIndex;
        }
    }

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

    public static List<Integer> mergeDuplicateNotes(List<Integer> indicesOfNotes, List<Integer> indicesOfSilence, double[] originalSignal, double samplingFreq) {

        List<Map<List<Double>, List<Double>>> fourierResults = new ArrayList<>();
        int startingIndex = indicesOfNotes.get(0);
        int count = 1;
        while(count <= indicesOfNotes.size()) {
            double[] noteArray = count != indicesOfNotes.size() ? Arrays.copyOfRange(originalSignal, startingIndex, indicesOfNotes.get(count)) : Arrays.copyOfRange(originalSignal, startingIndex, originalSignal.length);
            Map<List<Double>, List<Double>> fourierResult = AutocorrelationByFourier.extractFourierInformation(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq);
            fourierResults.add(fourierResult);
            count++;
        }

        List<Integer> noteIndicesToRemove = new ArrayList<>();
        for(int i = 0; i < fourierResults.size()-1; i++) {
            // if notes have no silence between them & the same dominant freq, assume this is the same note
            int currIndex = indicesOfNotes.get(i);
            int nextIndex = indicesOfNotes.get(i+1);
            // if there's an index of silence before next index
            int inc = 0;
            boolean silenceBetweenNotes = false;
            while(inc < indicesOfSilence.size()) {
                if(indicesOfSilence.get(inc) < nextIndex && indicesOfSilence.get(inc) > currIndex) {
                    silenceBetweenNotes = true;
                }
                inc++;
            }
            if(!silenceBetweenNotes) {
                // check if the fundamental freqs are the same
                Map<List<Double>, List<Double>> currFourierResult = fourierResults.get(i);
                Map<List<Double>, List<Double>> nextFourierResult = fourierResults.get(i + 1);

                List<Double> currFundFreqs = new ArrayList<>(currFourierResult.keySet()).get(0);
                List<Double> nextFundFreqs = new ArrayList<>(nextFourierResult.keySet()).get(0);

                Set<Note> sharedFundamentalNotes = new HashSet<>();
                Set<Note> currFundamentalNotes = new HashSet<>();
                Set<Note> nextFundamentalNotes = new HashSet<>();
                int upperBound = currFundFreqs.size() < nextFundFreqs.size() ? nextFundFreqs.size() : currFundFreqs.size();
                for (int j = 0; j < upperBound; j++) {
                    if (j < currFundFreqs.size()) {
                        Note note = Note.roundFreqToNearestNote(currFundFreqs.get(j));
                        currFundamentalNotes.add(note);
                        sharedFundamentalNotes.add(note);
                    }
                    if (j < nextFundFreqs.size()) {
                        Note note = Note.roundFreqToNearestNote(nextFundFreqs.get(j));
                        nextFundamentalNotes.add(note);
                        sharedFundamentalNotes.add(note);
                    }
                }
                // if notes are equivalent then fundamental notes will be same size as freq lists
                if (compareNoteSets(currFundamentalNotes, nextFundamentalNotes, sharedFundamentalNotes)) {
                    // assume they are the same note, so mark next index for removal
                    noteIndicesToRemove.add(nextIndex);
                }
            }
        }
        // merge any notes that are the same note with no silence between them
        indicesOfNotes.removeIf(entry -> noteIndicesToRemove.contains(entry));
        return indicesOfNotes;
    }

    /**
     * Check that 3 given sets (current, next & shared notes) are equivalent.
     * @param currFundamentalNotes
     * @param nextFundamentalNotes
     * @param sharedFundamentalNotes
     * @return true if equivalent, false if not
     */
    private static boolean compareNoteSets(Set<Note> currFundamentalNotes, Set<Note> nextFundamentalNotes, Set<Note> sharedFundamentalNotes) {
        if(currFundamentalNotes.size() == nextFundamentalNotes.size() && nextFundamentalNotes.size() == sharedFundamentalNotes.size()) {
            // all must be the same size at this point
            for(Note note : currFundamentalNotes) {
                // if any notes don't match
                if(!nextFundamentalNotes.contains(note) || !sharedFundamentalNotes.contains(note)) {
                    return false;
                }
            }
            // if all match
            return true;
        }
        return false;
    }
}
