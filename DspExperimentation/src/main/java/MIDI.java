public class MIDI {

    private Note note;
    private Long tickTimeStampOn;
    private Long tickTimeStampOff;
    private int velocity;
    private double tickValueOnMs;

    public MIDI(Note note, Long tickTimeStampOn, Long tickTimeStampOff, int velocity, double tickValueOnMs) {
        this.note = note;
        this.tickTimeStampOn = tickTimeStampOn;
        this.tickTimeStampOff = tickTimeStampOff;
        this.velocity = velocity;
        this.tickValueOnMs = tickValueOnMs;
    }

    public Note getNote() {
        return note;
    }

    public void setNote(Note note) {
        this.note = note;
    }

    public Long getTickTimeStampOn() {
        return tickTimeStampOn;
    }

    public void setTickTimeStampOn(Long tickTimeStampOn) {
        this.tickTimeStampOn = tickTimeStampOn;
    }

    public Long getTickTimeStampOff() {
        return tickTimeStampOff;
    }

    public void setTickTimeStampOff(Long tickTimeStampOff) {
        this.tickTimeStampOff = tickTimeStampOff;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public double getTickValueOnMs() {
        return tickValueOnMs;
    }

    public void setTickValueOnMs(double tickValueOnMs) {
        this.tickValueOnMs = tickValueOnMs;
    }


}
