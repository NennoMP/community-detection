/*
 * DataValidator
 *
 * @description: Class for validating SNAP datasets, from the graph file an edges list file is generated, then the
 *               node ids are rescaled both in the edge file and the ground-truth communities file to account for
 *               missing node ids in the original dataset. Finally, small communities (<3) are filtered.
 *
 * @author: matteo.pinna@hotmail.com
 */

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataValidator {


    /**
     * Validate a dataset: generate edges file from entire graph file, and rescale node ids.
     *
     * @param dir     the directory of the dataset
     * @param dataset the name of the dataset
     */
    public static void validate(String dir, String dataset) {
        System.out.println("[INFO]: validating dataset");

        String edgesFileToValidate = String.format("%s/original/%s.txt", dir, dataset);
        String gtcFileToValidate = String.format("%s/original/%sGTC.txt", dir, dataset);

        String edgesFileValidated = String.format("%s/%s_edges.txt", dir, dataset);
        String gtcFileValidated = String.format("%s/%sGTC.txt", dir, dataset);

        // Rescale node ids
        rescaleNodeIds(edgesFileToValidate, edgesFileValidated, gtcFileToValidate, gtcFileValidated);
        // Remove small communities (< 3)
        removeSmallCommunities(gtcFileValidated);

        System.out.println("[INFO]: finished validating dataset");
    }

    /**
     * Rescale node ids in the list of edges.
     *
     * @param infileEdges  edges file to rescale
     * @param outfileEdges rescaled edges file
     * @param infileGtc    gtc file to rescale
     * @param outfileGtc   rescaled gtc file
     */
    public static void rescaleNodeIds(String infileEdges, String outfileEdges, String infileGtc, String outfileGtc) {
        Map<Integer, Integer> oldToNewIds = new HashMap<>();

        int newId = 0;
        // Process and rescale node ids for each edge
        try (BufferedReader inf = new BufferedReader(new FileReader(infileEdges));
             BufferedWriter outf = new BufferedWriter(new FileWriter(outfileEdges))) {
            String line;
            while ((line = inf.readLine()) != null) {
                if (!line.matches("^\\d+\\s+\\d+$")) continue;

                String[] parts = line.strip().split("\\s+");
                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                if (!oldToNewIds.containsKey(u)) {
                    oldToNewIds.put(u, newId);
                    newId++;
                }
                if (!oldToNewIds.containsKey(v)) {
                    oldToNewIds.put(v, newId);
                    newId++;
                }
                outf.write(oldToNewIds.get(u) + " " + oldToNewIds.get(v) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Rescale the gtc communities
        rescaleCommunities(oldToNewIds, infileGtc, outfileGtc);
    }

    /**
     * Filter out small communities with less than 3 nodes (as in SNAP description).
     *
     * @param file the communities file to filter
     */
    public static void removeSmallCommunities(String file) {
        List<String> filteredCommunities = new ArrayList<>();
        try (BufferedReader f = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = f.readLine()) != null) {
                if (line.split("\\s+").length >= 3) {
                    filteredCommunities.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedWriter f = new BufferedWriter(new FileWriter(file))) {
            for (String c : filteredCommunities) {
                f.write(c + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Rescale node ids in the communities file.
     *
     * @param dictionary the dictionary containing the mapping between new node ids and old ones.
     * @param infile     communities file to rescale
     * @param outfile    rescaled communities file
     */

    public static void rescaleCommunities(Map<Integer, Integer> dictionary, String infile, String outfile) {
        try (BufferedReader inf = new BufferedReader(new FileReader(infile));
             BufferedWriter outf = new BufferedWriter(new FileWriter(outfile))) {
            String line;
            while ((line = inf.readLine()) != null) {
                String[] parts = line.strip().split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    try {
                        int node = Integer.parseInt(part);
                        sb.append(dictionary.get(node)).append(" ");
                    } catch (NumberFormatException e) {
                        //do nothing
                    }
                }
                outf.write(sb.toString().trim() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
