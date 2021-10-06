import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WavUtilities {

    public static List<Double> getWavSignalListFromFile(File file) throws IOException, WavFileException {
        // http://www.labbookpages.co.uk/audio/javaWavFiles.html
        WavFile wavFile = WavFile.openWavFile(file);

        // Create a buffer of 100 frames
        double[] buffer = new double[100 * wavFile.getNumChannels()];
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

        return audios;
    }

    public static List<Double> signalArrayToList(double[] signal) {
        List<Double> returnList = new ArrayList<>();
        for(double d : signal) {
            returnList.add(d);
        }
        return returnList;
    }

    public static double[] signalListToArray(List<Double> signal) {
        double[] returnArray = new double[signal.size()];
        for(double d : signal) {
            returnArray[signal.indexOf(d)] = d;
        }
        return returnArray;
    }
}
