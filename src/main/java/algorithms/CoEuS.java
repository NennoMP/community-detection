/*
 * algorithms.CoEuS
 *
 * @description: Class implementing the CoEuS algorithm logic.
 *
 * @author: matteo.pinna@hotmail.com
 */

package algorithms;

import community.CommunityNode;
import community.CommunityScore;

import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CoEuS extends CommunityDetectionAlgorithm {

    // number of seeds for seed-set initialization
    private final int NUM_SEEDS = 3;
    // window size for the pruning
    private final int WINDOW_SIZE = 10000;
    // community size for the pruning
    private final int COMMUNITY_SIZE_THRESHOLD = 50;
    private final List<Set<Integer>> seedSets;
    // update rule for community degrees (DEFAULT, EDGE_QUALITY)
    private UpdateRule updateRule;

    public CoEuS(String dir, String dataset, UpdateRule updateRule) {
        super(dir, dataset);
        this.detectedCommunitiesFile = String.format("%s%s_%s_detected_communities.txt", dir,
                this.getClass().getSimpleName(), dataset);
        this.updateRule = updateRule;
        this.seedSets = initSeedSets();

        try {
            FileHandler handler = new FileHandler("./src/logs/algorithms.CoEuS.log", true);
            this.logger = Logger.getLogger("CoEuSLog");
            this.logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
    }

    /**
     * Initialize seed-sets NUM_SEEDS from ground truth communities.
     *
     * @return the initialized seed-sets
     */
    private List<Set<Integer>> initSeedSets() {
        List<Set<Integer>> seedSets = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(gtcCommunitiesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] community = line.split("\\s+");

                // Check that there are enough node ids to choose from
                if (community.length < NUM_SEEDS) {
                    throw new IllegalArgumentException("[ERROR]: less nodes ids than number of seeds: "
                            + "consider lowering number of seeds");
                }
                // Randomly select the node ids
                Set<Integer> randomIndices = new HashSet<>(NUM_SEEDS);
                while (randomIndices.size() < NUM_SEEDS) {
                    randomIndices.add(new Random().nextInt(community.length));

                }

                // Populate the seed-sets
                Set<Integer> seedSet = new HashSet<>();
                for (int i : randomIndices) {
                    seedSet.add(Integer.parseInt(community[i]));
                }
                seedSets.add(seedSet);
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }
        return seedSets;
    }

    /**
     * Execute the algorithm and store the detected communities in a file.
     */
    @Override
    public void run() {
        System.out.println("[INFO]: executing " + this.getClass().getSimpleName());

        int[] degrees = new int[this.nNodes];
        Map<CommunityNode, Integer> communityDegrees = new HashMap<>();
        List<Set<Integer>> communities = new ArrayList<>(seedSets.size());
        int processedElements = 0;

        // Populate communities with seed-sets
        for (int i = 0; i < seedSets.size(); i++) {
            Set<Integer> community = new HashSet<>(seedSets.get(i));
            communities.add(i, community);
            for (Integer node : community) {
                communityDegrees.merge(new CommunityNode(node, i), 1, Integer::sum);
            }
        }

        // Process the edges as a stream
        try (BufferedReader br = new BufferedReader(new FileReader(edgesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                processedElements++;

                // Retrieve nodes
                String[] nodes = line.split("\\s+");
                int u = Integer.parseInt(nodes[0]);
                int v = Integer.parseInt(nodes[1]);
                if (u == v) { // no self-loops
                    continue;
                }
                // Update edge's adjacent nodes degree
                degrees[u] += 1;
                degrees[v] += 1;

                // Communities and community degrees update rule
                for (int i = 0; i < communities.size(); i++) {
                    Set<Integer> community = communities.get(i);

                    performUpdateRule(degrees, communityDegrees, v, u, i, community);
                    performUpdateRule(degrees, communityDegrees, u, v, i, community);
                }

                // Prune all communities when window is full
                if (processedElements % WINDOW_SIZE == 0) {
                    final List<Set<Integer>> communitiesCopy = new ArrayList<>(communities);
                    communities = IntStream.range(0, communities.size())
                            .mapToObj(i -> pruneCommunity(i, communitiesCopy.get(i), degrees, communityDegrees))
                            .collect(Collectors.toList());
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR]: " + e.getMessage());
        }

        // Filter communities with less than <FILTER_COMMUNITY_THRESHOLD> nodes
        List<Set<Integer>> filteredCommunities = filterCommunities(communities);

        // Write out the detected (filtered) communities
        writeDetectedCommunities(filteredCommunities);

        System.out.println("[INFO]: finished " + this.getClass().getSimpleName());
    }

    /**
     * Apply the update rule to the nodes connected by an edge, possibly updating
     * the community degrees and communities.
     *
     * @param degrees          the node's degrees
     * @param communityDegrees the community degrees for each node
     * @param u                node 1
     * @param v                node 2
     * @param i                the current community id
     * @param community        the current community
     */
    private void performUpdateRule(int[] degrees, Map<CommunityNode, Integer> communityDegrees, int u, int v, int i,
                                   Set<Integer> community) {
        if (community.contains(v)) {
            if (updateRule == UpdateRule.DEFAULT) {
                communityDegrees.merge(new CommunityNode(u, i), 1, Integer::sum);
            } else if (updateRule == UpdateRule.EDGE_QUALITY) {
                if (communityDegrees.containsKey(new CommunityNode(u, i))) {
                    int currentDegree = communityDegrees.get(new CommunityNode(u, i));
                    int edgeQuality = communityDegrees.get(new CommunityNode(v, i)) / degrees[v];
                    communityDegrees.put(new CommunityNode(u, i), currentDegree + edgeQuality);
                } else {
                    communityDegrees.put(new CommunityNode(u, i), 1);
                }
            } else {
                throw new IllegalArgumentException("[ERROR]: unknown update rule!");
            }
            community.add(u);
        }
    }

    /**
     * Prune a community according to MAX_COMMUNITY_SIZE.
     *
     * @param communityId      the id of the community to be pruned
     * @param community        the community to be pruned
     * @param degrees          the nodes' degrees
     * @param communityDegrees the community degrees for each node
     * @return the pruned community
     */
    private Set<Integer> pruneCommunity(int communityId, Set<Integer> community, int[] degrees,
                                        Map<CommunityNode, Integer> communityDegrees) {

        // Min-heap
        PriorityQueue<CommunityScore> minHeap = new PriorityQueue<>(Comparator.comparing(CommunityScore::getCpScore));

        for (int c : community) {
            // compute community participation value
            int cp = communityDegrees.get(new CommunityNode(c, communityId)) / Math.max(1,
                    degrees[c]);

            // If heap not full -> push community score <node, cp>
            if (minHeap.size() < COMMUNITY_SIZE_THRESHOLD) {
                minHeap.add(new CommunityScore(c, cp));
            }
            // If heap full and better community score than minimum in heap -> replace
            else if (cp > minHeap.peek().cpScore) {
                int prunedNode = minHeap.poll().node;
                minHeap.add(new CommunityScore(c, cp));

                // Remove pruned node pair from community degrees
                communityDegrees.remove(new CommunityNode(prunedNode, communityId));
            }
        }

        // Convert heap to community to be returned
        Set<Integer> prunedCommunity = new HashSet<>();
        while (!minHeap.isEmpty()) {
            prunedCommunity.add(minHeap.poll().node);
        }
        return prunedCommunity;
    }

    /**
     * Filter small communities with less than FILTER_COMMUNITY_THRESHOLD nodes
     *
     * @param communities the communities to filter.
     * @return the filtered communities.
     */
    private List<Set<Integer>> filterCommunities(List<Set<Integer>> communities) {
        communities.removeIf(community -> community.size() < FILTER_COMMUNITY_THRESHOLD);
        return communities;
    }

    /**
     * Write detected communities to file.
     *
     * @param communities the communities to write out
     */
    private void writeDetectedCommunities(List<Set<Integer>> communities) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(detectedCommunitiesFile))) {
            for (Set<Integer> community : communities) {
                for (Integer node : community) {
                    bw.write(node + " ");
                }
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Evaluate the algorithm's detected communities with average F1 score.
     */
    public void evaluate() {
        System.out.println("[INFO]: evaluating " + this.getClass().getSimpleName());

        double score = CommunityDetectionAlgorithm.averageF1Score(gtcCommunitiesFile, detectedCommunitiesFile);
        String msg = String.format("[%s] [%s] | [average-F1-score] | [%s]: %.5f", dataset,
                updateRule.name(), this.getClass().getSimpleName(), score);
        logger.info(msg);

    }

    public List<Set<Integer>> getSeedSets() {
        return seedSets;
    }

    public void setUpdateRule(UpdateRule newUpdateRule) {
        this.updateRule = newUpdateRule;
    }

    /**
     * Enum containing available update rules for community degrees.
     */
    public enum UpdateRule {
        DEFAULT,
        EDGE_QUALITY,
    }
}
