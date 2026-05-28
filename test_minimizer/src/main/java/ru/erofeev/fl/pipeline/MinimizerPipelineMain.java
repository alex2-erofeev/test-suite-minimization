package ru.erofeev.fl.pipeline;

import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.coverage.CoverageMatrixBuilder;
import ru.erofeev.fl.coverage.JacocoAgentClient;
import ru.erofeev.fl.coverage.JunitSuiteRunner;
import ru.erofeev.fl.coverage.TestRunRecord;
import ru.erofeev.fl.coverage.TestStatus;
import ru.erofeev.fl.coverage.TestsCsv;
import ru.erofeev.fl.metrics.ResourceUsageSampler;
import ru.erofeev.fl.metrics.ResourceUsageSnapshot;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class MinimizerPipelineMain {
    private static final long RESOURCE_SAMPLE_INTERVAL_MS = 200L;

    private MinimizerPipelineMain() {
    }

    public static void premain(String agentArgs, java.lang.instrument.Instrumentation inst) {
        // no-op
    }

    public static void main(String[] args) throws Exception {
        boolean relaunchedChild = JacocoJvmRelauncher.alreadyRelaunched();
        String command = args.length == 0 ? "pipeline" : args[0].trim().toLowerCase();
        if (requiresCoverageCollection(command) && !JacocoAgentClient.isAgentActive() && !relaunchedChild) {
            int code = JacocoJvmRelauncher.relaunchWithJacocoAgent(MinimizerPipelineMain.class, args);
            if (code != 0) {
                throw new IllegalStateException("Relaunched JVM exited with code " + code);
            }
            return;
        }

        PipelineOptions options = PipelineOptions.fromSystemProperties();
        int exitCode = 0;
        try {
            MinimizerPipelineMain pipeline = new MinimizerPipelineMain();
            pipeline.run(command, options);
        } catch (Exception ex) {
            exitCode = 1;
            throw ex;
        } finally {
            if (relaunchedChild) {
                System.exit(exitCode);
            }
        }
    }

    private void run(String command, PipelineOptions options) throws Exception {
        switch (command) {
            case "collect-full":
                collectFull(options);
                break;
            case "minimize":
                minimize(options);
                break;
            case "collect-selected":
                collectSelected(options);
                break;
            case "pipeline":
                collectFull(options);
                minimize(options);
                collectSelected(options);
                break;
            default:
                printUsage();
        }
    }

    private void collectFull(PipelineOptions options) throws Exception {
        ensureJacocoAgent("collect-full");
        JunitSuiteRunner runner = new JunitSuiteRunner();
        Path testsCsv = runner.runFullSuite(options.paths().fullRunDir(), options.classNamePattern());
        System.out.println("Full test run metadata: " + testsCsv);
    }

    private void minimize(PipelineOptions options) throws Exception {
        ResourceUsageSampler resourceSampler = ResourceUsageSampler.start(RESOURCE_SAMPLE_INTERVAL_MS);
        List<TestRunRecord> sourceRecords = TestsCsv.read(options.paths().fullTestsCsv());
        try {
            EnumMap<TestStatus, Integer> statusCount = new EnumMap<>(TestStatus.class);
            for (TestStatus status : TestStatus.values()) {
                statusCount.put(status, 0);
            }
            for (TestRunRecord record : sourceRecords) {
                statusCount.put(record.status(), statusCount.get(record.status()) + 1);
            }

            System.out.println("Minimize input tests: total=" + sourceRecords.size()
                + ", success=" + statusCount.get(TestStatus.SUCCESS)
                + ", failed=" + statusCount.get(TestStatus.FAILED)
                + ", aborted=" + statusCount.get(TestStatus.ABORTED)
                + ", skipped=" + statusCount.get(TestStatus.SKIPPED));
            System.out.println("Minimizer algorithm: " + options.minimizerAlgorithm());

            long matrixStart = System.nanoTime();
            CoverageMatrixBuilder builder = new CoverageMatrixBuilder();
            CoverageMatrix matrix = builder.build(
                options.paths().fullTestsCsv(),
                options.classesDir(),
                options.coverageMetric(),
                true
            );
            long matrixMillis = (System.nanoTime() - matrixStart) / 1_000_000L;

            System.out.println("Matrix size: tests=" + matrix.testCount() + ", elements=" + matrix.elementCount() + ", metric=" + options.coverageMetric());
            
            switch (options.minimizerAlgorithm()) {
                case NAIVE:
                case GREEDY_ESSENTIAL:
                case GENETIC:
                case GENETIC_JENETICS:
                    minimizeWithHeuristic(options, matrix, matrixMillis, resourceSampler);
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported minimizer algorithm: " + options.minimizerAlgorithm() + ". Please use NAIVE, GREEDY_ESSENTIAL or GENETIC.");
            }
        } finally {
            resourceSampler.stopAndSnapshot();
        }
    }

    private void minimizeWithHeuristic(
        PipelineOptions options,
        CoverageMatrix matrix,
        long matrixMillis,
        ResourceUsageSampler resourceSampler
    ) throws Exception {
        HeuristicMinimizer minimizer = new HeuristicMinimizer();
        HeuristicResult result = minimizer.minimize(matrix, options.minimizerAlgorithm());
        long solverMillis = result.solveNanos() / 1_000_000L;
        ResourceUsageSnapshot resourceSnapshot = resourceSampler.stopAndSnapshot();
        HeuristicResultWriter.write(
            options.paths().minimizerDir(options.minimizerAlgorithm()),
            result,
            matrix,
            options.coverageMetric(),
            matrixMillis,
            solverMillis,
            resourceSnapshot
        );

        System.out.println("Heuristic algorithm: " + result.algorithmName() + " (" + options.minimizerAlgorithm() + ")");
        System.out.println("Heuristic selected tests: " + result.selectedTestIds().size());
        System.out.println("Heuristic reported coverage %: " + formatDouble(result.coveragePercent()));
        System.out.println("Matrix build ms: " + matrixMillis);
        System.out.println("Heuristic solve ms: " + solverMillis);
        printResourceMetrics(resourceSnapshot);
        System.out.println("Selected IDs file: " + options.paths().selectedIdsFile(options.minimizerAlgorithm()));
    }

    private static void printResourceMetrics(ResourceUsageSnapshot snapshot) {
        System.out.println("Peak RAM MB (heap used): " + formatDouble(snapshot.peakHeapUsedMb()));
        if (snapshot.hasPeakProcessCommittedBytes()) {
            System.out.println("Peak RAM MB (process committed virtual): " + formatDouble(snapshot.peakProcessCommittedMb()));
        } else {
            System.out.println("Peak RAM MB (process committed virtual): n/a");
        }
        if (snapshot.hasPeakProcessCpuPercent()) {
            System.out.println("Peak CPU % (process, sampled): " + formatDouble(snapshot.peakProcessCpuPercent()));
        } else {
            System.out.println("Peak CPU % (process, sampled): n/a");
        }
        System.out.println("Resource sample interval ms: " + snapshot.sampleIntervalMs());
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void collectSelected(PipelineOptions options) throws Exception {
        ensureJacocoAgent("collect-selected");
        List<String> selectedIds = JunitSuiteRunner.readSelectedIds(
            options.paths().selectedIdsFile(options.minimizerAlgorithm())
        );
        JunitSuiteRunner runner = new JunitSuiteRunner();
        Path testsCsv = runner.runSelectedTests(options.paths().selectedRunDir(), selectedIds);
        System.out.println("Selected test run metadata: " + testsCsv);
    }

    private static boolean requiresCoverageCollection(String command) {
        return "collect-full".equals(command) || "collect-selected".equals(command) || "pipeline".equals(command);
    }

    private static void ensureJacocoAgent(String command) {
        if (!JacocoAgentClient.isAgentActive()) {
            throw new IllegalStateException("JaCoCo agent is not active for command " + command + ".");
        }
    }

    private static void printUsage() {
        System.out.println("Usage: MinimizerPipelineMain <command>");
        System.out.println("Commands: collect-full | minimize | collect-selected | pipeline");
        System.out.println("Optional property: -Dminimizer.algorithm=NAIVE|GREEDY_ESSENTIAL|GENETIC|GA|GR (default GREEDY_ESSENTIAL)");
    }
}