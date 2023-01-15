/*
 * algorithms.CommunityDetectionAlgorithm
 *
 * @description: Abstract (super) class for the community detection algorithms.
 *
 * @author: matteo.pinna@hotmail.com
 */

package algorithms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class CommunityDetectionAlgorithm {

    protected final int FILTER_COMMUNITY_THRESHOLD = 3;
    protected String dir;
    protected String dataset;
    protected String gtcCommunitiesFile; // ground-truth communities
    protected String detectedCommunitiesFile;
    protected String edgesFile;

    protected int nNodes;
    protected int nEdges;

    protected Logger logger = null;

    public CommunityDetectionAlgorithm(String dir, String dataset) {
        this.dir = dir;
        this.dataset = dataset;
        this.edgesFile = dir + dataset + "_edges.txt";
        this.gtcCommunitiesFile = dir + dataset + "GTC.txt";

        int[] graphSize = getGraphSize(edgesFile);
        this.nNodes = graphSize[0];
        this.nEdges = graphSize[1];
    }

    /**
     * Retrieve graph size (# nodes, # edges) and store it.
     *
     * @param inputFile the input edges list file
     * @return numbers of nodes and number of edges
     */
    private int[] getGraphSize(String inputFile) {
        Set<Integer> nodes = new HashSet<>();
        int nEdges = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                nodes.add(u);
                nodes.add(v);
                nEdges++;
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
        return new int[]{nodes.size(), nEdges};
    }

    /**
     * Compute average F1 score between two sets of communities.
     *
     * @param gtcCommunitiesFile      the ground-truth communities file
     * @param detectedCommunitiesFile the detected communities file
     * @return the average F1 score
     */
    protected static double averageF1Score(String gtcCommunitiesFile, String detectedCommunitiesFile) {
        List<Set<Integer>> gtcCommunities = loadCommunities(gtcCommunitiesFile);
        List<Set<Integer>> detectedCommunities = loadCommunities(detectedCommunitiesFile);

        // Create matrix with pair-wise F1 scores
        double[][] f1Matrix = new double[detectedCommunities.size()][gtcCommunities.size()];
        for (int i = 0; i < detectedCommunities.size(); i++) {
            for (int j = 0; j < gtcCommunities.size(); j++) {
                f1Matrix[i][j] = F1Score(detectedCommunities.get(i), gtcCommunities.get(j));
            }
        }

        // Compute F1score(detectedCommunities, gtcCommunities)
        double f1AverageDetectedToGtc = 0;
        for (double[] matrix : f1Matrix) {
            double max = Double.MIN_VALUE;
            for (double v : matrix) {
                max = Math.max(max, v);
            }
            f1AverageDetectedToGtc += max;
        }
        f1AverageDetectedToGtc = f1AverageDetectedToGtc / detectedCommunities.size();

        // Compute F1score(gtcCommunities, detectedCommunities)
        double f1AverageGtcToDetected = 0;
        for (int j = 0; j < f1Matrix[0].length; j++) {
            double max = Double.MIN_VALUE;
            for (double[] matrix : f1Matrix) {
                max = Math.max(max, matrix[j]);
            }
            f1AverageGtcToDetected += max;
        }
        f1AverageGtcToDetected = f1AverageGtcToDetected / gtcCommunities.size();

        // Compute and return average F1 score
        return (f1AverageDetectedToGtc + f1AverageGtcToDetected) / 2;
    }

    /**
     * Load communities into memory from corresponding file.
     *
     * @param communitiesFile the file containing the communities.
     * @return a list of sets containing the communities (node ids)
     */
    private static List<Set<Integer>> loadCommunities(String communitiesFile) {
        List<Set<Integer>> communities = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(communitiesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nodes = line.split("\\s+");
                Set<Integer> community = new HashSet<>();
                for (String node : nodes) {
                    community.add(Integer.parseInt(node));
                }
                communities.add(community);
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
        return communities;
    }

    /**
     * Compute F1 score between two communities.
     *
     * @param gtcCommunity      the ground-truth community
     * @param detectedCommunity the detected community
     * @return the f1 score between the two communities
     */
    protected static double F1Score(Set<Integer> gtcCommunity, Set<Integer> detectedCommunity) {
        // Compute intersection between the two sets
        Set<Integer> intersection = new HashSet<>(detectedCommunity);
        intersection.retainAll(gtcCommunity);

        // Compute precision
        double precision = (double) intersection.size() / detectedCommunity.size();
        // Compute recall
        double recall = (double) intersection.size() / gtcCommunity.size();

        // Avoid division by 0
        if (precision + recall == 0) {
            return 0;
        }

        // Compute and return F1 score
        return 2 * (precision * recall) / (precision + recall);
    }

    abstract void run();

    public int getNodes() {
        return nNodes;
    }

    public int getEdges() {
        return nEdges;
    }
}
