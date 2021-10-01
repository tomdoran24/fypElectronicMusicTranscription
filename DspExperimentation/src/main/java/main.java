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
        //allZeroCrossingWork();

    }

    private static void allZeroCrossingWork() throws IOException, LineUnavailableException, UnsupportedAudioFileException, WavFileException {
        double samplingFreq = 44000;
        Generate gp = new Generate(0, 1, (int) samplingFreq);
        double[] signalArraySineWave = gp.generateSineWave(2093);
        ArrayList<Double> sineWaveSignal = new ArrayList<>();
        for(double d : signalArraySineWave) {
            sineWaveSignal.add(d);
        }
        //double sineNoteFreq = zeroCrossingAlgorithm(sineWaveSignal, samplingFreq);
        // Note sineNote = findByFreq(sineNoteFreq);

        ShortMessage shortMessage = new ShortMessage();
        //shortMessage.setMessage(ShortMessage.NOTE_ON, );

        // import file
        File file = new File("/Users/tomdoran/Desktop/sine_tone.wav");
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);

        // extract data
        double sampleRate = fileFormat.getFormat().getSampleRate();
        int bits = audioStream.getFormat().getSampleSizeInBits();
        double max = Math.pow(2, bits-1);
        int bytesOfAudio = audioStream.available();
        byte[] audioBytes = new byte[bytesOfAudio];

        // build signal & process
        ArrayList<Double> signal = new ArrayList<>();
        int bytesRead = -1;
        try {
            // if not supported format encoding throw exception
            if (audioStream.getFormat().getEncoding() != AudioFormat.Encoding.PCM_SIGNED)
                throw new UnsupportedAudioFileException();
            // fill audio bytes array
            bytesRead = audioStream.read(audioBytes);
        } finally {
            audioStream.close();
        }

        while(bytesRead > -1) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(audioBytes);
            byteBuffer.order(fileFormat.getFormat().isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
            // iterate through byte array, while bytes remain
            for (int i = 0; i < byteBuffer.array().length - 1; i++) {
                // fill in other possibilities (8, 16, 32, 64)       // Î24?
                if (bits == 16) {
                    signal.add(new Double(byteBuffer.getShort(i) / max));
                }
            }
            bytesRead = audioStream.read(audioBytes);
        }

        Note result = null;
        result = findByFreq(zeroCrossingAlgorithm(signal, sampleRate));
        System.out.println(result == null ? "No note found!" : result.getValue());

        // https://stackoverflow.com/questions/26824663/how-do-i-use-audio-sample-data-from-java-sound
        float[] signals = new float[bytesOfAudio];
        SimpleAudioConversion.decode(audioBytes, signals, bytesRead, audioStream.getFormat());
        ArrayList<Float> signalsArr = new ArrayList<>();
        for(float f : signals) {
            signalsArr.add(f);
        }

        int numChannels = 1; // Mono
        AudioFormat format = audioStream.getFormat();
        TargetDataLine tdl = AudioSystem.getTargetDataLine(format);
        tdl.open(format);
        tdl.start();
        if (!tdl.isOpen()) {
            System.exit(1);
        }
        byte[] data = new byte[(int)sampleRate*10];
        int read = tdl.read(data, 0, (int)sampleRate*10);
        if (read > 0) {
            for (int i = 0; i < read-1; i = i + 2) {
                long val = ((data[i] & 0xffL) << 8L) | (data[i + 1] & 0xffL);
                long valf = extendSign(val, 16);
            }
        }
        tdl.close();

        // http://www.labbookpages.co.uk/audio/javaWavFiles.html
        WavFile wavFile = WavFile.openWavFile(file);

        // Get the number of audio channels in the wav file
        numChannels = wavFile.getNumChannels();

        // Create a buffer of 100 frames
        double[] buffer = new double[100 * numChannels];
        ArrayList<Double> audios = new ArrayList<>();
        int framesRead;

        do
        {
            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, 100);

            // Loop through frames and look for minimum and maximum value
            for(int i = 0; i < buffer.length; i++) {
                // avoid adding duplicate entries
                if(i == 0 || buffer[i] != audios.get(audios.size()-1)) {
                    audios.add(buffer[i]);
                }
            }
        }
        while (framesRead != 0);

        // Close the wavFile
        wavFile.close();


        // create Workbook with arrays
        createWorkbooks(audios, sineWaveSignal);
        Note wavNote = findByFreq(zeroCrossingAlgorithm(audios, samplingFreq));
        System.out.println(wavNote.getValue());
    }

    private static void createWorkbooks(ArrayList<Double> wavSignal, ArrayList<Double> sineSignal) throws java.io.IOException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet1 = wb.createSheet("Sheet 1");
        Row wavRow = sheet1.createRow(0);
        Row sineRow = sheet1.createRow(1);

        for(int i = 0; i < 256; i++) {
            wavRow.createCell(i).setCellValue(wavSignal.get(i));
        }
        for(int i = 0; i < 256; i++) {
            sineRow.createCell(i).setCellValue(sineSignal.get(i));
        }

        try  (OutputStream fileOut = new FileOutputStream("waves.xls")) {
            wb.write(fileOut);
        }
    }

    private static long extendSign(long temp, int bitsPerSample) {
        int extensionBits = 64 - bitsPerSample;
        return (temp << extensionBits) >> extensionBits;
    }


    private static Double zeroCrossingAlgorithm(ArrayList<Double> signal, double samplingFreq) {
        // sampling period is the time between 2 consecutive samples in a sound
        double samplingPeriod = 1 / samplingFreq;

        // Zero Crossing (extremely rough & useless for anything other than single sine waves)
        double nMinus1 = 0;
        double nMinus2 = 5;
        ArrayList<Integer> indices = new ArrayList<>();
        // get value every time a peak passes
        for (int i = 0; i < signal.size(); i++) {
            double n = signal.get(i);
            if (crossedZero(n, nMinus1)) {
                indices.add(signal.indexOf(n));
            }
            if (i > 1) {
                nMinus2 = nMinus1;
            }
            nMinus1 = n;
        }
        // choose most common cycle value (samples between repeats)
        Map<Integer, Integer> differenceValues = new HashMap<>();
        for (int i = 0; i < indices.size() - 1; i++) {
            int diff = indices.get(i + 1) - indices.get(i);
            Integer count = differenceValues.get(diff);
            if (count == null && diff != 0) {
                differenceValues.put(diff, 1);
            } else if (count != null && diff != 0) {
                differenceValues.put(diff, count + 1);
            }
        }

        int commonCycle = 0;
        int topCount = 0;
        for (int key : differenceValues.keySet()) {
            if (differenceValues.get(key) > topCount) {
                commonCycle = key;
                topCount = differenceValues.get(key);
            }
        }

        double period = commonCycle * samplingPeriod;
        double freq = 1 / period;

        return freq;
    }

    private static boolean crossedZero(double n, double nMinus1) {
        if(n < 0 && nMinus1 >= 0) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean crossedPeak(double n, double nMinus1, double nMinus2) {
        if(n < nMinus1 && nMinus2 <= nMinus1) {
            //this will mean n-1 is the end of the peak
            return true;
        } else {
            return false;
        }
    }

    private static Note findByFreq(double freq) {
        for(Note note : Note.values()) {
            if(freq % note.getFreq() == 0) {     // check this maths
                return note;
            }
        }
        return null;
    }
}