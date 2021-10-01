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
    B(30.868,   "B");


    private double freq;
    private String value;
    private Note(double freq, String value) {
        this.freq = freq;
        this.value = value;
    }

    public double getFreq() {
        return freq;
    }

    public String getValue() {
        return value;
    }
}
