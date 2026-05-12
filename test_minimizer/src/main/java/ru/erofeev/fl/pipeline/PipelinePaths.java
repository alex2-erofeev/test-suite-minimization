package ru.erofeev.fl.pipeline;

import java.nio.file.Path;
import java.nio.file.Paths;

final class PipelinePaths {
    private final Path root;

    private PipelinePaths(Path root) {
        this.root = root;
    }

    static PipelinePaths fromSystemProperties() {
        String rootDir = System.getProperty("minimizer.outputDir", "target/minimizer");
        return new PipelinePaths(Paths.get(rootDir).toAbsolutePath().normalize());
    }

    Path root() {
        return root;
    }

    Path fullRunDir() {
        return root.resolve("full-run");
    }

    Path selectedRunDir() {
        return root.resolve("selected-run");
    }

    Path naiveDir() {
        return root.resolve("naive");
    }

    Path greedyEssentialDir() {
        return root.resolve("greedy-essential");
    }

    Path geneticDir() {
        return root.resolve("genetic");
    }

    Path minimizerDir(MinimizerAlgorithm algorithm) {
        switch (algorithm) {
            case NAIVE:
                return naiveDir();
            case GENETIC:
                return geneticDir();
            case GREEDY_ESSENTIAL:
            default:
                return greedyEssentialDir();
        }
    }

    Path fullTestsCsv() {
        return fullRunDir().resolve("tests.csv");
    }

    Path selectedTestsCsv() {
        return selectedRunDir().resolve("tests.csv");
    }

    Path selectedIdsFile(MinimizerAlgorithm algorithm) {
        return minimizerDir(algorithm).resolve("selected-test-ids.txt");
    }
}