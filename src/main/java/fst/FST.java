package fst;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import gnu.trove.list.TCharList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TCharArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import util.StringUtils;

import java.util.*;

public class FST {

    // The wildcard char
    public static final char ANY_CHAR = '.';
    // The char representing epsilon/no char
    public static final char NULL_CHAR = '0';
    // The string representing a variable POS
    public static final String ANY_POS = "*";

    // The start state of the fst.FST
    protected FSTState start;

    protected FST(FSTState start) {
        this.start = start;
    }

    /**
     * Creates a transducer from a pattern.
     * @param pattern the pattern
     * @param weight the weight of the pattern
     * @param label the label of the pattern
     */
    public FST(String pattern, int weight, String label) {
        String[] pairs = StringUtils.split(pattern, ' ');
        String pos = pairs[pairs.length-1];
        int split = pos.indexOf('/');
        String fromPos = pos.substring(0, split);
        String toPos = pos.substring(split+1);
        this.start = FSTState.createState(pairs, 0, fromPos, toPos, label, weight);
    }

    public int size() {
        return getStates().size();
    }

    private Set<FSTState> getStates() {
        Set<FSTState> states = new HashSet<>();
        states.add(start);
        start.getStates(states);
        return states;
    }

    /**
     * Checks whether this transducer may produce one string as output when given
     * the other as input.
     * @param s1 the input string
     * @param s2 the output string
     * @return true if the string can be transduced
     */
    public Result transduce(String s1, String s2) {
        int split1 = s1.indexOf(':');
        int split2 = s2.indexOf(':');
        Result res = Result.empty();
        start.transduce(s1.substring(0, split1), s2.substring(0, split2),
                s1.substring(split1+1), s2.substring(split2+1), 0, 0, res);
        return res;
    }

    /**
     * Applies this transducer to a giving input string, yielding an output string.
     * @param s the input string
     * @return the output string
     */
    public Set<String> apply(String s) {
        Set<String> res = new HashSet<>();
        int split1 = s.indexOf(':');
        this.start.apply(s.substring(0, split1), s.substring(split1+1), 0, "", res);
        return res;
    }

    /**
     * @param fst1 an FST
     * @param fst2 another FST
     * @return the disjunction of the two FSTs
     */
    public static FST disjunct(FST fst1, FST fst2) {
        FSTState start = new FSTState();
        start.addEpsilonTransition(fst1.start);
        start.addEpsilonTransition(fst2.start);
        return new FST(start);
    }

    /**
     * @param fsts a list of FSTs
     * @return the disjunction of all FSTs in the list
     */
    public static FST disjunctAll(Collection<FST> fsts) {
        FSTState start = new FSTState();
        for (FST fst : fsts)
            start.addEpsilonTransition(fst.start);
        return new FST(start);
    }

