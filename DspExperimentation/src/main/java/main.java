import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiFileWriter;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException, InvalidMidiDataException {

        // import file
        File file = new File("/Users/tomdoran/Desktop/FYP WAV files/overlapping_notes_test.wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        double samplingFreq = audioStream.getFormat().getSampleRate();
        double samplingPeriod = 1 / samplingFreq;

        double[] signal = WavUtilities.getSingleChannelFromSignal(WavUtilities.getWavSignalListFromFile(file), 0);
        List<Integer> indicesOfNotes = NoteDuration.calculateNoteStartingIndices(signal, samplingPeriod);
        List<Integer> indicesOfSilence = NoteDuration.calculateSilenceIndices(signal);
        List<Double> noteLengthSeconds = NoteDuration.calculateNoteLengthInSeconds(indicesOfNotes, indicesOfSilence, signal, samplingPeriod);

        double tickLengthInMs = (60000 / (120 * 24));

        List<MIDI> midiData = new ArrayList<>();

        int startingIndex = indicesOfNotes.get(0);
        int count = 1;
        List<Note> notes = new ArrayList<>();
        while(count <= indicesOfNotes.size()) {
            int velocity = 90; // TO DO: implement velocity
            // run fft on each note
            double[] noteArray = count != indicesOfNotes.size() ? Arrays.copyOfRange(signal, startingIndex, indicesOfNotes.get(count)) : Arrays.copyOfRange(signal, startingIndex, signal.length);
            Map<List<Double>, List<Double>> fourierResult = extractFourierInformation(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq);
            Note noteTest = Note.roundFreqToNearestNote(getFourierFundamentalFreq(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq));

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
            // TO DO: implement multiple notes at this point!
            if(!peakNotes.isEmpty()) {
                Note note = new ArrayList<>(peakNotes).get(0);   // change me!

                // set up MIDI message object for note start
                ShortMessage noteOnMsg = new ShortMessage();
                int midiNumber = (int) Math.round(12 * (Math.log(note.getFreq() / 220) / Math.log(2)) + 57);
                noteOnMsg.setMessage(ShortMessage.NOTE_ON, midiNumber, velocity);

                double noteStartInSeconds = indicesOfNotes.get(count - 1) * samplingPeriod;
                double tickValueOnMs = (noteStartInSeconds / tickLengthInMs) * 1000;
                Long tickTimeStampOn = new Double(tickValueOnMs).longValue();

                double noteLengthInSeconds = noteLengthSeconds.get(count - 1);
                double tickValueOffMs = tickValueOnMs + ((noteLengthInSeconds / tickLengthInMs) * 1000);
                Long tickTimeStampOff = new Double(tickValueOffMs).longValue();

                midiData.add(new MIDI(note, tickTimeStampOn, tickTimeStampOff, velocity, tickValueOnMs));
            }
            startingIndex = count != indicesOfNotes.size() ? indicesOfNotes.get(count) : -1;
            count++;
        }

        // generate MIDI data from midi data list
        MidiGenerator.generateMidi(midiData, signal.length);

        List<Double> testSignal = new LinkedList<>();
        for(double d : signal) {
            testSignal.add(d);
        }
        //GraphSignals.createWorkbooks(testSignal, null);
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
        for(int i = 0; i<fourierResult.size()/2; i++) {
            if(fourierResult.get(i) > peakValue) {
                peakValue = fourierResult.get(i);
                peakIndex = i;
            }
        }
        // once peak has been found, calculate frequency
        return ((double) (peakIndex+1) / fourierResult.size()) * sampleRate;
    }

    private static Map<List<Double>,List<Double>> extractFourierInformation(List<Double> fourierResult, double sampleRate) {
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