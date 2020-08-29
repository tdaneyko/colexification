package pattern;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import util.StringUtils;

public class PatternRanker {

    public static TObjectIntMap<String> rankPatterns(TObjectIntMap<String> patternFrequencies){
        // temporary map with weighted frequencies
        TObjectIntMap<String> weightedFrequencies = new TObjectIntHashMap<>();
        // iterator over frequency map
        TObjectIntIterator<String> iter = patternFrequencies.iterator();
        // minimum frequency
        int min = Integer.MAX_VALUE;

        while (iter.hasNext()) {
            iter.advance();
            String pattern = iter.key();
            // count number of non-identity transitions
            int noniden = (StringUtils.count(pattern, '/')-1 - StringUtils.count(pattern, '.')/2);
            // add number of insertions/deletions (affixes are weighted higher because they are a clearer case)
            int weight = noniden+ StringUtils.count(pattern, '-');
            // if only identity freq*10, else freq*weight
            int weightedFrequency = (noniden == 0) ? iter.value() * 10 : iter.value() * weight;
            // update minimum frequency
            if (iter.value() < min) min = iter.value();
            // put patterns
            weightedFrequencies.put(pattern, weightedFrequency);
        }

        // compute threshold
        double freqThreshold = min*5;
        // remove patterns below threshold
        for (Object pattern : weightedFrequencies.keys())
            if (weightedFrequencies.get(pattern) < freqThreshold)
                weightedFrequencies.remove(pattern);

        return weightedFrequencies;
    }
}
