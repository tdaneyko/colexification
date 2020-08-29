package pattern; /**
 * A class to align all the words in a list with each other
 */

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class SequenceAlignment {

    // value by which the cleanup frequency threshold gets increased
    private static final double CLEANUP_INCREASE = 1.0;
    // portion of memory which may be filled before cleanup
    private static final double CLEANUP_THRESHOLD = 0.85;
    // portion of same char transitions for a pattern to be recorded
    private static final double KEEP = 1.0/3;

    /**
     * Gets a set of words, aligns all the words with each other and stores all
     * reasonable patterns in a map.
     * @param words a set of words of the form WORD:POS
     * @return a map of patterns with their frequencies
     */
    public static TObjectIntMap<String> getPatterns(Collection<String> words) {
        // put words into list and shuffle for fair pairings
        List<String> wlist = new ArrayList<>(words);
        Collections.shuffle(wlist);
        return getPatterns(wlist, wlist, true);
    }

    /**
     * Gets a set of words, aligns all the words with each other and stores all
     * reasonable patterns in a map.
     * @param words1 a set of words of the form WORD:POS
     * @param words2 a set of words of the form WORD:POS
     * @return a map of patterns with their frequencies
     */
    public static TObjectIntMap<String> getPatterns(Collection<String> words1, Collection<String> words2) {
        // put words into list and shuffle for fair pairings
        List<String> wlist1 = new ArrayList<>(words1);
        List<String> wlist2 = new ArrayList<>(words2);
        Collections.shuffle(wlist1);
        Collections.shuffle(wlist2);
        return getPatterns(wlist1, wlist2, false);
    }

    /**
     * Gets a set of words, aligns all the words with each other and stores all
     * reasonable patterns in a map.
     * @param wlist1 a shuffled list of words of the form WORD:POS
     * @param wlist2 a shuffled list of words of the form WORD:POS
     * @return a map of patterns with their frequencies
     */
    private static TObjectIntMap<String> getPatterns(List<String> wlist1, List<String> wlist2, boolean same) {
        // table which stores the patterns with frequencies
        TObjectIntMap<String> patternFrequencyTable = new TObjectIntHashMap<>();

        // frequency threshold for next cleanup
        double f = CLEANUP_INCREASE;

        // amount of used memory at which a cleanup will be scheduled
        Runtime r = Runtime.getRuntime();
        long threshold = (long)(r.maxMemory()*CLEANUP_THRESHOLD);

        for (int i = 0; i < wlist1.size(); i++) {
            String w1 = wlist1.get(i);
            //System.out.println(f + " " + i + " " + w1 + " " + (r.totalMemory()-r.freeMemory()) + " " + r.totalMemory());
            for (int j = ((same) ? i+1 : 0); j < wlist2.size(); j++) {
                String w2 = wlist2.get(j);
                if (!(w1.equals(w2))) {
                    // pair words
                    String pattern = findPattern(w1, w2);
                    // if a valid pattern was found, store it in both directions
                    if (!pattern.isEmpty()) {
                        patternFrequencyTable.adjustOrPutValue(pattern, 1, 1);
                        patternFrequencyTable.adjustOrPutValue(findPattern(w2, w1), 1, 1);
                    }
                }
            }

            // if memory threshold is reached, do cleanup to free space
            if (r.totalMemory() - r.freeMemory() > threshold) {
                cleanup(f, patternFrequencyTable);
                f += CLEANUP_INCREASE;
            }
        }

        // do cleanup when finished
        cleanup(f, patternFrequencyTable);

        // final cleanup with threshold = average pattern frequency
        double freqsum = 0;
        for (int freq : patternFrequencyTable.values())
            freqsum += freq;
        cleanup(freqsum/patternFrequencyTable.size(), patternFrequencyTable);

        // return table
        return patternFrequencyTable;
    }

    /**
     * Aligns two tokens to find a transition pattern between the two, where at least
     * KEEP*shorterString.length characters must be same char substitutions.
     * @param a first token
     * @param b second token
     * @return the pattern or an empty string if KEEP*shorterString.length did not equal
     * the amount of same char substitutions
     */
    public static String findPattern(String a, String b) {
        return findPattern(a, b, KEEP);
    }

    /**
     * Aligns two tokens to find a transition pattern between the two. Only returns a pattern
     * if a certain length of the pattern with respect to the shorter token is same character
     * substitution, i.e. if at least a certain percentage of the shorter string reoccurs in
     * the longer string.
     * @param a first token
     * @param b second token
     * @param mustKeep portion of shorter string that must be kept
     * @return the pattern or an empty string if mustKeep*shorterString.length did not equal
     * the amount of same char substitutions
     */
    public static String findPattern(String a, String b, double mustKeep) {
        int split1 = a.indexOf(':');
        int split2 = b.indexOf(':');
        String s1 = a.substring(0, split1);
        String s2 = b.substring(0, split2);

        // Levenshtein
        int[][] T = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            T[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            T[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 2;
                T[i][j] = Math.min(Math.min(
                        T[i - 1][j] + 1,
                        T[i][j - 1] + 1),
                        T[i - 1][j - 1] + cost);
            }
        }

        // used to construct the pattern inside the next loop
        StringBuilder pattern = new StringBuilder();

        // controls if a deletion occurs immediately before an insertion
        // which would then be converted inside the next loop to a substitution
        boolean justDeleted = false;
        // flag to be used when the chars on input and on output tapes are the same
        boolean justAppended = false;
        // count the number of same char substitutions
        int equal = 0;

        // retrieve pattern
        for (int i = s1.length(), j = s2.length(); i > 0 || j > 0; ) {
            if (i > 0 && T[i][j] == T[i - 1][j] + 1) {
                pattern.append(s1.charAt(--i) + "/- ");
                justDeleted = true;
                justAppended = false;
            } else if (j > 0 && T[i][j] == T[i][j - 1] + 1) {
                if ((pattern.toString().endsWith("- ")) && justDeleted) {
                    pattern.replace(pattern.length() - 2, pattern.length(), s2.charAt(--j) + " ");
                } else {
                    pattern.append("-/" + s2.charAt(--j) + " ");
                }
                justDeleted = false;
                justAppended = false;
            } else if (i > 0 && j > 0 && T[i][j] == T[i - 1][j - 1]) {
                if (!justAppended) pattern.append("./. ");
                i--;
                j--;
                justDeleted = false;
                justAppended = true;
                equal++;
            }
        }

        // only keep pattern if at least mustKeep of the shorter string matches the other string
        if (equal > Math.min(s1.length(), s2.length())*mustKeep)
            return pattern
                    .deleteCharAt(pattern.length()-1)
                    .reverse()
                    .append(" ")
                    .append(b.substring(split2+1))
                    .append("/")
                    .append(a.substring(split1+1))
                    .toString();
        // else return empty string
        return "";
    }

    /**
     * Removes all patterns with a frequency below the given threshold and
     * suggests garbage collection afterwards.
     * @param threshold frequency threshold for removal
     */
    private static void cleanup(double threshold, TObjectIntMap<String> map) {
        for (Object pattern : map.keys())
            if (map.get(pattern) <= threshold)
                map.remove(pattern);
        System.gc();
    }
}