    /**
     * @param fst1 an FST
     * @param fst2 another FST
     * @return the composition of the two FSTs
     */
    public static FST compose(FST fst1, FST fst2) {
        // get the states of the two FSTs, and mappings from integer ids
        // to states and vice versa
        FSTState[] states1 = fst1.getStates().stream().toArray(FSTState[] :: new);
        FSTState[] states2 = fst2.getStates().stream().toArray(FSTState[] :: new);
        TObjectIntMap<FSTState> stateIDs1 = new TObjectIntHashMap<>();
        TObjectIntMap<FSTState> stateIDs2 = new TObjectIntHashMap<>();
        for (int i = 0; i < states1.length; i++)
            stateIDs1.put(states1[i], i);
        for (int j = 0; j < states2.length; j++)
            stateIDs2.put(states2[j], j);
        // the states of the composition
        FSTState[][] newStates = new FSTState[states1.length][states2.length];

        // initialize new states
        for (int i = 0; i < states1.length; i++) {
            for (int j = 0; j < states2.length; j++) {
                if (states1[i].isFinal() && states2[j].isFinal()) {
                    FinalState state1 = (FinalState) states1[i];
                    FinalState state2 = (FinalState) states2[j];
                    newStates[i][j] = FinalState.compose(state1, state2);
                }
                else
                    newStates[i][j] = new FSTState();
            }
        }

        // get transitions between the new states
        for (int i = 0; i < states1.length; i++) {
            for (int j = 0; j < states2.length; j++) {
                // get a new state
                FSTState newState = newStates[i][j];
                // go through all transitions of FST 1
                for (int t1 = 0; t1 < states1[i].outChars.size(); t1++) {
                    // if it is an insertion or deletion, keep it
                    if (states1[i].inChars.get(t1) == NULL_CHAR || states1[i].outChars.get(t1) == NULL_CHAR) {
                        newState.addTransition(newStates[stateIDs1.get(states1[i].nextStates.get(t1))][j],
                                states1[i].inChars.get(t1), states1[i].outChars.get(t1));
                    }
                    // go through all transitions of FST 2
                    for (int t2 = 0; t2 < states2[j].inChars.size(); t2++) {
                        // merge transition if outChar of FST 1 == inChar of FST 2
                        if (states1[i].outChars.get(t1) == states2[j].inChars.get(t2)) {
                            newState.addTransition(newStates
                                            [stateIDs1.get(states1[i].nextStates.get(t1))]
                                            [stateIDs2.get(states2[j].nextStates.get(t2))],
                                    states1[i].inChars.get(t1),
                                    states2[j].outChars.get(t2));
                        }
                        // if it is an insertion or deletion, keep it
                        if (states2[j].inChars.get(t2) == NULL_CHAR || states2[j].outChars.get(t2) == NULL_CHAR) {
                            newState.addTransition(newStates[i][stateIDs2.get(states2[j].nextStates.get(t2))],
                                    states2[j].inChars.get(t2), states2[j].outChars.get(t2));
                        }
                    }
                }
            }
        }

        // return composed FST
        int i = stateIDs1.get(fst1.start);
        int j = stateIDs2.get(fst2.start);
        return new FST(newStates[i][j]);
    }

    /**
     * Removes all epsilon transitions in the FST
     */
    public void removeEpsilonTransitions() {
        // states of the original FST
        FSTState[] statesOld = this.getStates().stream().toArray(FSTState[] :: new);
        // states of the new epsilon-free FST
        FSTState[] statesNew = new FSTState[statesOld.length];
        // states to copy the transitions from
        Set<FSTState>[] copyStates = new Set[statesOld.length];
        // map from original states to list indices
        TObjectIntMap<FSTState> stateIDsOld = new TObjectIntHashMap<>();

        // initialize new states and get copy state sets
        for (int i = 0; i < statesOld.length; i++) {
            FSTState oldState = statesOld[i];
            copyStates[i] = Sets.newHashSet(oldState);
            oldState.getEpsilonClosure(copyStates[i]);
            Iterator<FSTState> finalStates = copyStates[i].stream().filter(FSTState::isFinal).iterator();
            if (finalStates.hasNext()) {
                FinalState finalState = new FinalState();
                while (finalStates.hasNext()) {
                    FinalState fs = (FinalState) finalStates.next();
                    finalState.addFinalStates(fs.fromPOS, fs.toPOS, fs.relation, fs.weight);
                }
                statesNew[i] = finalState;
            }
            else
                statesNew[i] = new FSTState();
            stateIDsOld.put(statesOld[i], i);
        }

        // add transitions
        for (int i = 0; i < statesNew.length; i++) {
            FSTState newState = statesNew[i];
            for (FSTState copyState : copyStates[i]) {
                for (int j = 0; j < copyState.nextStates.size(); j++) {
                    newState.addTransition(statesNew[stateIDsOld.get(copyState.nextStates.get(j))],
                            copyState.inChars.get(j),
                            copyState.outChars.get(j));
                }
            }
        }

        // set new start state
        this.start = statesNew[stateIDsOld.get(this.start)];
    }

    /**
     * Determinizes this FST.
     */
    public void determinize() {
        removeEpsilonTransitions();
        this.start = determinize(Sets.newHashSet(this.start), new HashMap<>());
    }

