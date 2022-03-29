import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException, InvalidMidiDataException {

        // import file
        File file = new File("/Users/tomdoran/Desktop/FYP WAV files/overlapping_notes_test.wav");

        // get signal information
        double[] signal = WavUtilities.getSingleChannelFromSignal(WavUtilities.getWavSignalListFromFile(file), 0);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        double samplingFreq = audioStream.getFormat().getSampleRate();
        double samplingPeriod = 1 / samplingFreq;

        // extract dynamic information from signal
        List<Integer> indicesOfNotes = DynamicInformation.calculateNoteStartingIndices(signal, samplingPeriod);
        List<Integer> indicesOfSilence = DynamicInformation.calculateSilenceIndices(signal);
        List<Double> noteLengthSeconds = DynamicInformation.calculateNoteLengthInSeconds(indicesOfNotes, indicesOfSilence, signal, samplingPeriod);

        // extract frequency information from signal
        List<MIDI> midiData = AudioToMidiConversion.convertAudioSlices(indicesOfNotes, signal, noteLengthSeconds, samplingFreq, samplingPeriod);

        // generate MIDI data from midi data list
        MidiGenerator.generateMidi(midiData, signal.length);
    }
}