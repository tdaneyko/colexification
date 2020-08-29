package cluster;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.PrintWriter;
import java.util.*;

public class Cluster {

    private ClusterNode root;

    public Cluster(ClusterNode root) {
        this.root = root;
    }

    public String getRoot() {
        return root.label;
    }

    public ClusterNode getRootNode() {
        return root;
    }

    public boolean hasEdges() {
        return !root.neighbors.isEmpty();
    }

    public List<ClusterNode> getNodes() {
        List<ClusterNode> nodes = new ArrayList<>();
        nodes.add(root);
        root.addChildren(nodes);
        return nodes;
    }

    /**
     * Calculates the best root node, i.e. the one with the shortest
     * label and the most outgoing edges.
     * @return the label of the new root
     */
    public String reroot() {
        ClusterNode newRoot = root;
        for (ClusterNode node : getNodes()) {
            if (node.label.length() < newRoot.label.length()
                    || (node.label.length() == newRoot.label.length()
                    && node.neighbors.size() > newRoot.neighbors.size())) {
                newRoot = node;
            }
        }
        this.root = newRoot;
        return root.label;
    }

    /**
     * Get all maximal cliques in this cluster.
     * @return a list of cliques
     */
    public List<List<ClusterNode>> getCliques() {
        List<List<ClusterNode>> cliques = new ArrayList<>();
        List<ClusterNode> nodes = getNodes();
        Collections.sort(nodes);
        List<ClusterNode> finished = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            ClusterNode node = nodes.get(i);
            getClique(Lists.newArrayList(node), new ArrayList<>(), finished, cliques);
            finished.add(node);
        }

        return cliques;
    }

    /**
     * Extend a clique.
     * @param currentClique the current clique
     * @param alreadyPaired the nodes that are already contained in a maximal clique
     *                      starting from the origin of the current clique
     * @param finished the nodes for which all cliques have already been calculated
     * @param results the collection of maximal cliques in this cluster
     */
    private void getClique(List<ClusterNode> currentClique, List<ClusterNode> alreadyPaired,
                           List<ClusterNode> finished, List<List<ClusterNode>> results) {
        // get candidates
        List<ClusterNode> candidates = getSharedNeighbors(currentClique);
        // if there are no more candidates and the clique contains more
        // than 2 nodes, add them to the results
        if (candidates.isEmpty()) {
            if (currentClique.size() > 2)
                results.add(currentClique);
            alreadyPaired.addAll(currentClique);
            Collections.sort(alreadyPaired);
        }
        // else continue searching for each not yet processed candidate
        else {
            for (ClusterNode candidate : candidates) {
                if (Collections.binarySearch(finished, candidate) < 0
                        && Collections.binarySearch(alreadyPaired, candidate) < 0) {
                    List<ClusterNode> newClique = new ArrayList<>(currentClique);
                    newClique.add(candidate);
                    getClique(newClique, alreadyPaired, finished, results);
                }
            }
        }
    }

    /**
     * Gets all nodes that are connected to all members of a given
     * collection of nodes.
     * @param nodes the nodes in question
     * @return the shared neighbors
     */
    private List<ClusterNode> getSharedNeighbors(List<ClusterNode> nodes) {
        // sort from fewest to most neighbors for faster intersecting
        List<ClusterNode> nodesCopy = new ArrayList<>(nodes);
        nodesCopy.sort(Comparator.comparingInt(n -> n.neighbors.size()));
        List<ClusterNode> result = nodesCopy.get(0).neighbors;
        // repeatedly intersect all neighbors of the nodes
        for (int i = 1; i < nodesCopy.size(); i++)
            result = intersect(result, nodesCopy.get(i).neighbors);
        return result;
    }

    /**
     * Intersect two sorted lists.
     * @param l1 list 1
     * @param l2 list 2
     * @param <T> any object
     * @return the intersection
     */
    private <T extends Comparable<T>> List<T> intersect(List<T> l1, List<T> l2) {
        if (l2 == null)
            return l1;

        List<T> intersection = new ArrayList<>();

        int i = 0;
        int j = 0;
        while (i != l1.size() && j != l2.size()) {
            if (l1.get(i).equals(l2.get(j))) {
                intersection.add(l1.get(i));
                i++;
                j++;
            }
            else if (l1.get(i).compareTo(l2.get(j)) < 0)
                i++;
            else
                j++;
        }

        return intersection;
    }

    /**
     * Cleans the clusters, i.e. removes all unnecessary connections between nodes.
     * Only the shortest path from the root to each node is retained, and if there
     * are two equally short paths, the one ending in the thickest connection is
     * kept.
     */
