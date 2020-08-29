package cluster;

import fst.FST;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import pattern.Pattern;
import pattern.PatternToFST;
import pattern.SequenceAlignment;
import util.FileIO;
import util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ClusterBuilder {

    private List<String> allTokens;
    private List<Cluster> clusters;
    private Map<String, Cluster> derivTrees;
    private Map<String, String> patterns;
    private Map<String, FST> fsts;
    private FST singleFST;
    private FST combFST;

    /**
     * Constructs a builder that takes every word in the list, searches for all other
     * words which this word can be transformed into by at least one of the transducers,
     * and groups all those words in a cluster.
     * @param patterns a list with FSTs that represent all possible transformations
     * @param words a file with a list of words that are to be put into clusters
     */
    public ClusterBuilder(Set<Pattern> patterns, Collection<String> words) {
        this.allTokens = new ArrayList<>(words);
        this.patterns = new HashMap<>();
        for (Pattern pattern : patterns)
            this.patterns.put(pattern.getLabel(), pattern.getPattern());

        this.fsts = PatternToFST.createMapFromPatterns(patterns);
        this.singleFST = FST.disjunctAll(this.fsts.values());
        this.singleFST.determinize();
        this.combFST = null;

        cluster();
        getDerivationTrees();
        combinePatterns();
        //getStemPatterns();
        cluster();
    }

    /**
     * Reads a set of clusters from a file generated by the ClusterBuilder class.
     * @param file the cluster file
     */
    public ClusterBuilder(String file) {
        clusters = new ArrayList<>();
        derivTrees = new HashMap<>();
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)), "UTF-8"))) {
            Map<String, Cluster.ClusterNode> currentCluster = new HashMap<>();
            for (String line = read.readLine(); line != null; line = read.readLine()) {
                if (!line.isEmpty()) {
                    String[] fields = StringUtils.split(line, '\t');
                    if (fields.length == 2) {
                        Cluster.ClusterNode rootNode = new Cluster.ClusterNode(fields[1]);
                        clusters.add(new Cluster(rootNode));
                        currentCluster = new HashMap<>();
                        currentCluster.put(fields[1], rootNode);
                    }
                    else {
                        if (!currentCluster.containsKey(fields[0]))
                            currentCluster.put(fields[0], new Cluster.ClusterNode(fields[0]));
                        Cluster.ClusterNode from = currentCluster.get(fields[0]);
                        if (!currentCluster.containsKey(fields[3]))
                            currentCluster.put(fields[3], new Cluster.ClusterNode(fields[3]));
                        Cluster.ClusterNode to = currentCluster.get(fields[3]);
                        if (fields[1].endsWith(".rev"))
                            from.addReverse(to, fields[1], Integer.parseInt(fields[2]));
                        else
                            from.addTransition(to, fields[1], Integer.parseInt(fields[2]));
                    }
                }
            }
            getDerivationTrees();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clusters the words into groups of related words.
     */
    public void cluster() {
        clusters = new ArrayList<>();

        // Set of words already in some cluster
        Set<String> done = new HashSet<>();

        for (int i = 0; i < allTokens.size(); i++){
            String token1 = allTokens.get(i);
            //System.out.print(i + " " + token1);
            Cluster.ClusterNode node1 = new Cluster.ClusterNode(token1);

            if (!done.contains(token1)) {
                // Start new cluster
                //System.out.print("cluster " + token1);
                Map<String, Cluster.ClusterNode> nodes = new HashMap<>();
                nodes.put(token1, node1);
                clusters.add(new Cluster(node1));
                // Stack of related words that still have to be processed
                Stack<Cluster.ClusterNode> candidates = new Stack<>();
                candidates.push(node1);

                // Search for all related words
                while (!candidates.isEmpty()) {
                    // Get a related word from the stack
                    node1 = candidates.pop();
                    token1 = node1.getLabel();

                    if (!done.contains(token1)) {
                        done.add(token1);
                        List<Cluster.ClusterNode> additions = getMatches(node1, allTokens, done, nodes);
                        for (Cluster.ClusterNode node2 : additions)
                            candidates.push(node2);
                    }
                }
            }
            //System.out.println();
        }
        updateRoots();
    }

    private List<Cluster.ClusterNode> getMatches(Cluster.ClusterNode node1, List<String> allTokens,
                                                 Set<String> done, Map<String, Cluster.ClusterNode> nodes) {
        String token1 = node1.getLabel();
        List<Cluster.ClusterNode> matches = new ArrayList<>();

        // Go through all not yet processed words
        for (int j = 0; j < allTokens.size(); j++) {
            // Get a second word to match
            String token2 = allTokens.get(j);
            if (!token1.equals(token2)) {
                // Check whether there is a pattern from token1 to token2
                FST.Result rel = singleFST.transduce(token1, token2);
                if (combFST != null && rel.isEmpty())
                    rel = combFST.transduce(token1, token2);
                // Add token2 to cluster and stack if it is related
                if (!rel.isEmpty()) {
                    if (!nodes.containsKey(token2))
                        nodes.put(token2, new Cluster.ClusterNode(token2));
                    Cluster.ClusterNode node2 = nodes.get(token2);
                    node1.addTransition(node2, rel.getRelation(), rel.getWeight(), true);
                    if (!done.contains(token2))
                        matches.add(node2);
                }
            }
        }

        return matches;
    }

    /**
     * During the clustering process, roots were fixed to the token of the cluster
     * that was encountered first. This method goes through all clusters and tries
     * to find the real root of each cluster, i.e. the shortest token. If there are
     * two or more tokens of the same minimum length, the one with the most connections
     * to other tokens is taken as the root.
     */
    private void updateRoots() {
        for (Cluster cluster : clusters)
            cluster.reroot();
    }

    /**
     * Cleans the clusters, i.e. removes all unnecessary connections between nodes.
     * Only the shortest path from the root to each node is retained, and if there
     * are two equally short paths, the one ending in the thickest connection is
     * kept.
     */
    public void cleanClusters() {
        for (Cluster cluster : clusters) {
            cluster.clean();
        }

        // update derivation trees with new clusters
        getDerivationTrees();
    }

    /**
     * Builds the derivation trees from the existing clusters.
     */
    private void getDerivationTrees() {
        derivTrees = new HashMap<>();
        for (Cluster cluster : clusters) {
            if (cluster.hasEdges()) {
                String currentToken = cluster.getRoot();
                String pos = currentToken.substring(currentToken.indexOf(':') + 1);

                if (!derivTrees.containsKey(pos))
                    derivTrees.put(pos, new Cluster(new Cluster.ClusterNode("ROOT:" + pos)));
                Cluster derivTree = derivTrees.get(pos);

                cluster.countDerivations(derivTree);
            }
        }
    }

    /**
     * Get all real-world combinations of patterns and store them in combFST.
     */
    private void combinePatterns() {
        Map<String, FST> combFSTs = new HashMap<>();
        for (Cluster deriv : derivTrees.values()) {
            for (Cluster.ClusterEdgeIterator iter = deriv.getRootNode().getTransitions(); iter.hasNext(); ) {
                iter.advance();
                combine(iter.getNextNode(), iter.getRelation(), combFSTs);
            }
        }
        this.combFST = FST.disjunctAll(combFSTs.values());
        combFST.determinize();
    }

    /**
     * Recursively combine two adjacent relations in the derivation tree and store them
     * in a map from relations to FSTs.
     * @param current current node in the derivation tree
     * @param incoming the relation to the previous node
     * @param combFSTs the FSTs created so far
     */
    private void combine(Cluster.ClusterNode current, String incoming, Map<String, FST> combFSTs) {
        // go through outgoing edges
        for (Cluster.ClusterEdgeIterator iter = current.getTransitions(); iter.hasNext(); ) {
            iter.advance();
            // create combined label
            String rel = incoming + " ; " + iter.getRelation();
            if (!combFSTs.containsKey(rel)) {
                FST comb = FST.compose(fsts.get(incoming), fsts.get(iter.getRelation()));
                if (comb != null)
                    combFSTs.put(rel, comb);
            }
            combine(iter.getNextNode(), iter.getRelation(), combFSTs);
        }
    }

    /**
     * WORK IN PROGRESS
     * Deduces stems and alternative stems and creates Patterns for clustering these.
     */
    public void getStemPatterns() {
        // get reduce patterns
        FST reducePatterns = reduceCliques();
        // get all stems
        List<String> stems = new ArrayList<>();
        for (String token : allTokens)
            stems.addAll(reducePatterns.apply(token));
        System.out.println(stems.size());

        // get all possible derivation relations for each stem
        Map<String, Set<String>> stemRel = new HashMap<>();
        for (String stem : stems) {
            Set<String> rels = new HashSet<>();
            for (String token : allTokens) {
                FST.Result rel = singleFST.transduce(stem, token);
                if (!rel.isEmpty())
                    rels.add(rel.getRelation());
            }
            if (!rels.isEmpty())
                stemRel.put(stem, rels);
        }System.out.println(stemRel.size());

        // get and count inter-stem patterns
        TObjectIntMap<String> stemPatterns = new TObjectIntHashMap<>();
        for (String stem1 : stemRel.keySet()) {//System.out.print(stem1);
            for (String stem2 : stemRel.keySet()) {//System.out.print(" "+stem2);
                if (Math.abs(stem1.length() - stem2.length()) <= 5
                        && singleFST.transduce(stem1, stem2).isEmpty()
                        && combFST.transduce(stem1, stem2).isEmpty()
                        && Collections.disjoint(stemRel.get(stem1), stemRel.get(stem2))) {
                    String pattern = SequenceAlignment.findPattern(stem1, stem2, 0.5);
                    if (!pattern.isEmpty()) {
                        int weight = stemRel.get(stem1).size() + stemRel.get(stem2).size();
                        stemPatterns.adjustOrPutValue(pattern, weight, weight);
                        stemPatterns.adjustOrPutValue(SequenceAlignment.findPattern(stem2, stem1, 0.5), weight, weight);
                    }
                }
            }//System.out.println();
        }
        // remove patterns that only occurred once
        for (TObjectIntIterator<String> iter = stemPatterns.iterator(); iter.hasNext(); ) {
            iter.advance();
            if (iter.value() == 1)
                iter.remove();
        }

        //TObjectIntMap<String> stemPatterns = SequenceAlignment.getPatterns(stems, clusters.stream().map(Cluster::getRoot).collect(Collectors.toSet()));

        FileIO.writePatterns(stemPatterns, "stempatterns");
    }

    /**
     * Finds all maximal cliques in the clusters, reduces the patterns in
     * between the nodes and stores these patterns in a disjunct FST.
     * @return an FST with the found reduce patterns
     */
    public FST reduceCliques() {
        // map with new patterns
        TObjectIntMap<String> newPatterns = new TObjectIntHashMap<>();

        for (Cluster cluster : clusters) {
            for (List<Cluster.ClusterNode> clique : cluster.getCliques()) {
                for (int i = 0; i < clique.size(); i++) {
                    for (int j = 0; j < clique.size(); j++) {
                        if (i != j) {
                            // get pattern between each pair of nodes in the clique
                            //System.out.print(clique.get(i).getLabel() + " & " + clique.get(j).getLabel() + " : ");
                            String relation = clique.get(i).getRelationTo(clique.get(j));
                            String pattern = (patterns.containsKey(relation))
                                    ? patterns.get(relation)
                                    : SequenceAlignment.findPattern(clique.get(i).getLabel(), clique.get(j).getLabel());
                            // reduce pattern
                            pattern = getInputTape(pattern);
                            // only store non-identity patterns
                            if (StringUtils.count(pattern, ' ') > 1)
                                newPatterns.adjustOrPutValue(pattern, 1, 1);
                        }
                    }
                }
            }
        }

        // store patterns in FST
        Set<Pattern> p = Pattern.label(newPatterns, "stem");
        for (Pattern pattern : p)
            patterns.put(pattern.getLabel(), pattern.getPattern());
        List<FST> newFSTs = PatternToFST.createMultipleFromPatterns(p);
        return FST.disjunctAll(newFSTs);
    }

    /**
     * Only retain the input side of a pattern.
     * @param pattern teh pattern
     * @return the reduced form of the pattern
     */
    private String getInputTape(String pattern) {
        StringBuilder newPattern = new StringBuilder();
        boolean dot = false;
        for (int i = 0; i < pattern.lastIndexOf('/'); i++) {
            if (pattern.charAt(i) == '.') {
                if (!dot) {
                    newPattern.append("./. ");
                    dot = true;
                }
                i += 3;
            }
            else if (pattern.charAt(i) == '-') {
                i += 3;
            }
            else {
                newPattern.append(pattern.charAt(i));
                if (pattern.charAt(i) == '/' && pattern.charAt(i - 1) != '.') {
                    newPattern.append('-');
                    i++;
                }
                dot = false;
            }
        }
        //System.out.println(pattern + "\t" + newPattern.toString());
        return newPattern.append("/"+FST.ANY_POS).toString();
    }

    /**
     * Prints the clusters to a file.
     * @param output the output file
     */
    public void printClusters(String output) {
        // Sort clusters according to size
        sortByClusterSize(clusters);
        // Print
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(output)), Charset.forName("UTF-8")))) {
            int i = 0;
            for (Cluster cluster : clusters){
                writer.println(i + "\t" + cluster.getRoot());
                cluster.printCluster(writer);
                writer.println();
                i++;
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the derivation trees to a file.
     * @param output the output file
     */
    public void printDerivations(String output) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(output)), Charset.forName("UTF-8")))) {
            int i = 0;
            for (Iterator<Map.Entry<String, Cluster>> iter = derivTrees.entrySet().iterator(); iter.hasNext(); ){
                Map.Entry<String, Cluster> cluster = iter.next();
                writer.println(i + "\t" + cluster.getKey());
                cluster.getValue().printWeightsAsLabels(writer, 1);
                writer.println();
                i++;
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /////// COMPARATORS ///////

    private static void sortByClusterSize(List<Cluster> l) {
        TObjectIntMap<String> clusterSize = new TObjectIntHashMap<>();
        for (int i = 0; i < l.size(); i++)
            clusterSize.put(l.get(i).getRoot(), l.get(i).getNodes().size());

        Comparator<Cluster> clusterSizeComparator = (Cluster c1, Cluster c2)
                -> Integer.compare(clusterSize.get(c2.getRoot()), clusterSize.get(c1.getRoot()));

        l.sort(clusterSizeComparator);
    }
}