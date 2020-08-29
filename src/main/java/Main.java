import cluster.ClusterBuilder;
import cluster.ClusterToGraphConverter;
import gnu.trove.map.TObjectIntMap;
import pattern.Pattern;
import pattern.PatternRanker;
import pattern.SequenceAlignment;
import util.FileIO;

import java.util.Set;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            help();
        }
        else if (args[0].equals("-graph")) {
            ClusterToGraphConverter.main(args);
        }
        else {
            String datafile = null;
            String patternfile = null;
            String clusterfile = null;
            boolean align = false;
            boolean rank = false;
            boolean nocomb = false;
            boolean cluster = false;
            boolean clean = false;
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-mode":
                        boolean stop = false;
                        for (int m = i + 1; !stop && m < args.length; m++, i++) {
                            switch (args[m]) {
                                case "align":
                                    align = true;
                                    break;
                                case "rank":
                                    rank = true;
                                    break;
                                case "nocomb":
                                    nocomb = true;
                                    break;
                                case "patterns":
                                    align = true;
                                    rank = true;
                                    break;
                                case "cluster":
                                    cluster = true;
                                    break;
                                case "clean":
                                    clean = true;
                                    break;
                                case "clcl":
                                    cluster = true;
                                    clean = true;
                                    break;
                                case "all":
                                    align = true;
                                    rank = true;
                                    cluster = true;
                                    clean = true;
                                    break;
                                case "allcl":
                                    align = true;
                                    rank = true;
                                    nocomb = true;
                                    cluster = true;
                                    clean = true;
                                    break;
                                default:
                                    stop = true;
                            }
                        }
                    case "-data":
                        datafile = args[++i];
                        break;
                    case "-patterns":
                        if (patternfile == null && patternfile == null && !align && (rank || nocomb || cluster))
                            patternfile = args[++i];
                        else
                            help();
                        break;
                    case "-clusters":
                        if (clusterfile == null && clusterfile == null && clean && !cluster)
                            clusterfile = args[++i];
                        else
                            help();
                        break;
                    default:
                        help();
                }
            }

            if (datafile == null
                    || (patternfile != null && clusterfile != null)
                    || (patternfile == null && !align && (rank || nocomb || cluster))
                    || (clusterfile == null && clean && !cluster))
                help();

            try {
                Set<String> data = FileIO.readData(datafile);
                TObjectIntMap<String> freqs = null;
                Set<Pattern> patterns = null;
                ClusterBuilder clus = null;

                if (align) {
                    System.err.println("Extracting patterns...");
                    freqs = SequenceAlignment.getPatterns(data);
                    FileIO.writePatterns(freqs, datafile + ".freq");
                }
                if (rank) {
                    System.err.println("Ranking patterns...");
                    if (freqs == null)
                        freqs = FileIO.readPatterns(patternfile);
                    patterns = Pattern.label(PatternRanker.rankPatterns(freqs));
                    FileIO.writePatterns(patterns, datafile + ".ranks");
                }
                if (nocomb) {
                    System.err.println("Removing combinations... (This mode is currently disabled)");
                    if (patterns == null)
                        patterns = FileIO.readPatternsWithLabels(patternfile);
//                    List<fst.FST> fsts = PatternToFST.createMultipleFromPatterns(patterns);
//                    PatternCombinator.removeCombinations(fsts);
//                    PatternCombinator.printFSTs(fsts, datafile + ".clranks");
                }
                if (cluster) {
                    System.err.println("Clustering...");
                    if (patterns == null)
                        patterns = FileIO.readPatternsWithLabels(patternfile);
                    clus = new ClusterBuilder(patterns, data);
                    clus.printClusters(datafile + ".clus");
                    clus.printDerivations(datafile + ".deriv");
                }
                if (clean) {
                    System.err.println("Cleaning clusters...");
                    if (clus == null)
                        clus = new ClusterBuilder(clusterfile);
                    clus.cleanClusters();
                    clus.printClusters(datafile + ".clclus");
                    clus.printDerivations(datafile + ".deriv");
                }
            }
            catch (FileIO.InvalidDataFormatException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    private static void help() {
        System.err.println("Usage: java -jar colex.jar -mode <mode(s)> -data <datafile> <additional files if required>");
        System.err.println("   or: java -jar colex.jar -graph -in <infile> -out <outfile-prefix> -id|-minsize|-top <value>");

        System.err.println();
        System.err.println("===REGULAR PROGRAM===");

        System.err.println("-mode: Which parts of the program to run. "
                + "Possible values (multiples are possible, divided by whitespace):");
        System.err.println("\talign -> retrieves patterns with frequencies");
        System.err.println("\trank -> weighs patterns, "
                + "requires additional -patterns <output of align> if run without align");
        System.err.println("\tpatterns -> combination of align and rank");
        System.err.println("\tnocmb -> removes combination patterns, "
                + "requires additional -patterns <output of rank> if run without patterns");
        System.err.println("\tcluster -> clusters data based on provided patterns, "
                + "requires additional -patterns <output of rank> if run without patterns");
        System.err.println("\tclean -> removes unnecessary connections from clusters, "
                + "requires additional -clusters <output of cluster> if run without cluster");
        System.err.println("\tclcl -> combination of cluster and clean, "
                + "requires additional -patterns <output of rank> if run without patterns");
        System.err.println("\tall -> combination of align, rank, cluster and clean");
        System.err.println("\tallcl -> combination of align, rank, nocomb, cluster and clean");

        System.err.println();
        System.err.println("===GRAPH PROGRAM===");

        System.err.println("-id: Print single cluster with id <value>");
        System.err.println("-minsize: Print all clusters with a minimum size of <value>");
        System.err.println("-top: Print top <value> clusters");

        System.exit(0);
    }
}
