package pattern;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;

import java.util.HashSet;
import java.util.Set;

public class Pattern implements Comparable<Pattern> {
    private String pattern;
    private int freq;
    private String label;

    public Pattern(String pattern, int freq, String label) {
        this.pattern = pattern;
        this.freq = freq;
        this.label = label;
    }

    public String getPattern() {
        return pattern;
    }

    public int getFreq() {
        return freq;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Pattern) {
            Pattern otherPattern = (Pattern) other;
            return this.freq == otherPattern.freq && this.label.equals(otherPattern.label) && this.pattern.equals(otherPattern.pattern);
        }
        return false;
    }

    public static Set<Pattern> label(TObjectIntMap<String> freqs) {
        return label(freqs, "pattern");
    }

    public static Set<Pattern> label(TObjectIntMap<String> freqs, String prefix) {
        Set<Pattern> patterns = new HashSet<>();
        int label = 0;
        for (TObjectIntIterator<String> iter = freqs.iterator(); iter.hasNext(); ) {
            iter.advance();
            patterns.add(new Pattern(iter.key(), iter.value(), prefix+label));
            label++;
        }
        return patterns;
    }

    @Override
    public int hashCode() {
        return (pattern.hashCode() + label.hashCode()) * freq;
    }

    @Override
    public int compareTo(Pattern o) {
        return this.pattern.compareTo(o.pattern);
    }
}
