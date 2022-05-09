import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class main {

    /**
     * Main method for running transcription algorithm.
     *
     * @param args
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws WavFileException
     * @throws LineUnavailableException
     * @throws InvalidMidiDataException
     */
    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException, InvalidMidiDataException {
        // import file - update these values to import WAV files
        String fileName = "sine_tone_440";
        String path = "/Users/tomdoran/Desktop/FYP WAV files/";
        File file = new File( path + fileName + ".wav");
        // run transcription algorithm
        Transcribe.audioToMidi(file, fileName);
    }
}