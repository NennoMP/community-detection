/*
 * algorithms.SCoDA
 *
 * @description: Class implementing the SCoDA algorithm logic.
 *
 * @author: matteo.pinna@hotmail.com
 */

package algorithms;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SCoDA extends CommunityDetectionAlgorithm {

    private final int SHUFFLE_BLOCK_SIZE = 1024 * 1024; // block size for shuffling edges in chunks
    private final double P = 0.5; // probability for deciding in degree equality cases
    private final int D; // threshold for edges arrival
    private final String shuffledEdgesFile;

    public SCoDA(String dir, String dataset) {
        super(dir, dataset);

        this.shuffledEdgesFile = String.format("%s%s_shuffled_edges.txt", dir, dataset);
        this.detectedCommunitiesFile = String.format("%s%s_%s_detected_communities.txt", dir,
                this.getClass().getSimpleName(), dataset);

        this.D = computeThreshold();

        try {
            FileHandler handler = new FileHandler("./src/logs/algorithms.SCoDA.log", true);
            this.logger = Logger.getLogger("SCoDALog");
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compute the threshold as the mode of the degree distribution
     * of the graph.
     *
     * @return the mode of the degree distribution of the graph
     */
    private int computeThreshold() {
        System.out.println("[INFO]: computing threshold " + this.getClass().getSimpleName());

        int[] degrees = new int[this.nNodes];
        Map<Integer, Integer> degreeDistribution = new HashMap<>();

        // Store degree of each node
        try (BufferedReader br = new BufferedReader(new FileReader(edgesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nodes = line.split(" ");
                int u = Integer.parseInt(nodes[0]);
                int v = Integer.parseInt(nodes[1]);
                degrees[u] += 1;
                degrees[v] += 1;
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }

        // Populate the degree distribution map
        for (Integer degree : degrees) {
            degreeDistribution.put(degree, degreeDistribution.getOrDefault(degree, 0) + 1);
        }

        // Remove leaf nodes (i.e. degree equal to 1)
        degreeDistribution.remove(1);

        // Compute the mode of the degree distribution
        int maxDegree = 0;
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : degreeDistribution.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxDegree = entry.getKey();
                maxCount = entry.getValue();
            }
        }
        return maxDegree;
    }

    /**
     * Execute the algorithm and store the detected communities in a file.
     */
    @Override
    public void run() {
        System.out.println("[INFO]: executing " + this.getClass().getSimpleName());

        Random rand = new Random();
        // Keep track of node degrees and communities
        int[] degrees = new int[nNodes];
        int[] communities = new int[nNodes];

        // Shuffle the edges list
        shuffleStream(SHUFFLE_BLOCK_SIZE);

        // Initialize communities
        for (int i = 0; i < nNodes; i++) {
            communities[i] = i;
        }

        // Process the edges as a stream
        try (BufferedReader br = new BufferedReader(new FileReader(shuffledEdgesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] nodes = line.split("\\s+");
                int u = Integer.parseInt(nodes[0]);
                int v = Integer.parseInt(nodes[1]);

                // Update edge's adjacent nodes degree
                degrees[u] += 1;
                degrees[v] += 1;

                // Communities update rule
                if (degrees[u] <= D && degrees[v] <= D) {
                    if (degrees[u] < degrees[v]) {
                        communities[u] = communities[v];
                    } else if (degrees[v] < degrees[u]) {
                        communities[v] = communities[u];
                    } else { // equality case -> arbitrarily decide based on P
                        if (rand.nextDouble() >= P) {
                            communities[u] = communities[v];
                        } else {
                            communities[v] = communities[u];
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }

        // Filter communities with less than <FILTER_COMMUNITY_THRESHOLD> nodes
        Map<Integer, List<Integer>> filteredCommunities = filterCommunities(communities);
        // Write out the detected (filtered) communities
        writeDetectedCommunities(filteredCommunities);

        System.out.println("[INFO]: finished " + this.getClass().getSimpleName());
    }

    /**
     * Randomly shuffle all the lines (i.e. edges) in a file in chunks using Fisher
     * Yates algorithm.
     *
     * @param blockSize the block size for shuffling the file's lines in chunks.
     */
    private void shuffleStream(int blockSize) {

        try (BufferedReader reader = new BufferedReader(new FileReader(edgesFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(shuffledEdgesFile))) {
            // Keep track of read and written lines
            String[] lines = new String[blockSize];
            String line;
            int nLines = 0;

            // Read lines
            while ((line = reader.readLine()) != null) {
                lines[nLines % blockSize] = line;
                nLines++;
                if (nLines == blockSize) { // shuffle and write lines
                    shuffleLines(lines);
                    for (String shuffledLine : lines) {
                        writer.write(shuffledLine);
                        writer.newLine();
                    }
                    nLines = 0;
                }
            }

            // Shuffle and write remaining non-shuffled lines
            int remaining = nLines % blockSize;
            if (remaining > 0) {
                String[] remainingLines = new String[remaining];
                System.arraycopy(lines, 0, remainingLines, 0, remaining);
                shuffleLines(remainingLines);
                for (String shuffledLine : remainingLines) {
                    writer.write(shuffledLine);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
    }

    /**
     * Filter small communities with less than FILTER_COMMUNITY_THRESHOLD nodes
     *
     * @param communities the communities to filter.
     * @return the filtered communities.
     */
    private Map<Integer, List<Integer>> filterCommunities(int[] communities) {

        // Convert array of communities to a dictionary
        Map<Integer, List<Integer>> communitiesDict = new HashMap<>();
        for (int i = 0; i < communities.length; i++) {
            if (!communitiesDict.containsKey(communities[i])) {
                communitiesDict.put(communities[i], new ArrayList<>());
            }
            communitiesDict.get(communities[i]).add(i);
        }

        // Filter the communities
        Map<Integer, List<Integer>> filteredCommunities = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : communitiesDict.entrySet()) {
            if (entry.getValue().size() >= FILTER_COMMUNITY_THRESHOLD) {
                filteredCommunities.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredCommunities;
    }

    /**
     * Write detected communities to file.
     *
     * @param communities the communities to write out
     */
    private void writeDetectedCommunities(Map<Integer, List<Integer>> communities) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(detectedCommunitiesFile))) {
            for (List<Integer> nodes : communities.values()) {
                StringBuilder sb = new StringBuilder();
                for (int node : nodes) {
                    sb.append(node).append(" ");
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
    }

    /**
     * Randomly shuffle a chunk of lines.
     *
     * @param lines the lines to be shuffled
     */
    private void shuffleLines(String[] lines) {
        for (int i = lines.length - 1; i > 0; i--) {
            int j = ThreadLocalRandom.current().nextInt(0, i + 1);
            String temp = lines[i];
            lines[i] = lines[j];
            lines[j] = temp;
        }
    }

    /**
     * Evaluate the algorithm's detected communities with average F1 score.
     */
    public void evaluate() {
        System.out.println("[INFO]: evaluating " + this.getClass().getSimpleName());

        double score = averageF1Score(gtcCommunitiesFile, detectedCommunitiesFile);
        String msg = String.format("[%s] | [average-F1-score] | [%s]: %.5f", dataset, this.getClass().getSimpleName()
                , score);
        logger.info(msg);
    }
}
