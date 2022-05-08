import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Class housing the transcription algorithm
 */
public class Transcribe {

    /**
     * Algorithm for performing a conversion from audio file to MIDI data.
     *
     * @param file audio file
     * @param outputFileName output file name
     * @throws InvalidMidiDataException
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws WavFileException
     */
    public static void audioToMidi(File file, String outputFileName) throws InvalidMidiDataException, IOException, UnsupportedAudioFileException, WavFileException {
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
        if(keySignature.size() > 0) {
            key = (Key) keySignature.stream().toArray()[0];
        }

        List<MIDI> midiData = mergeChannelMidi(bothChannelsMidiInformation);

        // generate actual MIDI data from midi data list
        MidiGenerator.generateMidi(midiData, key, totalSignalLength, outputFileName);
    }

    /**
     * Method for translating the extracted characteristics of an audio file into MIDI representation.
     *
     * @param indicesOfNotes list of indexes of note transients
     * @param signal original audio signal
     * @param noteLengthSeconds list of lengths of notes in seconds =
     * @param samplingFreq sampling frequency of WAV file
     * @param samplingPeriod sampling period of WAV file
     * @return Map with detected key signature & MIDI data
     */
    private static Map<Key,List<MIDI>> translateToMidiData(List<Integer> indicesOfNotes, double[] signal, List<Double> noteLengthSeconds, double samplingFreq, double samplingPeriod) {
        List<MIDI> midiData = new ArrayList<>();
        int startingIndex = indicesOfNotes.get(0);
        int count = 1;
        List<Note> notes = new ArrayList<>();
        while(count <= indicesOfNotes.size()) {
            int velocity = calculateNoteVelocity(signal[startingIndex]);
            // cut signal array into slices
            double[] noteArray;
            if(count != indicesOfNotes.size()) {
                noteArray = Arrays.copyOfRange(signal, startingIndex, indicesOfNotes.get(count));
            } else {
                noteArray = Arrays.copyOfRange(signal, startingIndex, signal.length);
            }
            // FOURIER ANALYSIS
            Map<List<Double>, List<Double>> fourierResult = FourierTransform.extractFourierInformation(FourierTransform.runFFT(noteArray), samplingFreq);
            List<Double> peakFreqs = new ArrayList<>(fourierResult.keySet()).get(0); // will only ever be 1 value in key set
            Set<Note> peakNotes = new HashSet<>();
            for(Double peakFreq : peakFreqs) {
                peakNotes.add(Note.roundFreqToNearestNote(peakFreq));
            }
            for(Note note : peakNotes) {
                notes.add(note);
            }
            // set tick length based on 120 bpm & 24 PPQ
            double tickLengthInMs = (60000 / (120 * 24));

            // OVERLAPPING NOTES
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

            // DOMINANT NOTES
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

        // KEY DETECTION & ACCIDENTAL REMOVAL
        Map<Key,Map<Note, Integer>> keyAndNoteWeighting = KeySignature.generateKeyAndNoteWeighting(notes);
        Key keySignature = new LinkedList<>(keyAndNoteWeighting.keySet()).get(0);
        Map<Note, Integer> noteWeights = keyAndNoteWeighting.get(keySignature);
        List<MIDI> revisedMidiData = removeAccidentals(midiData, keySignature, noteWeights, notes.size());

        // DUPLICATE ELIMINATION
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

        //
        revisedMidiData = mergeDuplicates(revisedMidiData, notesToBeRemoved);

        Map<Key, List<MIDI>> returnValue = new HashMap<>();
        returnValue.put(keySignature, revisedMidiData);
        return returnValue;
    }

    /**
     * Method for merging channels MIDI data (WIP: early implementation suspended at end of project).
     *
     * @param bothChannelsMidiInformation
     * @return single list of merged MIDI data
     */
    private static List<MIDI> mergeChannelMidi(List<List<MIDI>> bothChannelsMidiInformation) {
        List<MIDI> finalMidi = new LinkedList<>();
        List<MIDI> leftChannel = bothChannelsMidiInformation.get(0);
        List<MIDI> rightChannel = bothChannelsMidiInformation.get(1);
        int leftInc = 0;
        int rightInc = 0;
        int upperBound = 0;
        // use the higher value as the upper bound
        if (leftInc > rightInc) {
            upperBound = leftChannel.size();
        } else {
            upperBound = rightChannel.size();
        }
        while (leftInc < upperBound && rightInc < upperBound) {
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
            if (leftInc < leftChannel.size()) {
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

    /**
     * Method for calculating the velocity of a note given its amplitude.
     *
     * @param v amplitude
     * @return velocity between 50 & 90
     */
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

    /**
     * Method for accidental removal using Key Signature.
     *
     * @param midiData list of MIDI data detected
     * @param keySignature key signature detected
     * @param noteWeights weighting of notes in piece
     * @param totalNotes total number of notes in piece
     * @return altered list of MIDI data detected
     */
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

    /**
     * Algorithm for merging duplicate notes caused by inaccurate signal slicing.
     *
     * @param midiData list of MIDI data detected
     * @param notesToBeRemoved duplicates detected
     * @return altered list of MIDI detected
     */
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

    /**
     * Specialised Fourier transforms to detect a specific frequency and extend this note if it is present.
     *
     * @param note note to search for
     * @param noteArray section of original signal array
     * @param oldLength old length of note
     * @param sampleRate sample rate of WAV file
     * @param samplingPeriod sampling period of WAV file
     * @return new length of note
     */
    private static Double runFFTsToExtendNoteLength(Note note, double[] noteArray, Double oldLength, Double sampleRate, Double samplingPeriod) {
        int WINDOW = 2000;
        int lowerBound = 0;
        int upperBound = WINDOW;
        boolean notePresent = true;
        while(upperBound != noteArray.length && notePresent) {
            double[] subSection = Arrays.copyOfRange(noteArray, lowerBound, upperBound);
            if(getSpecifiedFourierFreq(FourierTransform.runFFT(subSection), sampleRate, note)) {
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

    /**
     * Assistance method to detect if a specific frequency is present in Fourier transform.
     *
     * @param fourierResult output of Fourier transform
     * @param sampleRate sample rate of WAV file
     * @param note note to check for in Fourier output
     * @return true if note present & false otherwise
     */
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
}
