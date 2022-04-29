import java.util.*;

public class KeySignature {

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

enum Key {

    // Major keys
    C_MAJOR(    "C Major",  Note.C_Minus2,     Signature.MAJOR,    Note.A_Minus2),
    Db_MAJOR(   "Db Major", Note.Db_Minus2,    Signature.MAJOR,    Note.Bb_Minus2),
    D_MAJOR(    "D Major",  Note.D_Minus2,     Signature.MAJOR,    Note.B_Minus2),
    Eb_MAJOR(   "Eb Major", Note.Eb_Minus2,    Signature.MAJOR,    Note.C_Minus2),
    E_MAJOR(    "E Major",  Note.E_Minus2,     Signature.MAJOR,    Note.Db_Minus2),
    F_MAJOR(    "F Major",  Note.F_Minus2,     Signature.MAJOR,    Note.D_Minus2),
    Gb_MAJOR(   "Gb Major", Note.Gb_Minus2,    Signature.MAJOR,    Note.Eb_Minus2),
    G_MAJOR(    "G Major",  Note.G_Minus2,     Signature.MAJOR,    Note.E_Minus2),
    Ab_MAJOR(   "Ab Major", Note.Ab_Minus2,    Signature.MAJOR,    Note.F_Minus2),
    A_MAJOR(    "A Major",  Note.A_Minus2,     Signature.MAJOR,    Note.Gb_Minus2),
    Bb_MAJOR(   "Bb Major", Note.Bb_Minus2,    Signature.MAJOR,    Note.G_Minus2),
    B_MAJOR(    "B Major",  Note.B_Minus2,     Signature.MAJOR,    Note.Ab_Minus2),

    // Minor Keys
    C_MINOR(    "C Minor",  Note.C_Minus2,     Signature.MINOR,    Note.Eb_Minus2),
    Db_MINOR(   "Db Minor", Note.Db_Minus2,    Signature.MINOR,    Note.E_Minus2),
    D_MINOR(    "D Minor",  Note.D_Minus2,     Signature.MINOR,    Note.F_Minus2),
    Eb_MINOR(   "Eb Minor", Note.Eb_Minus2,    Signature.MINOR,    Note.Gb_Minus2),
    E_MINOR(    "E Minor",  Note.E_Minus2,     Signature.MINOR,    Note.G_Minus2),
    F_MINOR(    "F Minor",  Note.F_Minus2,     Signature.MINOR,    Note.Ab_Minus2),
    Gb_MINOR(   "Gb Minor", Note.Gb_Minus2,    Signature.MINOR,    Note.A_Minus2),
    G_MINOR(    "G Minor",  Note.G_Minus2,     Signature.MINOR,    Note.Bb_Minus2),
    Ab_MINOR(   "Ab Minor", Note.Ab_Minus2,    Signature.MINOR,    Note.B_Minus2),
    A_MINOR(    "A Minor",  Note.A_Minus2,     Signature.MINOR,    Note.C_Minus2),
    Bb_MINOR(   "Bb Minor", Note.Bb_Minus2,    Signature.MINOR,    Note.Db_Minus2),
    B_MINOR(    "B Minor",  Note.B_Minus2,     Signature.MINOR,    Note.D_Minus2);


    private String name;
    private Note root;
    private List<Note> notes;
    private Signature signature;
    private Note relativeKey;

    // for readability
    private final int SEMITONE = 1;
    private final int TONE = 2;

    Key(String name, Note root, Signature signature, Note relativeKey) {
        this.name = name;
        this.root = root;
        this.signature = signature;
        this.relativeKey = relativeKey;
        this.notes = generateNotes(signature);
    }

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

    public Key getRelativeKey() {
        // gets the minor compliment of a key

        return null;
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
