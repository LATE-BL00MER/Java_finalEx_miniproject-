package Project_Mold;

import java.util.Vector;

public class TextStore {
    private Vector<String> v = new Vector<String>();

    public TextStore() {
        v.add("Java");
        v.add("Hansung");
        v.add("apple");
        v.add("banana");
        v.add("computer");
        v.add("kitae");
    }

    public String get() {
        int index = (int)(Math.random()*v.size());
        return v.get(index);
    }
}