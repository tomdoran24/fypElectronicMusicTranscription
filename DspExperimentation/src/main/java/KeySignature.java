import java.util.*;

/**
 * Class for key signature detection.
 */
public class KeySignature {

    /**
     * Method generating key signature and note weights given the notes present in a piece.
     *
     * @param notesPresent list of notes in a piece.
     * @return map with Key as the key and note weights as the value
     */
    public static Map<Key,Map<Note,Integer>> generateKeyAndNoteWeighting(List<Note> notesPresent) {

        Map<Note, Integer> noteWeights = new HashMap<>();

        // build power spectrum of notes
        for(Note note : notesPresent) {
            if(noteWeights.containsKey(note)) {
                int count = noteWeights.get(note) + 1;
                noteWeights.put(note, count);
            } else {
                noteWeights.put(note, 1);
            }
        }

        Map<Key, Integer> powerSpectrum = new HashMap<>();
        for(Note note : noteWeights.keySet()) {
            // only iterate through the major keys to match
            for(int i = 0; i < 12; i++) {
                Key key = Key.values()[i];
                if(key.getNotes().contains(note.convertToZeroOctave())) {
                    if (powerSpectrum.containsKey(key)) {
                        int count = powerSpectrum.get(key) + 1;
                        powerSpectrum.put(key, count);
                    } else {
                        powerSpectrum.put(key, 1);
                    }
                }
            }
        }
        int mostMatchedCount = 0;
        Key mostMatchedKey = null;
        for(Key key : powerSpectrum.keySet()) {
            if(powerSpectrum.get(key) > mostMatchedCount) {
                mostMatchedKey = key;
                mostMatchedCount = powerSpectrum.get(key);
            }
        }
        Map<Key,Map<Note, Integer>> keyAndNoteWeighting = new HashMap<>();
        keyAndNoteWeighting.put(mostMatchedKey, noteWeights);
        return keyAndNoteWeighting;
    }
}

/**
 * Enum for all major & minor keys (western scales only)
 */
enum Key {

    // Major keys
    C_MAJOR(    "C Major",  Note.C_Minus1,     Signature.MAJOR,    Note.A_Minus1),
    Db_MAJOR(   "Db Major", Note.Db_Minus1,    Signature.MAJOR,    Note.Bb_Minus1),
    D_MAJOR(    "D Major",  Note.D_Minus1,     Signature.MAJOR,    Note.B_Minus1),
    Eb_MAJOR(   "Eb Major", Note.Eb_Minus1,    Signature.MAJOR,    Note.C_Minus1),
    E_MAJOR(    "E Major",  Note.E_Minus1,     Signature.MAJOR,    Note.Db_Minus1),
    F_MAJOR(    "F Major",  Note.F_Minus1,     Signature.MAJOR,    Note.D_Minus1),
    Gb_MAJOR(   "Gb Major", Note.Gb_Minus1,    Signature.MAJOR,    Note.Eb_Minus1),
    G_MAJOR(    "G Major",  Note.G_Minus1,     Signature.MAJOR,    Note.E_Minus1),
    Ab_MAJOR(   "Ab Major", Note.Ab_Minus1,    Signature.MAJOR,    Note.F_Minus1),
    A_MAJOR(    "A Major",  Note.A_Minus1,     Signature.MAJOR,    Note.Gb_Minus1),
    Bb_MAJOR(   "Bb Major", Note.Bb_Minus1,    Signature.MAJOR,    Note.G_Minus1),
    B_MAJOR(    "B Major",  Note.B_Minus1,     Signature.MAJOR,    Note.Ab_Minus1),

    // Minor Keys
    C_MINOR(    "C Minor",  Note.C_Minus1,     Signature.MINOR,    Note.Eb_Minus1),
    Db_MINOR(   "Db Minor", Note.Db_Minus1,    Signature.MINOR,    Note.E_Minus1),
    D_MINOR(    "D Minor",  Note.D_Minus1,     Signature.MINOR,    Note.F_Minus1),
    Eb_MINOR(   "Eb Minor", Note.Eb_Minus1,    Signature.MINOR,    Note.Gb_Minus1),
    E_MINOR(    "E Minor",  Note.E_Minus1,     Signature.MINOR,    Note.G_Minus1),
    F_MINOR(    "F Minor",  Note.F_Minus1,     Signature.MINOR,    Note.Ab_Minus1),
    Gb_MINOR(   "Gb Minor", Note.Gb_Minus1,    Signature.MINOR,    Note.A_Minus1),
    G_MINOR(    "G Minor",  Note.G_Minus1,     Signature.MINOR,    Note.Bb_Minus1),
    Ab_MINOR(   "Ab Minor", Note.Ab_Minus1,    Signature.MINOR,    Note.B_Minus1),
    A_MINOR(    "A Minor",  Note.A_Minus1,     Signature.MINOR,    Note.C_Minus1),
    Bb_MINOR(   "Bb Minor", Note.Bb_Minus1,    Signature.MINOR,    Note.Db_Minus1),
    B_MINOR(    "B Minor",  Note.B_Minus1,     Signature.MINOR,    Note.D_Minus1);

    private String name;
    private Note root;
    private List<Note> notes;
    private Signature signature;
    private Note relativeKey;

    // for code readability
    private final int SEMITONE = 1;
    private final int TONE = 2;

    Key(String name, Note root, Signature signature, Note relativeKey) {
        this.name = name;
        this.root = root;
        this.signature = signature;
        this.relativeKey = relativeKey;
        this.notes = generateNotes(signature);
    }

    /**
     * Method to generate the notes in a scale for a key
     * @param signature major or minor
     * @return list of notes in scale
     */
    private List<Note> generateNotes(Signature signature) {
        List<Note> notes = new LinkedList<>();
        Note[] allNotes = Note.values();
        // first iterate to fundamental
        int i = 0;
        while(allNotes[i] != root) {
            i++;
        }
        // at this point allNotes[i] must == root
        int rootIndex = i;  // used to track i
        int scaleIndex = rootIndex;  // used to iterate through the scale
        while(i < rootIndex+7) {    // 7 notes in a scale
            if(i == rootIndex) {    // always add fundamental
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+1) {      // always add 2nd
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+2 && signature == Signature.MAJOR) {   // in major just add the 3rd
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + SEMITONE;
            } else if(i == rootIndex+2 && signature == Signature.MINOR) {   // in minor flatten the 3rd
                notes.add(allNotes[scaleIndex - SEMITONE]);
                scaleIndex = scaleIndex + SEMITONE;
            } else if(i == rootIndex+3) {       // always add 4th
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+4) {       // always add 5th
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+5 && signature == Signature.MAJOR) {   // in major just add the 6th
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+5 && signature == Signature.MINOR) {   // in minor flatten the 6th
                notes.add(allNotes[scaleIndex - SEMITONE]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+6 && signature == Signature.MAJOR) {   // in major just add the 7th
                notes.add(allNotes[scaleIndex]);
                scaleIndex = scaleIndex + TONE;
            } else if(i == rootIndex+6 && signature == Signature.MINOR) {   // in minor flatten the 7th
                notes.add(allNotes[scaleIndex - SEMITONE]);
                scaleIndex = scaleIndex + TONE;
            }
            i++;
        }
        return notes;
    }

    public String getName() {
        return name;
    }

    public List<Note> getNotes() {
        return notes;
    }
}
enum Signature {
    MAJOR,
    MINOR;
}
