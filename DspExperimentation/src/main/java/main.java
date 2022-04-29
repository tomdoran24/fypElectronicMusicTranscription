import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException, InvalidMidiDataException {

        // import file
        String fileName = "sine_tone_Bb0";
        File file = new File("/Users/tomdoran/Desktop/FYP WAV files/" + fileName + ".wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        double samplingFreq = audioStream.getFormat().getSampleRate();
        double samplingPeriod = 1 / samplingFreq;
        int totalSignalLength = 0;

        Set<Key> keySignature = new HashSet<>();
        List<List<MIDI>> bothChannelsMidiInformation = new LinkedList<>();
        for(int i = 0; i < 2; i++) {
            // get full signal of channel
            double[] signal = WavUtilities.getSingleChannelFromSignal(WavUtilities.getWavSignalListFromFile(file), i);
            // update signal length data if channel has more data
            if(totalSignalLength < signal.length) {
                totalSignalLength = signal.length;
            }
            List<Integer> indicesOfNotes = NoteDuration.calculateNoteStartingIndices(signal, samplingPeriod);
            List<Integer> indicesOfSilence = NoteDuration.calculateSilenceIndices(signal);
            List<Double> noteLengthSeconds = NoteDuration.calculateNoteLengthInSeconds(indicesOfNotes, indicesOfSilence, signal, samplingPeriod);

            // extract information from audio & store Key & Midi data
            Map<Key, List<MIDI>> keyAndMidiData = translateToMidiData(indicesOfNotes, signal, noteLengthSeconds, samplingFreq, samplingPeriod);

            // add Key data
            Key channelKeySig = (Key) keyAndMidiData.keySet().toArray()[0];
            keySignature.add(channelKeySig);

            // add midiData to list
            List<MIDI> channelMidiData = keyAndMidiData.get(channelKeySig);
            bothChannelsMidiInformation.add(channelMidiData);
        }

        // set key signature
        Key key = null;
        // if only 1 key detected
        if(keySignature.size() == 1) {
            key = (Key) keySignature.stream().toArray()[0];
        }

        List<MIDI> midiData = mergeChannelMidi(bothChannelsMidiInformation);

        // generate actual MIDI data from midi data list
        MidiGenerator.generateMidi(midiData, key, totalSignalLength, fileName);

        /*
        List<Double> testSignal = new LinkedList<>();
        for(double d : signal) {
            testSignal.add(d);
        }
        GraphSignals.createWorkbooks(testSignal, null);
         */
    }

    private static List<MIDI> mergeChannelMidi(List<List<MIDI>> bothChannelsMidiInformation) {
        List<MIDI> finalMidi = new LinkedList<>();
        List<MIDI> leftChannel = bothChannelsMidiInformation.get(0);
        List<MIDI> rightChannel = bothChannelsMidiInformation.get(1);
        int leftInc = 0;
        int rightInc = 0;
        int upperBound = 0;
        if(leftInc > rightInc) {
            upperBound = leftChannel.size();
        } else {
            upperBound = rightChannel.size();
        }
        while(leftInc < upperBound && rightInc < upperBound) {
            if (rightInc < rightChannel.size()) {
                // if both sides are different, include both
                if (!leftChannel.contains(rightChannel.get(rightInc)) && !finalMidi.contains(rightChannel.get(rightInc))) {
                    finalMidi.add(rightChannel.get(rightInc));
                }
                // if midi has not been added to final, add it
                if (!finalMidi.contains(rightChannel.get(rightInc))) {
                    finalMidi.add(rightChannel.get(rightInc));
                }
                rightInc++;
            }
            if(leftInc < leftChannel.size()) {
                // if both sides are different, include both
                if (!rightChannel.contains(leftChannel.get(leftInc)) && !finalMidi.contains(leftChannel.get(leftInc))) {
                    finalMidi.add(leftChannel.get(leftInc));
                }
                // if midi has not been added to final, add it
                if (!finalMidi.contains(leftChannel.get(leftInc))) {
                    finalMidi.add(leftChannel.get(leftInc));
                }
                // increment counter
                leftInc++;
            }
        }
        return finalMidi;
    }

    private static Map<Key,List<MIDI>> translateToMidiData(List<Integer> indicesOfNotes, double[] signal, List<Double> noteLengthSeconds, double samplingFreq, double samplingPeriod) {
        double tickLengthInMs = (60000 / (120 * 24));

        List<MIDI> midiData = new ArrayList<>();

        int startingIndex = indicesOfNotes.get(0);
        int count = 1;
        List<Note> notes = new ArrayList<>();
        while(count <= indicesOfNotes.size()) {
            int velocity = calculateNoteVelocity(signal[startingIndex]);
            // run fft on each note
            double[] noteArray = count != indicesOfNotes.size() ? Arrays.copyOfRange(signal, startingIndex, indicesOfNotes.get(count)) : Arrays.copyOfRange(signal, startingIndex, signal.length);
            Map<List<Double>, List<Double>> fourierResult = AutocorrelationByFourier.extractFourierInformation(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq);

            // freq calculation
            List<Double> peakFreqs = new ArrayList<>(fourierResult.keySet()).get(0); // will only ever be 1 value in key set
            Set<Note> peakNotes = new HashSet<>();
            for(Double peakFreq : peakFreqs) {
                peakNotes.add(Note.roundFreqToNearestNote(peakFreq));
            }
            for(Note note : peakNotes) {
                notes.add(note);
            }

            // detect if there are sustained notes under the played note
            List<Double> otherFreqs = fourierResult.get(peakFreqs);
            if(!otherFreqs.isEmpty()) {
                List<Note> notesExtended = new ArrayList<>();
                List<Double> newLengthSeconds = new ArrayList<>();
                for(Double freq : otherFreqs) {
                    // check that the note present in otherFreqs is the same as a note previously played
                    Note otherNotePresent = Note.roundFreqToNearestNote(freq);
                    if(!peakNotes.contains(otherNotePresent) && notes.contains(otherNotePresent) && !notesExtended.contains(otherNotePresent)) {

                        // check track for end time of note
                        MIDI lastNote = midiData.get(midiData.size()-1);
                        if(lastNote.getNote().getFreq() == otherNotePresent.getFreq()) {
                            // set OFF time stamp to null
                            lastNote.setTickTimeStampOff(null);
                        }

                        // check for note end time in this section of signal
                        newLengthSeconds.add(runFFTsToExtendNoteLength(otherNotePresent, noteArray, noteLengthSeconds.get(count - 2), samplingFreq, samplingPeriod));
                        notesExtended.add(otherNotePresent);
                    }
                    // do nothing if rounded value is the same as the peakFreq
                }
                for(int i = 0; i < notesExtended.size(); i++) {
                    // update note off event if any notes have been updated
                    Note note = notesExtended.get(i);
                    for(MIDI midi : midiData) {
                        if(midi.getNote().getFreq() == note.getFreq() && midi.getTickTimeStampOff() == null) {
                            midi.setTickTimeStampOff(new Double(midi.getTickValueOnMs() + (newLengthSeconds.get(i) / tickLengthInMs) * 1000).longValue());
                        }
                    }
                }
            }

            if(!peakNotes.isEmpty()) {
                for(Note note : peakNotes) {

                    // set up MIDI object data
                    double noteStartInSeconds = indicesOfNotes.get(count - 1) * samplingPeriod;
                    double tickValueOnMs = (noteStartInSeconds / tickLengthInMs) * 1000;
                    Long tickTimeStampOn = new Double(tickValueOnMs).longValue();
                    double noteLengthInSeconds = noteLengthSeconds.get(count - 1);
                    double tickValueOffMs = tickValueOnMs + ((noteLengthInSeconds / tickLengthInMs) * 1000);
                    Long tickTimeStampOff = new Double(tickValueOffMs).longValue();

                    // create new MIDI object to store data
                    midiData.add(new MIDI(note, tickTimeStampOn, tickTimeStampOff, velocity, tickValueOnMs));
                }
            }
            startingIndex = count != indicesOfNotes.size() ? indicesOfNotes.get(count) : -1;
            count++;
        }

        // detect key & eliminate accidentals
        Map<Key,Map<Note, Integer>> keyAndNoteWeighting = KeySignature.generateKeyAndNoteWeighting(notes);
        Key keySignature = new LinkedList<>(keyAndNoteWeighting.keySet()).get(0);
        Map<Note, Integer> noteWeights = keyAndNoteWeighting.get(keySignature);
        List<MIDI> revisedMidiData = removeAccidentals(midiData, keySignature, noteWeights, notes.size());

        // eliminate doubles
        List<MIDI> notesToBeRemoved = new ArrayList<>();
        for(int i = 0; i < midiData.size(); i++) {
            // if there is a note such that the end of this note is the start of the next note
            for(int j = 0; j < midiData.size(); j++) {
                if(j != i) {
                    if(midiData.get(i).getNote() == midiData.get(j).getNote()
                            && midiData.get(i).getTickTimeStampOff().compareTo(midiData.get(j).getTickTimeStampOn()) == 0) {
                        notesToBeRemoved.add(midiData.get(j));
                    }
                }
            }
        }

        revisedMidiData = mergeDuplicates(revisedMidiData, notesToBeRemoved);

        Map<Key, List<MIDI>> returnValue = new HashMap<>();
        returnValue.put(keySignature, revisedMidiData);
        return returnValue;
    }

    private static int calculateNoteVelocity(double v) {
        Double roundedVelocity = new Double(Math.abs(v));
        while (roundedVelocity.doubleValue() < 1) {
            if (roundedVelocity.doubleValue() <= 0.1) {
                roundedVelocity = roundedVelocity * 10;
            }
            if(roundedVelocity.doubleValue() >= 0.1 || roundedVelocity.doubleValue() == 0) {
                break;
            }
        }
        // we don't actually want notes of 128 or 1 velocity, so generalise everything to the 50-90 range
        roundedVelocity = (40 * roundedVelocity.doubleValue()) + 50;
        return roundedVelocity.intValue();
    }

    private static List<MIDI> removeAccidentals(List<MIDI> midiData, Key keySignature, Map<Note, Integer> noteWeights, int totalNotes) {
        List<MIDI> accidentals = new LinkedList<>();
        // first get the lowest count
        int lowest = Integer.MAX_VALUE;
        for(Integer count : noteWeights.values()) {
            if(count < lowest) {
                lowest = count;
            }
        }
        // perhaps use this value to set a threshold?
        if(lowest < totalNotes / 12) {
            // consider notes outside the key sig with the lowest count accidentals & remove them
            for(MIDI midi : midiData) {
                if(!keySignature.getNotes().contains(midi.getNote()) &&
                noteWeights.get(midi.getNote()) == lowest) {
                    accidentals.add(midi);
                }
            }
        }
        midiData.removeIf(entry -> accidentals.contains(entry));
        return midiData;
    }

    private static List<MIDI> mergeDuplicates(List<MIDI> midiData, List<MIDI> notesToBeRemoved) {
        // remove overlap between lists
        midiData.removeIf(entry -> notesToBeRemoved.contains(entry));
        for(MIDI noteToBeRemoved : notesToBeRemoved) {
            // first, find the note to extend
            for(MIDI midi : midiData) {
                // if found note to extend, update TickTimeStampOff & move to next note to be removed
                if(midi.getNote() == noteToBeRemoved.getNote() &&
                midi.getTickTimeStampOff().compareTo(noteToBeRemoved.getTickTimeStampOn()) == 0) {
                    midi.setTickTimeStampOff(noteToBeRemoved.getTickTimeStampOff());
                    break;
                }
            }
        }
        return midiData;
    }

    private static Double runFFTsToExtendNoteLength(Note note, double[] noteArray, Double oldLength, Double sampleRate, Double samplingPeriod) {
        int WINDOW = 2000;
        int lowerBound = 0;
        int upperBound = WINDOW;
        boolean notePresent = true;
        while(upperBound != noteArray.length && notePresent) {
            double[] subSection = Arrays.copyOfRange(noteArray, lowerBound, upperBound);
            if(getSpecifiedFourierFreq(AutocorrelationByFourier.runAutocorrellationByFourier(subSection), sampleRate, note)) {
                lowerBound = lowerBound+WINDOW;
                if(upperBound+WINDOW < noteArray.length) {
                    upperBound = upperBound + WINDOW;
                } else {
                    upperBound = noteArray.length;
                }
            } else {
                notePresent = false;
            }
        }
        // use lower bound to update old length
        return oldLength + (lowerBound * samplingPeriod);
    }

    private static boolean getSpecifiedFourierFreq(List<Double> fourierResult, double sampleRate, Note note) {
        boolean present = false;
        for(int i = 0; i<fourierResult.size()/2; i++) {
            if(fourierResult.get(i) >= 2 // only above threshold
                    && Note.roundFreqToNearestNote(((double) i / fourierResult.size()) * sampleRate).getFreq() == note.getFreq()) {
                present = true;
            }
        }
        // once peak has been found, calculate frequency
        return present;
    }

    private static double getFourierFundamentalFreq(List<Double> fourierResult, double sampleRate) {
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


    private static double periodToFrequency(int periodInSamples, double samplingPeriod) {
        double period = periodInSamples * samplingPeriod;
        double freq = 1 / period;
        return freq;
    }

    private static int calculatePeakDistance(List<Double> autocorrelationResult) {
        // calculate distance between first & last peak in autocorrelation
        // 1st peak will always be 0 (perfect correlation)
        int inc = 1;
        // while correlation is decreasing walk through the array
        while(correlationDecreasing(autocorrelationResult.get(inc), autocorrelationResult.get(inc-1))) {
            if(inc == autocorrelationResult.size()-1) {
                break;
            }
            inc++;
        }
        // when starts to increase/level out we are approaching another peak - so now just walk until not increasing
        while(!correlationDecreasing(autocorrelationResult.get(inc), autocorrelationResult.get(inc-1))) {
            if(inc == autocorrelationResult.size()-1) {
                break;
            }
            inc++;
        }
        // inc-1 will now be equal to the distance between the first 2 peaks
        return inc-1;
    }

    private static int calculatePeakDistance(List<Double> autocorrelationResult, int lookahead) {
        // calculate distance between first & last peak in autocorrelation
        // 1st peak will always be 0 (perfect correlation)
        int inc = 1;
        // while correlation is decreasing walk through the array

        while(correlationDecreasingOnAverageWithLookahead(autocorrelationResult.subList(inc, lookahead))) {

            if(inc == autocorrelationResult.size()-lookahead) {
                break;
            }
            inc++;
        }
        while(!correlationDecreasingOnAverageWithLookahead(autocorrelationResult.subList(inc, lookahead))) {

            if(inc == autocorrelationResult.size()-lookahead) {
                break;
            }
            inc++;
        }
        // inc-1 will now be equal to the distance between the first 2 peaks
        return inc-1;
    }

    private static boolean correlationDecreasing(Double n, Double nMinus1) {
        if(n < nMinus1) {
            return true;
        }
        return false;
    }

    private static boolean correlationDecreasingOnAverageWithLookahead(List<Double> autocorrelationSubArray) {
        // get highest value
        Double topValue = autocorrelationSubArray.get(0) > autocorrelationSubArray.get(1) ? autocorrelationSubArray.get(0) : autocorrelationSubArray.get(1);
        // iterate through remaining sub array to see if value generally stays up or down
        int valuesAbove = 0;
        int valuesBelow = 0;
        for(int i = 2; i < autocorrelationSubArray.size(); i++) {
            if(autocorrelationSubArray.get(i) > topValue) {
                valuesAbove++;
            } else {
                valuesBelow++;
            }
        }
        // if there are equal or more values above, correlation is levelling out
        return valuesBelow > valuesAbove;
    }

    private static List<Double> pruneAutocorrelationResult(List<Double> autocorrelationResult) {
        autocorrelationResult.removeIf(entry -> autocorrelationResult.indexOf(entry) % 2 != 0);
        return autocorrelationResult;
    }
}