    /**
     * Creates the determinized equivalent of a set of non-determinized states.
     * @param stateSet the non-determinized state set
     * @param stateMapping the existing determinized states
     * @return the determinized state
     */
    private static FSTState determinize(Set<FSTState> stateSet, Map<Set<FSTState>, FSTState> stateMapping) {
        // create new FSTState only if it does not exist yet
        if (!stateMapping.containsKey(stateSet)) {
            // create new final state in case this state set contains final states
            FinalState finalState = new FinalState();
            // the transitions from this state
            Map<String, Set<FSTState>> trans = new HashMap<>();

            for (FSTState copyState : stateSet) {
                // get information for final state
                if (copyState.isFinal()) {
                    FinalState copyFinal = (FinalState) copyState;
                    finalState.addFinalStates(copyFinal.fromPOS, copyFinal.toPOS,
                            copyFinal.relation, copyFinal.weight);
                }
                // get transitions
                for (int i = 0; i < copyState.nextStates.size(); i++) {
                    String c = copyState.inChars.get(i) + "" + copyState.outChars.get(i);
                    if (trans.containsKey(c))
                        trans.get(c).add(copyState.nextStates.get(i));
                    else
                        trans.put(c, Sets.newHashSet(copyState.nextStates.get(i)));
                }
            }

            // create new state
            FSTState newState = (finalState.fromPOS.isEmpty()) ? new FSTState() : finalState;
            stateMapping.put(stateSet, newState);

            // add transitions while recursively calling determinize() to create
            // the rest of the determinized states
            for (Map.Entry<String, Set<FSTState>> t : trans.entrySet()) {
                newState.addTransition(determinize(t.getValue(), stateMapping),
                        t.getKey().charAt(0), t.getKey().charAt(1));
            }
        }
        // return state
        return stateMapping.get(stateSet);
    }

    private static <T extends Comparable<T>> int firstIndexOf(List<T> list, T element) {
        int x = Collections.binarySearch(list, element);
        if (x >= 0) {
            // get first matching entry
            while (x > 0 && list.get(x-1).equals(element)) x--;
        }
        return x;
    }

    private static int firstIndexOf(TCharList list, char element) {
        int x = list.binarySearch(element);
        if (x >= 0) {
            // get first matching entry
            while (x > 0 && list.get(x-1) == element) x--;
        }
        return x;
    }


    /**
     * A state in an FST.
     */
    private static class FSTState {
        // the transitions to other states
        private TCharList inChars;
        private TCharList outChars;
        private List<FSTState> nextStates;
        // the epsilon transitions
        private List<FSTState> epsilonStates;

        private FSTState() {
            inChars = new TCharArrayList();
            outChars = new TCharArrayList();
            nextStates = new ArrayList<>();
            epsilonStates = new ArrayList<>();
        }

        /**
         * Creates the FST for a given pattern.
         * @param pairs the pattern
         * @param i the current position in the pattern
         * @param fromPOS the input POS of the pattern
         * @param toPOS the output POS of the pattern
         * @param relation the label of the pattern
         * @param weight the weight of the pattern
         * @return the start state of the FST for the given pattern starting at i
         */
        static FSTState createState(String[] pairs, int i, String fromPOS, String toPOS, String relation, int weight) {
            if (i == pairs.length-1)
                return new FinalState(fromPOS, toPOS, relation, weight);
            else {
                FSTState state = new FSTState();
                FSTState next = createState(pairs, i+1, fromPOS, toPOS, relation, weight);
                if (pairs[i].equals("./.")) {
                    state.addTransition(next, ANY_CHAR, ANY_CHAR);
                    next.addTransition(next, ANY_CHAR, ANY_CHAR);
                }
                else {
                    char inChar = pairs[i].charAt(0);
                    char outChar = pairs[i].charAt(2);
                    state.addTransition(next, ((inChar == '-') ? NULL_CHAR : inChar), ((outChar == '-') ? NULL_CHAR : outChar));
                }
                return state;
            }
        }

        /**
         * @return true if the state is accepting, false if not
         */
        boolean isFinal() {
            return false;
        }

        /**
         * Collects all states that can be reached from this state.
         * @param states the states that were already found
         */
        void getStates(Set<FSTState> states) {
            Set<FSTState> next = new HashSet<>();
            next.addAll(nextStates);
            next.addAll(epsilonStates);
            next.removeAll(states);
            states.addAll(next);
            for (FSTState state : next) {
                state.getStates(states);
            }
        }

        /**
         * Collects the epsilon closure, i.e. all states that can be reached
         * from this state via epsilon transitions.
         * @param states the states that were already found
         */
        void getEpsilonClosure(Set<FSTState> states) {
            for (FSTState state : epsilonStates) {
                if (!states.contains(state)) {
                    states.add(state);
                    state.getEpsilonClosure(states);
                }
            }
        }

