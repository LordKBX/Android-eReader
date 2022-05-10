package lordkbx.workshop.ereader;

import java.util.Comparator;

public class StringComparator implements Comparator<String> {
    public int compare(String left, String right) {
        return left.compareTo(right);
    }
}
