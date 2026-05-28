package ru.erofeev.fl.algorithm;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.RandomRegistry;
import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.model.MinimizationResult;

import java.util.*;

public class GeneticJeneticsAlgorithm implements TestSuiteMinimizationAlgorithm {

    private final int populationSize = 16;
    private final int generations   = 48;
    private final double mutationRate  = 0.01;
    private final double crossoverRate = 0.20;
    private final double elitismRate   = 0.45;
    private final int numRuns = 5;

    @Override
    public String getName() {
        return "GeneticJenetics";
    }

    @Override
    public MinimizationResult run(CoverageMatrix matrix) {
        long start = System.nanoTime();

        // === Step 1: Essential tests (идентично GeneticAlgorithm) ===
        Set<Integer> essential = new LinkedHashSet<>();
        BitSet coveredByEssential = new BitSet(matrix.elementCount());
        for (int r = 0; r < matrix.elementCount(); r++) {
            List<Integer> covering = matrix.testsCoveringElement(r);
            if (covering != null && covering.size() == 1) {
                essential.add(covering.get(0));
            }
        }
        for (int t : essential) {
            coveredByEssential.or(matrix.coverageForTest(t));
        }

        BitSet remainingReqs = new BitSet(matrix.elementCount());
        remainingReqs.set(0, matrix.elementCount());
        remainingReqs.andNot(coveredByEssential);

        List<Integer> candidates = new ArrayList<>();
        for (int t = 0; t < matrix.testCount(); t++) {
            if (!essential.contains(t)) candidates.add(t);
        }

        Set<Integer> bestSelected = new LinkedHashSet<>(essential);

        if (!remainingReqs.isEmpty() && !candidates.isEmpty()) {
            // === Step 2: Jenetics GA ===
            final int n = candidates.size();
            final BitSet reqsFinal = remainingReqs;

            boolean[] bestBits = null;
            double bestFitness = Double.MAX_VALUE;

            Engine<BitGene, Double> engine = Engine
                .builder(
                    gt -> fitness(gt, candidates, matrix, reqsFinal),
                    Genotype.of(BitChromosome.of(n, 0.2))
                )
                .populationSize(populationSize)
                .alterers(
                    new Mutator<>(mutationRate),
                    new SinglePointCrossover<>(crossoverRate)
                )
                .selector(new TournamentSelector<>(3))
                .offspringFraction(1.0 - elitismRate)
                .executor(Runnable::run)  // однопоточность → детерминизм
                .minimizing()
                .build();

            for (int run = 0; run < numRuns; run++) {

                final int currentRun = run;
                Genotype<BitGene> best = RandomRegistry.with(
                    new Random(42L + currentRun),
                    r -> engine.stream()
                        .limit(generations)
                        .collect(EvolutionResult.toBestGenotype())
                );

                boolean[] bits = toBooleanArray(best, n);
                double f = fitnessArray(bits, candidates, matrix, reqsFinal);
                if (f < bestFitness) {
                    bestFitness = f;
                    bestBits = bits;
                }
            }

            if (bestBits != null) {
                for (int i = 0; i < bestBits.length; i++) {
                    if (bestBits[i]) bestSelected.add(candidates.get(i));
                }
            }

        } else if (!remainingReqs.isEmpty()) {
            bestSelected.addAll(candidates);
        }

        long end = System.nanoTime();
        List<String> names = new ArrayList<>();
        BitSet finalCoverage = new BitSet();
        for (int t : bestSelected) {
            names.add(matrix.tests().get(t).testName());
            finalCoverage.or(matrix.coverageForTest(t));
        }
        double coveragePercent = matrix.elementCount() == 0 ? 1.0
            : (double) finalCoverage.cardinality() / matrix.elementCount();

        return new MinimizationResult(
            "GeneticJenetics", bestSelected, names, coveragePercent, end - start
        );
    }

    // ── Fitness (Jenetics-обёртка) ──────────────────────────────────────────

    private static Double fitness(Genotype<BitGene> gt,
                                  List<Integer> candidates,
                                  CoverageMatrix matrix,
                                  BitSet remainingReqs) {
        return fitnessArray(toBooleanArray(gt, candidates.size()),
                            candidates, matrix, remainingReqs);
    }

    private static double fitnessArray(boolean[] chromosome,
                                       List<Integer> candidates,
                                       CoverageMatrix matrix,
                                       BitSet remainingReqs) {
        int selected = 0;
        BitSet covered = new BitSet();
        for (int i = 0; i < chromosome.length; i++) {
            if (chromosome[i]) {
                selected++;
                covered.or(matrix.coverageForTest(candidates.get(i)));
            }
        }
        BitSet uncovered = (BitSet) remainingReqs.clone();
        uncovered.andNot(covered);
        // минимизируем: штраф 1000 за каждое непокрытое требование + размер набора
        return selected + 1000.0 * uncovered.cardinality();
    }

    // ── Утилита: Genotype → boolean[] ──────────────────────────────────────

    private static boolean[] toBooleanArray(Genotype<BitGene> gt, int n) {
        BitChromosome chr = gt.get(0).as(BitChromosome.class);
        boolean[] bits = new boolean[n];
        for (int i = 0; i < n; i++) {
            bits[i] = chr.get(i).booleanValue();
        }
        return bits;
    }
}