        void addTransition(FSTState to, char inChar, char outChar) {
            int i = inChars.binarySearch(inChar);
            if (i < 0) i = -(i+1);
            inChars.insert(i, inChar);
            outChars.insert(i, outChar);
            nextStates.add(i, to);
        }

        void addEpsilonTransition(FSTState to) {
            epsilonStates.add(to);
        }

        void transduce(String s1, String s2, String pos1, String pos2, int i1, int i2, Result res) {
            if (i1 < s1.length() || i2 < s2.length()) {

                char inChar = (i1 < s1.length()) ? s1.charAt(i1) : NULL_CHAR;
                char outChar = (i2 < s2.length()) ? s2.charAt(i2) : NULL_CHAR;

                if (!nextStates.isEmpty()) {
                    // insertion
                    if (outChar != NULL_CHAR) {
                        int x = firstIndexOf(inChars, NULL_CHAR);
                        if (x >= 0) {
                            // check all matching entries
                            while (x < inChars.size() && inChars.get(x) == NULL_CHAR) {
                                if (outChars.get(x) == outChar)
                                    nextStates.get(x).transduce(s1, s2, pos1, pos2, i1, i2 + 1, res);
                                x++;
                            }
                        }
                    }

                    // substitution & deletion
                    if (inChar != NULL_CHAR) {
                        int x = firstIndexOf(inChars, inChar);
                        if (x >= 0) {
                            // check all matching entries
                            while (x < inChars.size() && inChars.get(x) == inChar) {
                                if (outChars.get(x) == NULL_CHAR) //deletion
                                    nextStates.get(x).transduce(s1, s2, pos1, pos2, i1 + 1, i2, res);
                                else if (outChars.get(x) == outChar) //substitution
                                    nextStates.get(x).transduce(s1, s2, pos1, pos2, i1 + 1, i2 + 1, res);
                                x++;
                            }
                        }
                    }

                    // same char transition
                    if (inChar == outChar && inChar != NULL_CHAR) {
                        int x = firstIndexOf(inChars, ANY_CHAR);
                        if (x >= 0) {
                            // check all matching entries
                            while (x < inChars.size() && inChars.get(x) == ANY_CHAR) {
                                nextStates.get(x).transduce(s1, s2, pos1, pos2, i1 + 1, i2 + 1, res);
                                x++;
                            }
                        }
                    }
                }
            }

            // epsilon transition
            for (FSTState next : epsilonStates) {
                next.transduce(s1, s2, pos1, pos2, i1, i2, res);
            }
        }

        void apply(String s, String pos, int i, String prefix, Set<String> results) {
            if (!nextStates.isEmpty()) {
                char inChar = (i < s.length()) ? s.charAt(i) : NULL_CHAR;

                // insertion
                int x = firstIndexOf(inChars, NULL_CHAR);
                if (x >= 0) {
                    // check all matching entries
                    while (x < inChars.size() && inChars.get(x) == NULL_CHAR) {
                        nextStates.get(x).apply(s, pos, i, prefix+outChars.get(x), results);
                        x++;
                    }
                }

                if (i < s.length()) {

                    // substitution & deletion
                    if (inChar != NULL_CHAR) {
                        x = firstIndexOf(inChars, inChar);
                        if (x >= 0) {
                            // check all matching entries
                            while (x < inChars.size() && inChars.get(x) == inChar) {
                                if (outChars.get(x) == NULL_CHAR) //deletion
                                    nextStates.get(x).apply(s, pos, i+1, prefix, results);
                                else //substitution
                                    nextStates.get(x).apply(s, pos, i+1, prefix+outChars.get(x), results);
                                x++;
                            }
                        }

                        // same char transition
                        x = firstIndexOf(inChars, ANY_CHAR);
                        if (x >= 0) {
                            // check all matching entries
                            while (x < inChars.size() && inChars.get(x) == ANY_CHAR) {
                                nextStates.get(x).apply(s, pos, i+1, prefix+s.charAt(i), results);
                                x++;
                            }
                        }
                    }
                }
            }

            // epsilon transition
            for (FSTState next : epsilonStates) {
                next.apply(s, pos, i, prefix, results);
            }
        }
    }

