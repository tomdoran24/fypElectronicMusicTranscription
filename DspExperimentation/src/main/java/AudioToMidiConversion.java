import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import java.util.*;

/**
 * Implementation of algorithm converting audio information to MIDI data.
 */
public class AudioToMidiConversion {

    /**
     * Main algorithm for converting audio into MIDI representation, using dynamic information
     * (e.g. volume information) and WAV file metadata.
     *
     * @param indicesOfNotes
     * @param signal
     * @param noteLengthSeconds
     * @param samplingFreq
     * @param samplingPeriod
     * @return MIDI data representing given audio
     * @throws InvalidMidiDataException
     */
    public static List<MIDI> convertAudioSlices(List<Integer> indicesOfNotes, double[] signal, List<Double> noteLengthSeconds, double samplingFreq, double samplingPeriod) throws InvalidMidiDataException {

        double tickLengthInMs = (60000 / (120 * 24));   // TO DO: detect BPM instead of defaulting to 120

        List<MIDI> midiData = new ArrayList<>();

        int startingIndex = indicesOfNotes.get(0);
        int count = 1;
        List<Note> notes = new ArrayList<>();
        while(count <= indicesOfNotes.size()) {
            int velocity = 90; // TO DO: implement velocity
            // run fft on each segment of signal
            double[] noteArray = count != indicesOfNotes.size() ? Arrays.copyOfRange(signal, startingIndex, indicesOfNotes.get(count)) : Arrays.copyOfRange(signal, startingIndex, signal.length);
            FFTOutputWrapper fourierResult = extractFourierInformation(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq);

            // round all peak frequencies to the nearest note & record these values
            List<Double> peakFreqs = fourierResult.getPeakFrequencies();
            Set<Note> peakNotes = new HashSet<>();
            for(Double peakFreq : peakFreqs) {
                peakNotes.add(Note.roundFreqToNearestNote(peakFreq));
            }
            // record all notes in signal to detect overlapping notes
            for(Note note : peakNotes) {
                notes.add(note);
            }

            // detect if there are sustained notes under the played note
            List<Double> otherFreqs = fourierResult.getOtherFrequencies();
            if(!otherFreqs.isEmpty()) {
                List<Note> notesExtended = new ArrayList<>();
                List<Double> newLengthSeconds = new ArrayList<>();
                for(Double freq : otherFreqs) {
                    // check that the note present in otherFreqs is the same as a note previously played
                    Note otherNotePresent = Note.roundFreqToNearestNote(freq);
                    if(!peakNotes.contains(otherNotePresent) && notes.contains(otherNotePresent) && !notesExtended.contains(otherNotePresent)) {

                        // check track for end time of note
                        MIDI lastNote = midiData.get(midiData.size()-1);    // TO DO: overlapping more than just last note
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
        return midiData;
    }

    /**
     * Runs a Fast Fourier Transform on a section of signal array to look for the note specified,
     * allowing for sustained notes to be detected when a new note is played.
     *
     * @param note
     * @param noteArray
     * @param oldLength
     * @param sampleRate
     * @param samplingPeriod
     * @return new length of note
     */
    private static Double runFFTsToExtendNoteLength(Note note, double[] noteArray, Double oldLength, Double sampleRate, Double samplingPeriod) {
        // analyse the segment of array in chunks, to detect where the note stops
        int WINDOW = 2000;
        int lowerBound = 0;
        int upperBound = WINDOW;
        boolean notePresent = true;
        while(upperBound != noteArray.length && notePresent) {
            double[] subSection = Arrays.copyOfRange(noteArray, lowerBound, upperBound);
            // if the note specified is present in the FFT output
            if(getSpecifiedFourierFreq(AutocorrelationByFourier.runAutocorrellationByFourier(subSection), sampleRate, note)) {
                // continue to iterate through the segment
                lowerBound = lowerBound+WINDOW;
                if(upperBound+WINDOW < noteArray.length) {
                    upperBound = upperBound + WINDOW;
                } else {
                    upperBound = noteArray.length;
                }
            } else {
                // otherwise, terminate iteration
                notePresent = false;
            }
        }
        // use lower bound to update old length (loop will have terminated when note not present in lowerBound - upperBound)
        return oldLength + (lowerBound * samplingPeriod);
    }

    /**
     * Method for checking for the presence of a given note in an FFT result.
     *
     * @param fourierResult
     * @param sampleRate
     * @param note
     * @return true if note present, false if note not present
     */
    private static boolean getSpecifiedFourierFreq(List<Double> fourierResult, double sampleRate, Note note) {
        boolean present = false;
        for(int i = 0; i<fourierResult.size()/2; i++) {
            if(fourierResult.get(i) >= 2 // only above threshold
                    && Note.roundFreqToNearestNote(((double) i / fourierResult.size()) * sampleRate).getFreq() == note.getFreq()) {
                present = true;
            }
        }
        return present;
    }

    /**
     * Method for extracting fundamental & harmonic peaks from an FFT output, returns an instance
     * of FFTOutputWrapper containing the peak values & other frequencies present in the FFT
     * output.
     *
     * @param fourierResult
     * @param sampleRate
     * @return FFTOutputWrapper with FFT information
     */
    private static FFTOutputWrapper extractFourierInformation(List<Double> fourierResult, double sampleRate) {
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
        // return result as wrapper object
        return new FFTOutputWrapper(peakFreqs, otherPeaksFreq);
    }
}
