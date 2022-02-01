import com.github.psambit9791.jdsp.signal.Generate;
import com.github.psambit9791.wavfile.WavFileException;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.tools.ant.types.selectors.TypeSelector;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiFileWriter;
import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException, InvalidMidiDataException {

        // import file
        File file = new File("/Users/tomdoran/Desktop/FYP WAV files/test.wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        double samplingFreq = audioStream.getFormat().getSampleRate();
        double samplingPeriod = 1 / samplingFreq;

        double[] signal = WavUtilities.getSingleChannelFromSignal(WavUtilities.getWavSignalListFromFile(file), 0);
        List<Integer> noteLengths = NoteDuration.calculateNoteStartingIndices(signal, samplingPeriod);
        List<Double> noteLengthSeconds = NoteDuration.calculateNoteLengthInSeconds(noteLengths, signal, samplingPeriod);
        //List<Double> noteStartTimeSeconds = NoteDuration.calculateNoteStartTimeInSeconds(noteLengths, signal, samplingPeriod);

        Sequence seq = new Sequence(Sequence.PPQ, 24);
        Track track = seq.createTrack();

        // General MIDI sysex
        byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
        SysexMessage sm = new SysexMessage();
        sm.setMessage(b, 6);
        MidiEvent me = new MidiEvent(sm,(long)0);
        track.add(me);

        // add tempo data: 120 bpm
        MetaMessage mt = new MetaMessage();
        byte[] bt = { 0x07, (byte)0xA1, 0x20};
        mt.setMessage(0x51 ,bt, 3);
        me = new MidiEvent(mt,(long)0);
        track.add(me);

        // add track name
        mt = new MetaMessage();
        String TrackName = new String("midifile track");
        mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
        me = new MidiEvent(mt,(long)0);
        track.add(me);

        //****  set omni on  ****
        ShortMessage mm = new ShortMessage();
        mm.setMessage(0xB0, 0x7D,0x00);
        me = new MidiEvent(mm,(long)0);
        track.add(me);

        //****  set poly on  ****
        mm = new ShortMessage();
        mm.setMessage(0xB0, 0x7F,0x00);
        me = new MidiEvent(mm,(long)0);
        track.add(me);

        double tickLengthInMs = (60000 / (120 * 24));

        int startingIndex = noteLengths.get(0);
        int count = 1;
        while(count <= noteLengths.size()) {
            int velocity = 90; // TO DO: implement velocity
            ShortMessage noteOnMsg = new ShortMessage();
            // run fft on each note
            double[] noteArray = count != noteLengths.size() ? Arrays.copyOfRange(signal, startingIndex, noteLengths.get(count)) : Arrays.copyOfRange(signal, startingIndex, signal.length);
            Note note = Note.roundFreqToNearestNote(getFourierFundamentalFreq(AutocorrelationByFourier.runAutocorrellationByFourier(noteArray), samplingFreq));

            // set up MIDI message object for note start
            int midiNumber = (int) Math.round(12 * (Math.log(note.getFreq()/220)/Math.log(2)) + 57);
            noteOnMsg.setMessage(ShortMessage.NOTE_ON, midiNumber, velocity);

            double noteStartInSeconds = noteLengths.get(count-1) * samplingPeriod;
            double tickValueOnMs = (noteStartInSeconds / tickLengthInMs) * 1000;
            Long tickTimeStampOn = new Double(tickValueOnMs).longValue();
            MidiEvent noteOnEvent = new MidiEvent(noteOnMsg, tickTimeStampOn);
            track.add(noteOnEvent);

            // insert either silence or new note to end the note - TO DO: overlapping notes???
            ShortMessage noteOffMsg = new ShortMessage();
            noteOffMsg.setMessage(ShortMessage.NOTE_OFF, midiNumber, 0);
            double noteLengthInSeconds = noteLengthSeconds.get(count-1);
            double tickValueOffMs = tickValueOnMs + ((noteLengthInSeconds / tickLengthInMs) * 1000);
            Long tickTimeStampOff = new Double(tickValueOffMs).longValue();
            MidiEvent noteOffEvent = new MidiEvent(noteOffMsg, tickTimeStampOff);
            track.add(noteOffEvent);

            startingIndex = count != noteLengths.size() ? noteLengths.get(count) : -1;
            count++;
        }

        //****  set end of track (meta event) 19 ticks later  ****
        mt = new MetaMessage();
        byte[] bet = {}; // empty array
        mt.setMessage(0x2F,bet,0);
        me = new MidiEvent(mt, new Double(signal.length / track.ticks()).longValue());
        track.add(me);

        MidiFileWriter fileWriter = new MidiFileWriterImpl();
        fileWriter.write(seq, 1, new File("midi-file-test.mid"));


        List<Double> testSignal = new LinkedList<>();
        for(double d : signal) {
            testSignal.add(d);
        }
        //GraphSignals.createWorkbooks(testSignal, null);
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