    /**
     * An accepting state in an FST.
     */
    private static class FinalState extends FSTState {
        // Pattern properties
        private List<String> fromPOS;
        private List<String> toPOS;
        private List<String> relation;
        private TIntList weight;

        FinalState() {
            super();
            this.fromPOS = new ArrayList<>();
            this.toPOS = new ArrayList<>();
            this.relation = new ArrayList<>();
            this.weight = new TIntArrayList();
        }

        FinalState(String fromPOS, String toPOS, String relation, int weight) {
            this();
            addFinalState(fromPOS, toPOS, relation, weight);
        }

        FinalState(List<String> fromPOS, List<String> toPOS, List<String> relation, TIntList weight) {
            this();
            addFinalStates(fromPOS, toPOS, relation, weight);
        }

        void addFinalState(String fromPOS, String toPOS, String relation, int weight) {
            int x = Collections.binarySearch(this.fromPOS, fromPOS);
            if (x < 0) x = -(x+1);
            this.fromPOS.add(x, fromPOS);
            this.toPOS.add(x, toPOS);
            this.relation.add(x, relation);
            this.weight.insert(x, weight);
        }

        void addFinalStates(List<String> fromPOS, List<String> toPOS, List<String> relation, TIntList weight) {
            for (int i = 0; i < fromPOS.size(); i++) {
                int x = Collections.binarySearch(this.fromPOS, fromPOS.get(i));
                if (x < 0) x = -(x + 1);
                this.fromPOS.add(x, fromPOS.get(i));
                this.toPOS.add(x, toPOS.get(i));
                this.relation.add(x, relation.get(i));
                this.weight.insert(x, weight.get(i));
            }
        }

        /**
         * @return true because this is an accepting state
         */
        @Override
        boolean isFinal() {
            return true;
        }

        @Override
        void transduce(String s1, String s2, String pos1, String pos2, int i1, int i2, Result res) {
            // get result if the POS of both strings match
            if (i1 == s1.length() && i2 == s2.length()) {
                if (pos1.equals(ANY_POS)) {
                    for (int x = 0; x < fromPOS.size(); x++) {
                        if ((pos2.equals(toPOS.get(x)) || pos2.equals(ANY_POS)) && res.weight < weight.get(x)) {
                            res.relation = this.relation.get(x);
                            res.weight = this.weight.get(x);
                        }
                    }
                }
                else {
                    updateResultFor(pos1, pos2, res);
                    updateResultFor(ANY_POS, pos2, res);
                }
            }
            // continue search in neighboring states
            super.transduce(s1, s2, pos1, pos2, i1, i2, res);
        }

        private void updateResultFor(String pos1, String pos2, Result res) {
            int x = firstIndexOf(fromPOS, pos1);
            if (x >= 0) {
                // check all matching entries
                while (x < fromPOS.size() && fromPOS.get(x).equals(pos1)) {
                    if ((pos2.equals(toPOS.get(x)) || pos2.equals(ANY_POS)) && res.weight < weight.get(x)) {
                        res.relation = this.relation.get(x);
                        res.weight = this.weight.get(x);
                    }
                    x++;
                }
            }
        }

        @Override
        void apply(String s, String pos, int i, String prefix, Set<String> results) {
            // get result if the POS of the string matches
            if (i == s.length()) {
                if (pos.equals(ANY_POS)) {
                    for (String to : toPOS)
                        results.add(prefix + ":" + to);
                }
                else {
                    updateOutputFor(pos, prefix, results);
                    updateOutputFor(ANY_POS, prefix, results);
                }
            }
            // continue search in neighboring states
            super.apply(s, pos, i, prefix, results);
        }

        private void updateOutputFor(String pos, String prefix, Set<String> results) {
            int x = firstIndexOf(fromPOS, pos);
            if (x >= 0) {
                // check all matching entries
                while (x < fromPOS.size() && fromPOS.get(x).equals(pos)) {
                    results.add(prefix + ":" + toPOS);
                    x++;
                }
            }
        }

