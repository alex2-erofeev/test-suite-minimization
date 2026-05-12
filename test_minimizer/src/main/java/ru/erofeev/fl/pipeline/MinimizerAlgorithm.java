package ru.erofeev.fl.pipeline;

enum MinimizerAlgorithm {
    NAIVE,
    GREEDY_ESSENTIAL,
    GENETIC;

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
        if ("GR".equals(normalized) || "GREEDY".equals(normalized)) {
            return GREEDY_ESSENTIAL;
        }
        
        try {
            return MinimizerAlgorithm.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported algorithm: " + normalized + ". Supported: NAIVE, GREEDY_ESSENTIAL, GENETIC");
        }
    }
}