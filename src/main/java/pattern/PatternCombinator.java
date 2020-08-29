//import java.io.*;
//import java.nio.charset.Charset;
//import java.util.*;
//
//public class PatternCombinator {
//
//    /**
//     * Removes all FSTs that are combinations of two other FSTs, i.e.
//     * whose output may be generated by a chain of two other FSTs when
//     * given the same input.
//     * @param original The FSTs to check
//     * @return A list of FSTs that were removed
//     */
//    public static Set<fst.FST> removeCombinations(List<fst.FST> original) {
//        // The FSTs that have been removed
//        Set<fst.FST> removed = new HashSet<>();
//        // Whether a combination has been found in this round
//        boolean found = true;
//
//        // repeat until no more combinations are found, maximally for 100 rounds
//        for (int n = 0; n < 100 && found; n++) {
//            found = false;
//
//            // Sort FSTs according to weight, so that the higher weighed
//            // FSTs are combined first
//            original.sort(new fst.FST.PatternWeightDescendingComparator());
//
//            // Remove FSTs with negative weight
//            for (int z = original.size()-1; original.get(z).getWeight() < 0; z--)
//                removed.add(original.remove(z));
//
//            for (int i = 0; i < original.size(); i++) {
//                fst.FST fst1 = original.get(i);
//                // Do not check identity FSTs that only change POS
//                if (!removed.contains(fst1)) {
//                    if (fst1.length() == 1) {
//                        fst1.setWeight(fst1.getWeight() + 10);
//                    } else {
//                        String[] pattern = util.StringUtils.split(fst1.getPattern(), ' ');
//
//                        // Get input and output side of the current fst.FST
//                        StringBuilder inBuild = new StringBuilder();
//                        StringBuilder outBuild = new StringBuilder();
//                        for (int j = 0; j < pattern.length - 1; j++) {
//                            char in = pattern[j].charAt(0);
//                            char out = pattern[j].charAt(2);
//                            if (in != '-')
//                                inBuild.append(in);
//                            if (out != '-')
//                                outBuild.append(out);
//                        }
//                        String input1 = inBuild.append(':').append(fst1.getFromPos()).toString();
//                        String output1 = outBuild.append(':').append(fst1.getToPos()).toString();
//
//                        // Combine with another fst.FST
//                        for (int j = 0; j < original.size(); j++) {
//                            fst.FST fst2 = original.get(j);
//                            if (!fst2.equals(fst1) && !removed.contains(fst2) && fst2.length() > 1) {
//                                String output2 = fst2.apply(output1);
//                                if (!output2.isEmpty()) {
//                                    // Get pattern representation of combination
//                                    String comb = SequenceAlignment.findPattern(input1, output2, 0);
//                                    // Only consider if combination is not an identity pattern
//                                    if (util.StringUtils.count(comb, ' ') > 1) {
//                                        // Check if a following fst.FST represents the same pattern
//                                        for (int k = j + 1; k < original.size(); k++) {
//                                            fst.FST fst3 = original.get(k);
//                                            if (!(fst3.equals(fst1) || fst3.equals(fst2)) && fst3.getPattern().equals(comb)) {
//                                                found = true;
//                                                // adjust weights accordingly
//                                                fst1.setWeight(fst1.getWeight() + 5);
//                                                fst2.setWeight(fst2.getWeight() + 5);
//                                                fst3.setWeight(fst3.getWeight() - 5);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Return removed FSTs
//        return removed;
//    }
//
//    public static void printFSTs(List<fst.FST> fsts, String file) {
//        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(file)), Charset.forName("UTF-8")))) {
//            fsts.sort(new fst.FST.PatternWeightDescendingComparator());
//            for (fst.FST fst : fsts)
//                writ.println(fst.getWeight() + "\t" + fst.getPattern());
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
//}