        /**
         * Creates the composition of two final states by matching all pos pairs
         * where toPOS of state1 equals fromPOS of state2.
         * @param state1 a final state
         * @param state2 another final state
         * @return the composition of these two final states
         */
        static FSTState compose(FinalState state1, FinalState state2) {
            FinalState newFinal = new FinalState();
            // go through all output POS of state1
            for (int x = 0; x < state1.toPOS.size(); x++) {
                if (state1.toPOS.get(x).equals(ANY_POS)) {
                    for (int y = 0; y < state2.fromPOS.size(); y++) {
                        newFinal.addFinalState(state1.fromPOS.get(x), state2.toPOS.get(y),
                                state1.relation.get(x) + " ; " + state2.relation.get(y),
                                Math.min(state1.weight.get(x), state2.weight.get(y)) / 2);
                    }
                }
                else {
                    updateFinalStateFor(state1.fromPOS.get(x), state1.toPOS.get(x), state1.relation.get(x),
                            state1.weight.get(x), state2, newFinal);
                    updateFinalStateFor(state1.fromPOS.get(x), ANY_POS, state1.relation.get(x),
                            state1.weight.get(x), state2, newFinal);
                }
            }
            if (newFinal.fromPOS.isEmpty()) return new FSTState();
            return newFinal;
        }

        private static void updateFinalStateFor(String from, String to, String rel, int w,
                                                FinalState state2, FinalState newFinal) {
            int y = firstIndexOf(state2.fromPOS, to);
            if (y >= 0) {
                while (y < state2.fromPOS.size() && state2.fromPOS.get(y).equals(to)) {
                    newFinal.addFinalState(from, state2.toPOS.get(y),
                            rel + " ; " + state2.relation.get(y),
                            Math.min(w, state2.weight.get(y)) / 2);
                    y++;
                }
            }
        }
    }

    /**
     * The result of searching for a relation that holds between two strings.
     */
    public static class Result {

        private String relation;
        private int weight;

        protected Result(String relation, int weight) {
            this.relation = relation;
            this.weight = weight;
        }

        protected static Result empty() {
            return new Result("", 0);
        }

        public boolean isEmpty() {
            return weight == 0 && relation.isEmpty();
        }

        public String getRelation() {
            return relation;
        }

        public int getWeight() {
            return weight;
        }
    }




    ////////// TEST CODE ///////////
    public static void main(String[] args) {
        FST raa = new FST("-/r -/a -/a ./. V/V", 10, "raa+");
        FST fel = new FST("-/f -/e -/l ./. V/V", 7, "fel+");
        FST el = new FST("-/e -/l ./. V/V", 8, "el+");
        FST oo = new FST("./. -/o -/o V/N", 8, "+oo");
        FST oodik = new FST("./. -/o -/o -/d -/i -/k V/V", 8, "+oodik");
        FST fel_oo = new FST("-/f -/e -/l ./. -/o -/o V/N", 8, "fel+,+oo");
        FST disjunct = FST.disjunctAll(Lists.newArrayList(raa, fel, el, oo, oodik, fel_oo));

        String eer = "eer:V";
        String teer = "teer:V";
        String aateer = "aateer:V";
        String raaeer = "raaeer:V";
        String feleer = "feleer:V";
        String eleer = "eleer:V";
        String ad = "ad:V";
        String felad = "felad:V";
        String adoo = "adoo:N";
        String feladoo = "feladoo:N";
        String adoodik = "adoodik:V";

        System.out.println("> "+disjunct.transduce(eer, raaeer).getRelation());
        System.out.println("> "+disjunct.transduce(teer, aateer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, feleer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, eleer).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoo).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoodik).getRelation());
        System.out.println("> "+disjunct.transduce(ad, feladoo).getRelation());

        disjunct.removeEpsilonTransitions();

        System.out.println("> "+disjunct.transduce(eer, raaeer).getRelation());
        System.out.println("> "+disjunct.transduce(teer, aateer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, feleer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, eleer).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoo).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoodik).getRelation());
        System.out.println("> "+disjunct.transduce(ad, feladoo).getRelation());

        disjunct.determinize();

        System.out.println("> "+disjunct.transduce(eer, raaeer).getRelation());
        System.out.println("> "+disjunct.transduce(teer, aateer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, feleer).getRelation());
        System.out.println("> "+disjunct.transduce(eer, eleer).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoo).getRelation());
        System.out.println("> "+disjunct.transduce(ad, adoodik).getRelation());
        System.out.println("> "+disjunct.transduce(ad, feladoo).getRelation());

        for (String res : disjunct.apply(eer))
            System.out.println(res);
    }
}
