package cluster;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class ClusterToGraphConverter {

    public static void printCluster(String infile, String outfile, int id) {
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(infile)), "UTF-8"))) {
            String outpath = new File(outfile).getAbsolutePath();
            File out = new File(outpath + ".gv");
            int c = 0;
            String line;
            do {
                if (c == id) {
                    read.readLine();
                    List<String> cluster = new ArrayList<>();
                    for (String edge = read.readLine(); edge != null && !edge.isEmpty(); edge = read.readLine()) {
                        cluster.add(edge);
                    }
                    PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charset.forName("UTF-8")));
                    printTree(cluster, writ);
                    writ.close();
                    Runtime.getRuntime().exec("dot -Tpng " + out.getAbsolutePath() + " -o " + outpath + '-' + c + ".png").waitFor();
                }
                do {
                    line = read.readLine();
                } while(line != null && !line.isEmpty());
                c++;
            } while (line != null && c <= id);
            out.delete();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void printTopNClusters(String infile, String outfile, int top) {
        printClusters(infile, outfile, 1, top);
    }

    public static void printClustersUntilSize(String infile, String outfile, int minSize) {
        printClusters(infile, outfile, minSize, Integer.MAX_VALUE);
    }

    public static void printClusters(String infile, String outfile, int minSize, int top) {
        try (BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(new File(infile)), "UTF-8"))) {
            String outpath = new File(outfile).getAbsolutePath();
            File out = new File(outpath + ".gv");
            Runtime run = Runtime.getRuntime();
            int c = 0;
            String line = read.readLine();
            boolean stop = false;
            do {
                if (!line.isEmpty() && line.split("\t").length == 2) {
                    List<String> cluster = new ArrayList<>();
                    for (String edge = read.readLine(); edge != null && !edge.isEmpty(); edge = read.readLine()) {
                        cluster.add(edge);
                    }
                    if (cluster.size() < minSize)
                        stop = true;
                    else {
                        PrintWriter writ = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charset.forName("UTF-8")));
                        printTree(cluster, writ);
                        writ.close();
                        run.exec("dot -Tpng " + out.getAbsolutePath() + " -o " + outpath + '-' + c + ".png").waitFor();
                        c++;
                    }
                    line = read.readLine();
                }
            } while (line != null && !stop && c < top);
            out.delete();
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void printTree(List<String> cluster, PrintWriter writ) {
        writ.println("graph g {");
        writ.println("graph[outputorder=edgesfirst,rankdir=LR,ranksep=2];");
        writ.println("node[style=filled];");
        writ.println("edge[headclip=false,tailclip=false];");
        for (String edge : cluster) {
            String[] fields = edge.split("\t");
            writ.println("\"" + fields[0] + "\" -- \"" + fields[3]
                    + "\" [ penwidth = " + (Integer.parseInt(fields[2])/100.0)
                    + " , label = \"" + fields[1] + "\" ];");
        }
        writ.println("}");
    }

    public static void main(String[] args) {
        if (args.length % 2 == 0) {
            help();
        }
        else {
            String infile = null;
            String outfile = null;
            int id = -1;
            int minsize = 1;
            int top = Integer.MAX_VALUE;

            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "-in": infile = args[++i]; break;
                    case "-out": outfile = args[++i]; break;
                    case "-id": id = Integer.parseInt(args[++i]); break;
                    case "-minsize": minsize = Integer.parseInt(args[++i]); break;
                    case "-top": top = Integer.parseInt(args[++i]); break;
                    default: help();
                }
            }

            if (infile == null || outfile == null)
                help();

            System.err.println("Printing selected clusters...");
            if (id >= 0)
                printCluster(infile, outfile, id);
            else
                printClusters(infile, outfile, minsize, top);
        }
    }

    private static void help() {
        System.err.println("Usage: java -jar colex.jar -graph -in <infile> -out <outfile-prefix> -id|-minsize|-top <value>");

        System.err.println("-id: Print single cluster with id <value>");
        System.err.println("-minsize: Print all clusters with a minimum size of <value>");
        System.err.println("-top: Print top <value> clusters");

        System.exit(0);
    }
}