//    public void clean() {
//        // create new node for each of the existing nodes
//        List<ClusterNode> nodes = getNodes();
//        Collections.sort(nodes);
//        ClusterNode[] newNodes = new ClusterNode[nodes.size()];
//        for (int i = 0; i < newNodes.length; i++)
//            newNodes[i] = new ClusterNode(nodes.get(i).label);
//
//        // the nodes that were already checked
//        TIntSet visited = new TIntHashSet();
//        // the nodes at the respective level
//        List<TIntList> levels = Lists.newArrayList(new TIntArrayList(), new TIntArrayList());
//        // the nodes which connect to the nodes in levels
//        List<TIntList> from = Lists.newArrayList(new TIntArrayList(), new TIntArrayList());
//        // the weights of the edges to the nodes in levels
//        List<TIntList> weights = Lists.newArrayList(new TIntArrayList(), new TIntArrayList());
//
//        // initialize with root node
//        int rootID = Collections.binarySearch(nodes, root);
//        levels.get(0).add(rootID);
//
//        // proceed while there are still nodes left to visit
//        for (int lvl = 0; !levels.get(lvl).isEmpty() && visited.size() < nodes.size(); lvl++) {
//            // the nodes at the next level
//            if (lvl == levels.size()-2) {
//                levels.add(new TIntArrayList());
//                from.add(new TIntArrayList());
//                weights.add(new TIntArrayList());
//            }
//            TIntList current = levels.get(lvl);
//
//            // remove spurious skip nodes
//            for (int i = 0; i < current.size(); i++) {
//                int fromID = current.get(i);
//                if (visited.contains(fromID)) {
//                    newNodes[from.get(lvl).removeAt(i)].removeTransition(newNodes[fromID]);
//                    current.removeAt(i);
//                    weights.get(lvl).removeAt(i);
//                    i--;
//                }
//            }
//
//            // add current nodes to visited
//            visited.addAll(current);
//
//            // go through nodes at current level
//            for (int i = 0; i < current.size(); i++) {
//                // get a pair of original and corresponding new node
//                int fromID = current.get(i);
//                ClusterNode originalCurrent = nodes.get(fromID);
//                ClusterNode newCurrent = newNodes[fromID];
//                // go through all edges from the current node
//                for (int n = 0; n < originalCurrent.neighbors.size(); n++) {
//                    // get a neighbor
//                    ClusterNode to = originalCurrent.neighbors.get(n);
//                    int toID = Collections.binarySearch(nodes, to);
//                    // if the node this edge leads to has not yet been processed
//                    if (!visited.contains(toID)) {
//                        int skip = (originalCurrent.relations.get(n).contains(" ; ")) ? 2 : 1;
//                        // get weight of edge to neighbor
//                        int weight = originalCurrent.weights.get(n);
//                        // search for the neighbor in next
//                        int j = levels.get(lvl + skip).indexOf(toID);
//                        // add node if it is new
//                        if (j < 0) {
//                            from.get(lvl + skip).add(fromID);
//                            levels.get(lvl + skip).add(toID);
//                            weights.get(lvl + skip).add(weight);
//                            // add edge to node
//                            if (weight >= 0)
//                                newCurrent.addTransition(newNodes[toID], originalCurrent.relations.get(n), Math.abs(weight), false);
//                            else
//                                newCurrent.addReverse(newNodes[toID], originalCurrent.relations.get(n), Math.abs(weight));
//                        }
//                        // else only add edge if this edge is thicker than the one
//                        // already existing to this node
//                        else if (weight > weights.get(lvl + skip).get(j)) {
//                            // remove old edge
//                            newNodes[from.get(lvl + skip).get(j)].removeTransition(newNodes[toID]);
//                            // update from and weightTo
//                            from.get(lvl + skip).set(j, fromID);
//                            weights.get(lvl + skip).set(j, weight);//Math.min(weightTo.get(j), weight));
//                            // add edge to node
//                            if (weight >= 0)
//                                newCurrent.addTransition(newNodes[toID], originalCurrent.relations.get(n), Math.abs(weight), false);
//                            else
//                                newCurrent.addReverse(newNodes[toID], originalCurrent.relations.get(n), Math.abs(weight));
//                        }
//                    }
//                }
//            }
//        }
//        // replace cluster by cleaned edges
//        root = newNodes[rootID];
//    }

    /**
     * WORK IN PROGRESS
     * Cleans the clusters, i.e. only keeps the path to a node with the thickest
     * minimal edge.
     */
    public void clean() {
        List<ClusterNode> nodes = new ArrayList<>(getNodes());
        Collections.sort(nodes);
        TIntList[] paths = new TIntList[nodes.size()];
        int[] minWeights = new int[nodes.size()];
        int[] minPos = new int[nodes.size()];
        int r = Collections.binarySearch(nodes, root);
        paths[r] = new TIntArrayList();
        clean(root, new TIntArrayList(), new HashSet<>(), Integer.MAX_VALUE, -1, nodes, paths, minWeights, minPos);

        ClusterNode newRoot = new ClusterNode(root.label);
        ClusterNode[] newNodes = new ClusterNode[nodes.size()];
        for (int i = 0; i < newNodes.length; i++)
            newNodes[i] = new ClusterNode(nodes.get(i).label);

//        for (TIntList path : paths)
//            putPath(newRoot, root, path, 0, newNodes);
        for (int i = 0; i < paths.length; i++) {
//            System.out.print(root.label);
//            for (int j = 0; j < paths[i].size(); j++)
//                System.out.print(" -> " + newNodes[paths[i].get(j)].label);
//            System.out.println(" " + minWeights[i]);
            putPath(newRoot, root, paths[i], 0, newNodes);
        }

        this.root = newRoot;
    }

    private void clean(ClusterNode currentNode, TIntList currentPath, Set<ClusterNode> visited, int previousMin, int previosMinIdx,
                       List<ClusterNode> nodes, TIntList[] paths, int[] minWeights, int[] minPos) {
        if (!(currentNode.equals(root) && !visited.isEmpty())) {
            Set<ClusterNode> newVisited = new HashSet<>(visited);
            newVisited.add(currentNode);
            for (int n = 0; n < currentNode.neighbors.size(); n++) {
                ClusterNode neighbor = currentNode.neighbors.get(n);
                if (!visited.contains(neighbor)) {
                    int currentWeight = Math.abs(currentNode.weights.get(n));
                    int currentMin = Math.min(previousMin, currentWeight);
                    int i = Collections.binarySearch(nodes, neighbor);
                    int dist = (currentNode.relations.get(n).contains(" ; ")) ? 2 : 1;
                    int minIdx = (currentMin == previousMin) ? previosMinIdx : currentPath.size() - 1 + dist;
                    int diff = (paths[i] == null) ? Integer.MIN_VALUE : currentPath.size()+dist - paths[i].size();
                    if (currentMin > minWeights[i]
                            || (currentMin == minWeights[i] && (diff < 0 ||
                            (diff == 0 && (paths[i].contains(-1) || minIdx > minPos[i]))))) {
                        TIntList bestPath = new TIntArrayList(currentPath);
                        if (dist == 2) bestPath.add(-1);
                        bestPath.add(i);
                        paths[i] = bestPath;
                        minWeights[i] = currentMin;
                        minPos[i] = minIdx;
                        clean(neighbor, bestPath, newVisited, currentMin, minIdx, nodes, paths, minWeights, minPos);
                    }
                }
            }
        }
    }

    private void putPath(ClusterNode newCurrent, ClusterNode originalCurrent, TIntList path, int i, ClusterNode[] newNodes) {
        if (i < path.size()) {
            if (path.get(i) == -1)
                putPath(newCurrent, originalCurrent,path, i+1, newNodes);
            else {
                ClusterNode nextNode = newNodes[path.get(i)];
                int o = Collections.binarySearch(originalCurrent.neighbors, nextNode);
                newCurrent.addTransition(nextNode, originalCurrent.relations.get(o), originalCurrent.weights.get(o));
                putPath(nextNode, originalCurrent.neighbors.get(o), path, i+1, newNodes);
            }
        }
    }

    public void countDerivations(Cluster derTree) {
        countDerivations(derTree, false);
    }

    public void countDerivations(Cluster derTree, boolean withComb) {
        root.getDerivations(derTree.root, Sets.newHashSet(root.label), withComb);
    }

    public void printCluster(PrintWriter writ) {
        printCluster(writ, 0);
    }

    public void printCluster(PrintWriter writ, int threshold) {
        for (ClusterNode node : getNodes())
            writ.print(node.edges(threshold));
    }

    public void printWeightsAsLabels(PrintWriter writ) {
        printWeightsAsLabels(writ, 0);
    }

    public void printWeightsAsLabels(PrintWriter writ, int threshold) {
        for (ClusterNode node : getNodes())
            writ.print(node.edgesWithWeightsAsLabels(threshold));
    }


    /**
     * A node in a cluster.
     */
    public static class ClusterNode implements Comparable<ClusterNode> {

        private String label;

        private List<ClusterNode> neighbors;
        private List<String> relations;
        private TIntList weights;

        public ClusterNode(String label) {
            this.label = label;

            neighbors = new ArrayList<>();
            relations = new ArrayList<>();
            weights = new TIntArrayList();
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getRelationTo(ClusterNode other) {
            int i = Collections.binarySearch(neighbors, other);
            if (i < 0) return "";
            else return relations.get(i);
        }

        public Set<String> getRelations() {
            return new HashSet<>(relations);
        }

        public void addTransition(ClusterNode to, String relation, int weight) {
            addTransition(to, relation, weight, false);
        }

        public void addTransition(ClusterNode to, String relation, int weight, boolean reverse) {
            int i = Collections.binarySearch(neighbors, to);

            if (i >= 0 && weights.get(i) < weight) {
                neighbors.set(i, to);
                relations.set(i, relation);
                weights.set(i, weight);
            }
            else if (i < 0) {
                i = -(i + 1);
                neighbors.add(i, to);
                relations.add(i, relation);
                weights.insert(i, weight);
                if (reverse)
                    to.addReverse(this, relation, weight);
            }
        }

        public void removeTransition(ClusterNode to) {
            int i = Collections.binarySearch(neighbors, to);
            if (i >= 0) {
                neighbors.remove(i);
                relations.remove(i);
                weights.removeAt(i);
            }
        }

        public void addReverse(ClusterNode to, String relation, int weight) {
            int i = Collections.binarySearch(neighbors, to);
            if (i < 0) {
                i = -(i+1);
                neighbors.add(i, to);
                relations.add(i, relation);
                weights.insert(i, -weight);
            }
        }

        private void addChildren(List<ClusterNode> nodes) {
            for (ClusterNode node : neighbors) {
                if (!nodes.contains(node)) {
                    nodes.add(node);
                    node.addChildren(nodes);
                }
            }
        }

        /**
         * Count outgoing transitions of this node in a derivation tree.
         * @param currentNode the equivalent to this node in the derivation tree
         * @param visited the labels of the nodes already visited
         * @param withComb true if combination patterns should also be included
         *                 in the derivation tree
         */
        public void getDerivations(ClusterNode currentNode, Set<String> visited, boolean withComb) {
            // label of current node minus the pos tag
            String currentPrefix = currentNode.label.substring(0, currentNode.label.lastIndexOf(':'));
            // go through all neighboring tokens
            for (int i = 0; i < neighbors.size(); i++) {
                ClusterNode nextOriginal = neighbors.get(i);
                String nextToken = nextOriginal.label;
                if (!visited.contains(nextToken)) {
                    visited.add(nextToken);
                    String rel = relations.get(i);
                    if (withComb || !rel.contains(" ; ")) {
                        // count edge relation
                        ClusterNode nextNode;
                        int next = currentNode.relations.indexOf(rel);
                        if (next >= 0) {
                            currentNode.weights.set(next, currentNode.weights.get(next) + 1);
                            nextNode = currentNode.neighbors.get(next);
                        } else {
                            nextNode = new ClusterNode(currentPrefix + ';' + rel + nextToken.substring(nextToken.indexOf(':')));
                            currentNode.addTransition(nextNode, rel, 1);
                        }
                        // proceed with the next node
                        nextOriginal.getDerivations(nextNode, visited, withComb);
                    }
                }
            }
        }

        public ClusterEdgeIterator getTransitions() {
            return new ClusterEdgeIterator(this);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ClusterNode) {
                ClusterNode o = (ClusterNode) other;
                return this.label.equals(o.label);
            }
            return false;
        }

        @Override
        public String toString() {
            return edges(0);
        }

        public String edges(int threshold) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < this.neighbors.size(); i++) {
                if (Math.abs(this.weights.get(i)) > threshold) {
                    s.append(this.label).append('\t');
                    if (this.weights.get(i) < 0)
                        s.append(this.relations.get(i))
                                .append(".rev\t")
                                .append(Math.abs(this.weights.get(i)));
                    else
                        s.append(this.relations.get(i))
                                .append('\t')
                                .append(this.weights.get(i));
                    s.append('\t')
                            .append(this.neighbors.get(i).label)
                            .append('\n');
                }
            }
            return s.toString();
        }

        public String edgesWithWeightsAsLabels(int threshold) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < neighbors.size(); i++) {
                if (this.weights.get(i) > threshold) {
                    s.append(this.label)
                            .append('\t')
                            .append(this.relations.get(i))
                            .append(" (")
                            .append(this.weights.get(i))
                            .append(")\t100\t")
                            .append(this.neighbors.get(i).label)
                            .append('\n');
                }
            }
            return s.toString();
        }

        @Override
        public int compareTo(ClusterNode o) {
            return this.label.compareTo(o.label);
        }
    }


    public static class ClusterEdgeIterator {

        private ClusterNode node;
        private int i;

        protected ClusterEdgeIterator(ClusterNode node) {
            this.node = node;
            this.i = -1;
        }

        public boolean hasNext() {
            return i+1 < node.neighbors.size();
        }

        public void advance() {
            i++;
        }

        public String getRelation() {
            return (i < 0) ? "" : node.relations.get(i);
        }

        public int getWeight() {
            return (i < 0) ? -1 : node.weights.get(i);
        }

        public ClusterNode getNextNode() {
            return (i < 0) ? null : node.neighbors.get(i);
        }
    }
}
