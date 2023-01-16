/*
 * @author: matteo.pinna@hotmail.com
 */

import algorithms.CoEuS;
import algorithms.SCoDA;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;


public class Main {

    /**
     * @param args First and only argument must be the name of the dataset
     */
    public static void main(String[] args) {
        String dir = "./src/data/";
        String dataset = getUserInput(dir);
        dir = String.format("%s%s/", dir, dataset);


        // Validate dataset
        DataValidator.validate(dir, dataset);

        //SCoDA
        SCoDA sCoDA = new SCoDA(dir, dataset);
        sCoDA.run();
        sCoDA.evaluate();

        // CoEuS: default update rule
        CoEuS coEuS = new CoEuS(dir, dataset, CoEuS.UpdateRule.DEFAULT);
        coEuS.run();
        coEuS.evaluate();

        // CoEuS: edge quality update rule
        //coEuS.setUpdateRule(CoEuS.UpdateRule.EDGE_QUALITY);
        //coEuS.run();
        //coEuS.evaluate();
    }

    /**
     * Get user input, i.e. chosen dataset for analysis.
     *
     * @param dir the directory where datasets are and should be contained
     * @return the name of the dataset chosen, if exists
     */
    private static String getUserInput(String dir) {
        Scanner sc = new Scanner(System.in);
        String input;

        boolean flag;
        do {
            flag = true;
            printHelper();
            input = sc.nextLine().toLowerCase();
            switch (input) {
                case "amazon" -> {
                    input = Dataset.AMAZON.toString();
                }
                case "dblp" -> {
                    input = Dataset.DBLP.toString();
                }
                default -> {
                    String graphFile = String.format("%s%s/original/%s.txt", dir, input, input);
                    String gtcFile = String.format("%s%s/original/%sGTC.txt", dir, input, input);
                    if (!Files.exists(Paths.get(graphFile))) {
                        flag = false;
                        System.err.println("Invalid dataset name, no edges file found!");
                    } else if (!Files.exists(Paths.get(gtcFile))) {
                        flag = false;
                        System.err.println("Invalid dataset name, no ground truth communities file found!");
                    }
                }
            }
        } while (!flag);

        return input;
    }

    /**
     * Print a helper message to the user, showing available datasets to choose from.
     */
    private static void printHelper() {
        System.out.println("[USAGE]: insert a valid (SNAP) <dataset> name which MUST exists in the <data/original> " +
                "folder and have <dataset.txt> and <datasetGTC.txt> files!");
        System.out.println();
        System.out.println("Otherwise, you can choose from one of the available datasets:");
        for (Dataset dataset : Dataset.values()) {
            System.out.println(dataset.name());
        }
    }

    /**
     * Enum containing available datasets.
     */
    public enum Dataset {
        DBLP,
        AMAZON;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }
}
