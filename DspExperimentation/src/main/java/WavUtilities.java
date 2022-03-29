import com.github.psambit9791.wavfile.WavFile;
import com.github.psambit9791.wavfile.WavFileException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting information from WAV files for analysis.
 */
public class WavUtilities {

    /**
     * Uses a buffered reader to convert WAV file into a 2D signal array.
     *
     * @param file
     * @return 2D signal array
     * @throws IOException
     * @throws WavFileException
     */
    public static double[][] getWavSignalListFromFile(File file) throws IOException, WavFileException {
        WavFile wavFile = WavFile.openWavFile(file);

        // Create a buffer of 100 frames
        double[] buffer = new double[100 * wavFile.getNumChannels()];
        ArrayList<Double> channelOneArray = new ArrayList<>();
        ArrayList<Double> channelTwoArray = new ArrayList<>();

        int framesRead;

        do
        {
            // Read frames into buffer
            framesRead = wavFile.readFrames(buffer, 100);
            for(int bufferInc = 0; bufferInc < buffer.length; bufferInc++) {
                // separate channels
                if(bufferInc % 2 == 0) {
                    channelOneArray.add(buffer[bufferInc]);
                } else {
                    channelTwoArray.add(buffer[bufferInc]);
                }
            }
        }
        while (framesRead != 0);

        // Close the wavFile
        wavFile.close();

        double[][] audio = new double[wavFile.getNumChannels()][channelOneArray.size()];
        for(int i = 0; i < channelOneArray.size(); i++) {
            audio[0][i] = channelOneArray.get(i);
            audio[1][i] = channelTwoArray.get(i);
        }

        return audio;
    }

    /**
     * Method to return a single channel from the 2D signal array (left or right channel).
     *
     * @param signal
     * @param channel
     * @return 1D array of specified channel
     */
    public static double[] getSingleChannelFromSignal(double[][] signal, int channel) {
        double[] channelSignal = new double[signal[0].length];
        for(int i = 0; i < signal[0].length; i++) {
            channelSignal[i] = signal[channel][i];
        }
        return channelSignal;
    }
}
