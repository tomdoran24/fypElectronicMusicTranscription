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
    C_MAJOR(    "C Major",  Note.C,     Signature.MAJOR,    Note.A),
    Db_MAJOR(   "Db Major", Note.Db,    Signature.MAJOR,    Note.Bb),
    D_MAJOR(    "D Major",  Note.D,     Signature.MAJOR,    Note.B),
    Eb_MAJOR(   "Eb Major", Note.Eb,    Signature.MAJOR,    Note.C),
    E_MAJOR(    "E Major",  Note.E,     Signature.MAJOR,    Note.Db),
    F_MAJOR(    "F Major",  Note.F,     Signature.MAJOR,    Note.D),
    Gb_MAJOR(   "Gb Major", Note.Gb,    Signature.MAJOR,    Note.Eb),
    G_MAJOR(    "G Major",  Note.G,     Signature.MAJOR,    Note.E),
    Ab_MAJOR(   "Ab Major", Note.Ab,    Signature.MAJOR,    Note.F),
    A_MAJOR(    "A Major",  Note.A,     Signature.MAJOR,    Note.Gb),
    Bb_MAJOR(   "Bb Major", Note.Bb,    Signature.MAJOR,    Note.G),
    B_MAJOR(    "B Major",  Note.B,     Signature.MAJOR,    Note.Ab),

    // Minor Keys
    C_MINOR(    "C Minor",  Note.C,     Signature.MINOR,    Note.Eb),
    Db_MINOR(   "Db Minor", Note.Db,    Signature.MINOR,    Note.E),
    D_MINOR(    "D Minor",  Note.D,     Signature.MINOR,    Note.F),
    Eb_MINOR(   "Eb Minor", Note.Eb,    Signature.MINOR,    Note.Gb),
    E_MINOR(    "E Minor",  Note.E,     Signature.MINOR,    Note.G),
    F_MINOR(    "F Minor",  Note.F,     Signature.MINOR,    Note.Ab),
    Gb_MINOR(   "Gb Minor", Note.Gb,    Signature.MINOR,    Note.A),
    G_MINOR(    "G Minor",  Note.G,     Signature.MINOR,    Note.Bb),
    Ab_MINOR(   "Ab Minor", Note.Ab,    Signature.MINOR,    Note.B),
    A_MINOR(    "A Minor",  Note.A,     Signature.MINOR,    Note.C),
    Bb_MINOR(   "Bb Minor", Note.Bb,    Signature.MINOR,    Note.Db),
    B_MINOR(    "B Minor",  Note.B,     Signature.MINOR,    Note.D);


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
