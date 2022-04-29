import javax.sound.midi.*;
import javax.sound.midi.spi.MidiFileWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MidiGenerator {

    public static void generateMidi(List<MIDI> midiData, Key keySignature, int signalLength, String fileName) throws InvalidMidiDataException, IOException {

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

        for(MIDI midi : midiData) {
            ShortMessage noteOnMsg = new ShortMessage();
            int midiNumber = (int) Math.round(12 * (Math.log(midi.getNote().getFreq() / 220) / Math.log(2)) + 57);
            noteOnMsg.setMessage(ShortMessage.NOTE_ON, midiNumber, midi.getVelocity());
            MidiEvent noteOnEvent = new MidiEvent(noteOnMsg, midi.getTickTimeStampOn());
            track.add(noteOnEvent);

            ShortMessage noteOffMsg = new ShortMessage();
            noteOffMsg.setMessage(ShortMessage.NOTE_OFF, midiNumber, 0);
            MidiEvent noteOffEvent = new MidiEvent(noteOffMsg, midi.getTickTimeStampOff());
            track.add(noteOffEvent);
        }

        //****  set end of track (meta event) 19 ticks later  ****
        mt = new MetaMessage();
        byte[] bet = {}; // empty array
        mt.setMessage(0x2F,bet,0);
        me = new MidiEvent(mt, new Double(signalLength / track.ticks()).longValue());
        track.add(me);

        String prefix = "";
        if(keySignature != null) {
            prefix = "_" + keySignature.getName() + "_";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy_HH-mm", Locale.ENGLISH);
        String dateStr = sdf.format(new Date());

        MidiFileWriter fileWriter = new MidiFileWriterImpl();
        fileWriter.write(seq, 1, new File(fileName+prefix+dateStr+".mid"));

    }
}
