import com.github.psambit9791.jdsp.signal.Generate;
import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;

import javax.sound.midi.ShortMessage;
import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import org.apache.poi.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class main {

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, WavFileException, LineUnavailableException {

        Generate gp = new Generate(0, 1, 44000);
        double[] signalArraySineWave = gp.generateSineWave(440);

        // import file
        File file = new File("/Users/tomdoran/Desktop/sine_tone.wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
        List<Double> autocorrelationResult = Autocorrelation.runAutocorrelation(WavUtilities.signalListToArray(WavUtilities.getWavSignalListFromFile(file)));
        int distanceBetweenFirstTwoPeaks = calculatePeakDistance(autocorrelationResult);
        Note note = findByFreq(distanceBetweenFirstTwoPeaks);
        System.out.println(note.getValue());
    }

    private static int calculatePeakDistance(List<Double> autocorrelationResult) {
        return 100;
    }

    private static Note findByFreq(double freq) {
        for(Note note : Note.values()) {
            if(freq % note.getFreq() == 0) {
                return note;
            }
        }
        return null;
    }
}