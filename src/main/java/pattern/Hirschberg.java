package pattern;

import java.util.Stack;

public class Hirschberg {

    private static final int SAME_COST = 1;
    private static final int SUB_COST = -1;
    private static final int INDEL_COST = -1;

    private Stack<Character> from;
    private Stack<Character> to;

    private String s1;
    private String s2;
    private String r1;
    private String r2;

    public Hirschberg() {
        from = new Stack<>();
        to = new Stack<>();
    }

    public String findPattern(String a, String b) {
        int split1 = a.indexOf(':');
        int split2 = b.indexOf(':');
        s1 = a.substring(0, split1);
        s2 = b.substring(0, split2);
        r1 = new StringBuilder(s1).reverse().toString();
        r2 = new StringBuilder(s2).reverse().toString();

        hirschberg(0, s1.length(), 0, s2.length());

        StringBuilder res = new StringBuilder(Math.max(s1.length(),s2.length())*4+10);
        int equal = 0;
        boolean justAppended = false;

        while (!from.isEmpty()) {
            char fromC = from.pop();
            char toC = to.pop();

            if (!to.isEmpty() && fromC == '-' && to.peek() == '-') {
                fromC = from.pop();
                to.pop();
            }
            if (!from.isEmpty() && toC == '-' && from.peek() == '-') {
                toC = to.pop();
                from.pop();
            }

            if (fromC == toC) {
                if (!justAppended) {
                    res.append("./. ");
                    justAppended = true;
                }
                equal++;
            }
            else {
                res.append(fromC).append('/').append(toC).append(" ");
                justAppended = false;
            }
        }

        if (equal > Math.min(s1.length(), s2.length())/3.0)
            return res.append(a.substring(split1+1)).append("/").append(b.substring(split2+1)).toString();
        return "";
    }

    private void hirschberg(int a, int x, int b, int y) {
        if (a >= x)
            for (int j = y-1; j >= b; j--) {
                from.push('-');
                to.push(s2.charAt(j));
            }
        else if (b >= y)
            for (int i = x-1; i >= a; i--) {
                from.push(s1.charAt(i));
                to.push('-');
            }
        else {
            int s1len = x-a;
            int s2len = y-b;
            if (s1len == 1 || s2len == 1)
                needlemanWunsch(a, x, b, y);
            else {
                int s1mid = a+s1len/2;
                int s2mid = b+maxVal(nwScore(a, s1mid, b, y),
                        nwScore(r1.length()-x, r1.length()-s1mid,
                                r2.length()-y, r2.length()-b));
                hirschberg(s1mid, x, s2mid, y);
                hirschberg(a, s1mid, b, s2mid);
            }
        }
    }

    private int[] nwScore(int a, int x, int b, int y) {
        int s1l = x-a;
        int s2l = y-b;
        int[] cur = new int[s2l+1];
        int[] prev = new int[s2l+1];

        for (int i = 0; i < cur.length; i++)
            cur[i] = INDEL_COST*i;

        for (int i = 1; i <= s1l; i++) {
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
            cur[0] = prev[0]+INDEL_COST;
            for (int j = 1; j <= s2l; j++) {
                int cost = (s1.charAt(a+i-1) == s2.charAt(b+j-1)) ? SAME_COST : SUB_COST;
                cur[j] = Math.max(Math.max(
                        prev[j]+INDEL_COST,
                        cur[j - 1]+INDEL_COST),
                        prev[j - 1] + cost);
            }
        }
        return cur;
    }

    private void needlemanWunsch(int a, int x, int b, int y) {
        int s1l = x-a;
        int s2l = y-b;

        int[][] table = new int[s1l+1][s2l+1];

        for (int i = 0; i <= s1l; i++) {
            table[i][0] = INDEL_COST*i;
        }
        for (int j = 0; j <= s2l; j++) {
            table[0][j] = INDEL_COST*j;
        }

        for (int i = 1; i <= s1l; i++) {
            for (int j = 1; j <= s2l; j++) {
                int cost = (s1.charAt(a+i-1) == s2.charAt(b+j-1)) ? SAME_COST : SUB_COST;
                table[i][j] = Math.max(Math.max(
                        table[i - 1][j]+INDEL_COST,
                        table[i][j - 1]+INDEL_COST),
                        table[i - 1][j - 1] + cost);
            }
        }

        int i = table.length-1;
        int j = table[0].length-1;

        while (i > 0 && j > 0) {
            int sub = table[i-1][j-1];
            int del = table[i-1][j];
            int ins = table[i][j-1];
            int max = Math.max(sub, Math.max(del, ins));

            if (max == sub) {
                from.push(s1.charAt(a+i-1));
                to.push(s2.charAt(b+j-1));
                i--;
                j--;
            }
            else if (max == del) {
                from.push(s1.charAt(a+i-1));
                to.push('-');
                i--;
            }
            else if (max == ins) {
                from.push('-');
                to.push(s2.charAt(b+j-1));
                j--;
            }
        }

        while (i > 0) {
            from.push(s1.charAt(a+i-1));
            to.push('-');
            i--;
        }

        while (j > 0) {
            from.push('-');
            to.push(s2.charAt(b+j-1));
            j--;
        }
    }

    private static int maxVal(int[] arr, int[] rev) {
        int max = Integer.MIN_VALUE;
        int idx = -1;
        int j = rev.length-1;
        for (int i = 0; i < arr.length && j >= 0; i++, j--) {
            int sum = arr[i]+rev[j];
            if (sum > max) {
                max = sum;
                idx = i;
            }
        }
        return idx;
    }

    private static int[] reverse(int[] a) {
        int len = a.length-1;
        for (int i = 0; i < len/2; i++) {
            int tmp = a[i];
            a[i] = a[len-i];
            a[len-i] = tmp;
        }
        return a;
    }
}
