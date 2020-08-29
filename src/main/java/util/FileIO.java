package util;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import pattern.Pattern;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class FileIO {

    /**
     * Prints a string-integer map sorted by values from high to low in the format
     * value -tabstop- string (i.e. without labels).
     * @param patterns the pattern map to sort and print
     * @param outfile the desired output file
     */
    public static void writePatterns(TObjectIntMap<String> patterns, String outfile) {
        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outfile)), Charset.forName("UTF-8")))) {
            for (Map.Entry<String, Integer> entry : sortByValues(patterns).entrySet())
                writ.println(entry.getValue() + "\t" + entry.getKey());
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints a string-integer map sorted by values from high to low in the format
     * label -tabstop- value -tabstop- string.
     * @param patterns the patterns to sort and print
     * @param outfile the desired output file
     */
    public static void writePatterns(Collection<Pattern> patterns, String outfile) {
        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outfile)), Charset.forName("UTF-8")))) {
            for (Pattern pattern : sortPatternsByFrequency(patterns))
                writ.println(pattern.getLabel() + "\t" + pattern.getFreq() + "\t" + pattern.getPattern());
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

//    /**
//     * Prints a string-integer map sorted by values from high to low in the format
//     * label -tabstop- value -tabstop- string.
//     * @param patterns the patterns to sort and print
//     * @param outfile the desired output file
//     */
//    public static void writePatterns(Collection<fst.FST> patterns, String outfile) {
//        try (PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outfile)), Charset.forName("UTF-8")))) {
//            for (fst.FST pattern : sortFSTsByFrequency(patterns))
//                writ.println(pattern.getLabel() + "\t" + pattern.getWeight() + "\t" + pattern.getPattern());
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * Sorts a map by values (descending) instead of keys.
     * @param map the map to be sorted
     * @return the map after being sorted by values
     */
    private static <K> Map<K, Integer> sortByValues(final TObjectIntMap<K> map) {
        Comparator<K> valueComparator = (k1, k2) -> {
            int compare = Integer.compare(map.get(k2), (map.get(k1)));
            if (compare == 0) {
                return -1;
            } else {
                return compare;
            }
        };

        Map<K, Integer> sortedByValues = new TreeMap<>(valueComparator);
        for (K key : map.keySet())
            sortedByValues.put(key, map.get(key));
        return sortedByValues;
    }

    /**
     * Sorts a set of patterns by frequency (descending).
     * @param patterns the patterns to be sorted
     * @return a sorted list of patterns
     */
    private static List<Pattern> sortPatternsByFrequency(final Collection<Pattern> patterns) {
        List<Pattern> sortedByValues = new ArrayList<>(patterns);
        sortedByValues.sort((p1, p2) -> Integer.compare(p2.getFreq(), p1.getFreq()));
        return sortedByValues;
    }

//    /**
//     * Sorts a list of FSTs by frequency (descending).
//     * @param patterns the patterns to be sorted
//     * @return a sorted list of patterns
//     */
//    private static List<fst.FST> sortFSTsByFrequency(final Collection<fst.FST> patterns) {
//        List<fst.FST> sortedByValues = new ArrayList<>(patterns);
//        sortedByValues.sort((p1, p2) -> Integer.compare(p2.getWeight(), p1.getWeight()));
//        return sortedByValues;
//    }

    /**
     * Reads a data file into a set of strings with the format <lemma>:<pos>
     * @param file the data file
     * @return a set of strings
     * @throws InvalidDataFormatException if the data is not in a valid format
     */
    public static Set<String> readData(String file) throws InvalidDataFormatException {
        Set<String> data = new HashSet<>();
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)), "UTF-8"))) {
            // get first line and reset reader
            read.mark(4000);
            String line = read.readLine();
            read.reset();
            // detect format
            int colonCount = StringUtils.count(line, ':');
            if (colonCount == 1)
                readSimpleData(read, data);
            else if (colonCount == 2 && StringUtils.count(line, '\t') == 1 && line.indexOf('#') >= 0)
                readComplexData(read, data);
            else
                throw new InvalidDataFormatException(file);
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return data;
    }

    /**
     * Reads a data file in complex format, i.e. <translation>::<pos>\t<orthography>#<ipa>/<more entries...>
     * @param read a reader over the file
     * @param data the set to place the strings in
     * @throws IOException
     */
    private static void readComplexData(BufferedReader read, Set<String> data) throws IOException {
        for (String line = read.readLine(); line != null; line = read.readLine()) {
            String[] fields = line.split("(::)|[\t#/]");
            if (fields.length >= 4) {
                for (int i = 3; i < fields.length; i += 2) {
                    data.add(fields[i] + ":" + fields[1]);
                }
            }
        }
    }

    /**
     * Reads a data file in simple format, i.e. <lemma>:<pos>
     * @param read a reader over the file
     * @param data the set to place the strings in
     * @throws IOException
     */
    private static void readSimpleData(BufferedReader read, Set<String> data) throws IOException {
        for (String line = read.readLine(); line != null; line = read.readLine()) {
            data.add(line);
        }
    }

    /**
     * Reads a simple pattern file without labels into a map.
     * @param file the pattern file
     * @return a map from patterns to frequencies
     */
    public static TObjectIntMap<String> readPatterns(String file) {
        TObjectIntMap<String> patterns = new TObjectIntHashMap<>();
        try(BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] pattern = line.split("\t");
                patterns.put(pattern[1], Integer.parseInt(pattern[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return patterns;
    }

    /**
     * Reads a pattern file with labels into a set of patterns.
     * @param file the pattern file
     * @return a set of labeled patterns with frequencies
     */
    public static Set<Pattern> readPatternsWithLabels(String file) {
        Set<Pattern> patterns = new HashSet<>();
        try(BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)), "UTF-8"))) {
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                String[] pattern = line.split("\t");
                patterns.add(new Pattern(pattern[2], Integer.parseInt(pattern[1]), pattern[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return patterns;
    }


    public static class InvalidDataFormatException extends Exception {

        String message;

        public InvalidDataFormatException() {}

        public InvalidDataFormatException(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return "Invalid data format! (" + message + ")";
        }
    }
}
