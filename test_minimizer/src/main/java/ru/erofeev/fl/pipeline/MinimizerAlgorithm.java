package ru.erofeev.fl.pipeline;

enum MinimizerAlgorithm {
    NAIVE,
    GREEDY_ESSENTIAL,
    GENETIC,
    GENETIC_JENETICS;

    static MinimizerAlgorithm fromProperty(String rawValue) {
        if (rawValue == null) {
            return GREEDY_ESSENTIAL;
        }
        String normalized = rawValue.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return GREEDY_ESSENTIAL;
        }
        if ("GA".equals(normalized)) {
            return GENETIC;
        }
        if ("GJ".equals(normalized)) {
            return GENETIC_JENETICS;
        }
        if ("GR".equals(normalized) || "GREEDY".equals(normalized)) {
            return GREEDY_ESSENTIAL;
        }
        
        try {
            return MinimizerAlgorithm.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + normalized + ". Supported: NAIVE, GREEDY_ESSENTIAL, GENETIC, GENETIC_JENETICS, GA, GJ");
        }
    }
}

