package pattern;

import fst.FST;

import java.util.*;

public class PatternToFST {

    public static FST createSingleFromPatterns(Collection<Pattern> patterns) {
        return FST.disjunctAll(createMultipleFromPatterns(patterns));
    }

    public static List<FST> createMultipleFromPatterns(Collection<Pattern> patterns) {
        List<FST> fsts = new ArrayList<>();
        for (Pattern p : patterns) {
            fsts.add(new FST(p.getPattern(), p.getFreq(), p.getLabel()));
        }
        return fsts;
    }

    public static Map<String, FST> createMapFromPatterns(Collection<Pattern> patterns) {
        Map<String, FST> fsts = new HashMap<>();
        for (Pattern p : patterns) {
            String rel = p.getLabel();
            if (fsts.containsKey(p.getLabel()))
                fsts.put(rel, FST.disjunct(fsts.get(rel), new FST(p.getPattern(), p.getFreq(), rel)));
            else
                fsts.put(rel, new FST(p.getPattern(), p.getFreq(), rel));
        }
        for (FST fst : fsts.values())
            fst.determinize();
        return fsts;
    }
}
