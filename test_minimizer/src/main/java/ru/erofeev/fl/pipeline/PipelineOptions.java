package ru.erofeev.fl.pipeline;

import ru.erofeev.fl.coverage.CoverageMetric;

import java.nio.file.Path;
import java.nio.file.Paths;

final class PipelineOptions {
    private final PipelinePaths paths;
    private final Path classesDir;
    private final String classNamePattern;
    private final CoverageMetric coverageMetric;
    private final MinimizerAlgorithm minimizerAlgorithm;

    private PipelineOptions(
        PipelinePaths paths,
        Path classesDir,
        String classNamePattern,
        CoverageMetric coverageMetric,
        MinimizerAlgorithm minimizerAlgorithm
    ) {
        this.paths = paths;
        this.classesDir = classesDir;
        this.classNamePattern = classNamePattern;
        this.coverageMetric = coverageMetric;
        this.minimizerAlgorithm = minimizerAlgorithm;
    }

    static PipelineOptions fromSystemProperties() {
        PipelinePaths paths = PipelinePaths.fromSystemProperties();
        
        Path classesDir = Paths.get(System.getProperty("minimizer.classesDir", "target/classes"))
            .toAbsolutePath()
            .normalize();
            
        String classPattern = System.getProperty("minimizer.classPattern", "");
        String metricRaw = System.getProperty("minimizer.metric", "LINE").trim().toUpperCase();
        CoverageMetric metric = CoverageMetric.valueOf(metricRaw);
        
        // По умолчанию ставим GREEDY_ESSENTIAL вместо PBE
        MinimizerAlgorithm minimizerAlgorithm = MinimizerAlgorithm.fromProperty(
            System.getProperty("minimizer.algorithm", "GREEDY_ESSENTIAL")
        );

        return new PipelineOptions(paths, classesDir, classPattern, metric, minimizerAlgorithm);
    }

    PipelinePaths paths() {
        return paths;
    }

    Path classesDir() {
        return classesDir;
    }

    String classNamePattern() {
        return classNamePattern;
    }

    CoverageMetric coverageMetric() {
        return coverageMetric;
    }

    MinimizerAlgorithm minimizerAlgorithm() {
        return minimizerAlgorithm;
    }
}