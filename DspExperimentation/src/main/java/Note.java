/**
 * Enum representing musical notes, with their frequency information
 */
public enum Note {
    C(16.352,   "C"),
    Db(17.324,  "Db"),
    D(18.354,   "D"),
    Eb(19.445,  "Eb"),
    E(20.602,   "E"),
    F(21.827,   "F"),
    Gb(23.125,  "Gb"),
    G(24.500,   "G"),
    Ab(25.957,  "Ab"),
    A(27.500,   "A"),
    Bb(29.135,  "Bb"),
    B(30.868,   "B"),

    B1(123.47,   "B1"),
    // OCT 2                        // (2 on Logic, 3 on https://pages.mtu.edu/~suits/notefreqs.html)
    C2(130.81,   "C2"),
    Db2(138.59,  "Db2"),
    D2(146.83,   "D2"),
    Eb2(155.56,  "Eb2"),
    E2(164.81,   "E2"),
    F2(174.61,   "F2"),
    Gb2(185.00,  "Gb2"),
    G2(196.00,   "G2"),
    Ab2(207.65,  "Ab2"),
    A2(220.00,   "A2"),
    Bb2(233.08,  "Bb2"),
    B2(246.94,   "B2"),

    // OCT 3
    C3(261.63,   "C3"),
    Db3(277.18,  "Db3"),
    D3(293.66,   "D3"),
    Eb3(311.13,  "Eb3"),
    E3(329.63,   "E3"),
    F3(349.23,   "F3"),
    Gb3(369.99,  "Gb3"),
    G3(392.00,   "G3"),
    Ab3(415.30,  "Ab3"),
    A3(440.00,   "A3"),
    Bb3(466.16,  "Bb3"),
    B3(493.88,   "B3"),

    // OCT 4
    C4(523.25,   "C4"),
    Db4(554.37,  "Db4"),
    D4(587.33,   "D4"),
    Eb4(622.25,  "Eb4"),
    E4(659.25,   "E4"),
    F4(698.46,   "F4"),
    Gb4(739.99,  "Gb4"),
    G4(783.99,   "G4"),
    Ab4(830.61,  "Ab4"),
    A4(880.00,   "A4"),
    Bb4(932.33,  "Bb4"),
    B4(987.77,   "B4"),

    // OCT 5
    C5(1046.50,   "C5"),
    Db5(1108.73,  "Db5"),
    D5(1174.66,   "D5"),
    Eb5(1244.51,  "Eb5"),
    E5(1318.51,   "E5"),
    F5(1396.91,   "F5"),
    Gb5(1479.98,  "Gb5"),
    G5(1567.98,   "G5"),
    Ab5(1661.22,  "Ab5"),
    A5(1760.00,   "A5"),
    Bb5(1864.66,  "Bb5"),
    B5(1975.53,   "B5");

    private double freq;
    private String value;
    Note(double freq, String value) {
        this.freq = freq;
        this.value = value;
    }

    public double getFreq() {
        return freq;
    }

    /**
     * Method for rounding a given frequency to the nearest note.
     * @param freq given frequency
     * @return Note closest to given frequency
     */
    public static Note roundFreqToNearestNote(double freq) {
        Note upperBound = null;
        Note lowerBound = null;
        for(Note note : Note.values()) {
            // if found bounds, exit & calculate
            if(upperBound != null && lowerBound != null) {
                break;
            } else {
                // otherwise, update bounds until we find the closets frequencies
                if(note.getFreq() < freq) {
                    lowerBound = note;
                }
                // lower bound will repeatedly update until upper bound is updated
                if(note.getFreq() > freq) {
                    upperBound = note;
                }
            }
        }
        if(upperBound != null && lowerBound != null) {
            // calculate whether closer to upper or lower bound & round
            if (upperBound.getFreq() - freq > freq - lowerBound.getFreq()) {
                return lowerBound;
            } else {
                return upperBound;
            }
        } else {
            // in the event we have failed to discover an upper bound, return the lower bound (this is likely to be the top note of the enum)
            return upperBound == null ? lowerBound : upperBound;
        }
    }
}
