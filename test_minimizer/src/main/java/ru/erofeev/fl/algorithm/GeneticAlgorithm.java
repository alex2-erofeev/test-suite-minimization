package ru.erofeev.fl.algorithm;

import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.model.MinimizationResult;

import java.util.*;

public class GeneticAlgorithm implements TestSuiteMinimizationAlgorithm {

    private final int populationSize = 16;
    private final int iterations = 48;
    private final int tournamentSize = 3;
    private final double elitismRate = 0.45;
    private final double mutationRate = 0.01;
    private final double crossoverRate = 0.20;
    private final int numRuns = 5;
    private final double initSelectProb = 0.20;

    @Override
    public String getName() {
        return "GeneticAlgorithm";
    }

    @Override
    public MinimizationResult run(CoverageMatrix matrix) {
        long start = System.nanoTime();
        
        // === Step 1: Essential cases strategy (ATSM Section 3.3.4) ===
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
            // === Step 2: несколько независимых запусков GA ===
            boolean[] bestBits = null;
            double bestFitness = Double.MAX_VALUE;
            
            for (int run = 0; run < numRuns; run++) {
                Random runRandom = new Random(42L + run);
                boolean[] runBest = runGA(candidates, matrix, remainingReqs, runRandom);
                double runFitness = fitness(runBest, candidates, matrix, remainingReqs);
                if (runFitness < bestFitness) {
                    bestFitness = runFitness;
                    bestBits = Arrays.copyOf(runBest, runBest.length);
                }
            }
            
            if (bestBits != null) {
                for (int i = 0; i < bestBits.length; i++) {
                    if (bestBits[i]) bestSelected.add(candidates.get(i));
                }
            }
        } else if (!remainingReqs.isEmpty()) {
            // Нет кандидатов, но требования не покрыты - включаем всех
            bestSelected.addAll(candidates);
        }

        long end = System.nanoTime();
        List<String> names = new ArrayList<>();
        BitSet finalCoverage = new BitSet();
        for (int t : bestSelected) {
            names.add(matrix.tests().get(t).testName());
            finalCoverage.or(matrix.coverageForTest(t));
        }

        double coveragePercent = matrix.elementCount() == 0 ? 1.0 : 
                (double) finalCoverage.cardinality() / matrix.elementCount();

        return new MinimizationResult("GeneticAlgorithm", bestSelected, names, coveragePercent, end - start);
    }

    private boolean[] runGA(List<Integer> candidates, CoverageMatrix matrix, BitSet remainingReqs, Random random) {
        int n = candidates.size();
        List<boolean[]> population = new ArrayList<>();
        
        for (int i = 0; i < populationSize; i++) {
            boolean[] chromosome = new boolean[n];
            for (int j = 0; j < n; j++) {
                chromosome[j] = random.nextDouble() < initSelectProb;
            }
            repair(chromosome, candidates, matrix, remainingReqs);
            population.add(chromosome);
        }

        boolean[] best = null;
        double bestFitness = Double.MAX_VALUE;

        for (int iter = 0; iter < iterations; iter++) {
            double[] scores = new double[populationSize];
            for (int i = 0; i < populationSize; i++) {
                scores[i] = fitness(population.get(i), candidates, matrix, remainingReqs);
                if (scores[i] < bestFitness) {
                    bestFitness = scores[i];
                    best = Arrays.copyOf(population.get(i), n);
                }
            }

            int eliteCount = Math.max(1, (int) Math.round(elitismRate * populationSize));
            Integer[] idx = new Integer[populationSize];
            for (int i = 0; i < populationSize; i++) idx[i] = i;
            Arrays.sort(idx, Comparator.comparingDouble(i -> scores[i]));

            List<boolean[]> newPopulation = new ArrayList<>();
            for (int i = 0; i < eliteCount; i++) {
                newPopulation.add(Arrays.copyOf(population.get(idx[i]), n));
            }

            while (newPopulation.size() < populationSize) {
                boolean[] p1 = tournament(population, scores, random);
                boolean[] p2 = tournament(population, scores, random);

                boolean[] c1 = Arrays.copyOf(p1, n);
                boolean[] c2 = Arrays.copyOf(p2, n);

                if (n > 1 && random.nextDouble() < crossoverRate) {
                    int point = 1 + random.nextInt(n - 1);
                    for (int i = point; i < n; i++) {
                        boolean tmp = c1[i];
                        c1[i] = c2[i];
                        c2[i] = tmp;
                    }
                }

                mutate(c1, random);
                mutate(c2, random);

                repair(c1, candidates, matrix, remainingReqs);
                repair(c2, candidates, matrix, remainingReqs);

                newPopulation.add(c1);
                if (newPopulation.size() < populationSize) newPopulation.add(c2);
            }
            population = newPopulation;
        }

        return best != null ? best : population.get(0);
    }

    private double fitness(boolean[] chromosome, List<Integer> candidates, CoverageMatrix matrix, BitSet remainingReqs) {
        int selectedCount = 0;
        BitSet covered = new BitSet();
        
        for (int i = 0; i < chromosome.length; i++) {
            if (chromosome[i]) {
                selectedCount++;
                covered.or(matrix.coverageForTest(candidates.get(i)));
            }
        }

        // Клонируем оставшиеся требования и убираем те, что мы покрыли
        BitSet uncovered = (BitSet) remainingReqs.clone();
        uncovered.andNot(covered);
        
        return selectedCount + 1000.0 * uncovered.cardinality();
    }

    private boolean[] tournament(List<boolean[]> population, double[] scores, Random random) {
        int best = -1;
        for (int i = 0; i < tournamentSize; i++) {
            int candidate = random.nextInt(population.size());
            if (best == -1 || scores[candidate] < scores[best]) {
                best = candidate;
            }
        }
        return Arrays.copyOf(population.get(best), population.get(best).length);
    }

    private void mutate(boolean[] chromosome, Random random) {
        for (int i = 0; i < chromosome.length; i++) {
            if (random.nextDouble() < mutationRate) chromosome[i] = !chromosome[i];
        }
    }

    private void repair(boolean[] chromosome, List<Integer> candidates, CoverageMatrix matrix, BitSet remainingReqs) {
        BitSet uncovered = (BitSet) remainingReqs.clone();
        
        for (int i = 0; i < chromosome.length; i++) {
            if (chromosome[i]) {
                uncovered.andNot(matrix.coverageForTest(candidates.get(i)));
            }
        }

        while (!uncovered.isEmpty()) {
            int bestIdx = -1;
            int bestGain = 0;
            
            for (int i = 0; i < candidates.size(); i++) {
                if (chromosome[i]) continue;
                
                BitSet intersection = (BitSet) matrix.coverageForTest(candidates.get(i)).clone();
                intersection.and(uncovered);
                int gain = intersection.cardinality();
                
                if (gain > bestGain) {
                    bestGain = gain;
                    bestIdx = i;
                }
            }

            if (bestIdx == -1) break;

            chromosome[bestIdx] = true;
            uncovered.andNot(matrix.coverageForTest(candidates.get(bestIdx)));
        }
    }
}