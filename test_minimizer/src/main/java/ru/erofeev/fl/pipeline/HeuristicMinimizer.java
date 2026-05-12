package ru.erofeev.fl.pipeline;

import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.algorithm.GeneticAlgorithm;
import ru.erofeev.fl.algorithm.GreedyEssentialAlgorithm;
import ru.erofeev.fl.algorithm.NaiveAlgorithm;
import ru.erofeev.fl.algorithm.TestSuiteMinimizationAlgorithm;
import ru.erofeev.fl.model.MinimizationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class HeuristicMinimizer {

    HeuristicResult minimize(CoverageMatrix matrix, MinimizerAlgorithm algorithm) {
        TestSuiteMinimizationAlgorithm delegate = createDelegate(algorithm);
        
        // Засекаем время и запускаем ВАШ алгоритм с НОВОЙ быстрой матрицей
        long fallbackStart = System.nanoTime();
        MinimizationResult rawResult = delegate.run(matrix); 
        long fallbackNanos = System.nanoTime() - fallbackStart;

        // Вызываем getSelectedTests() из вашей модели MinimizationResult
        List<Integer> selectedIndices = new ArrayList<>(rawResult.getSelectedTests());
        Collections.sort(selectedIndices);
        List<String> selectedIds = new ArrayList<>(selectedIndices.size());
        
        for (Integer testIndex : selectedIndices) {
            if (testIndex == null || testIndex < 0 || testIndex >= matrix.testCount()) {
                continue;
            }
            selectedIds.add(matrix.tests().get(testIndex).uniqueTestId());
        }

        // Используем геттеры из вашей модели MinimizationResult
        long solveNanos = rawResult.getExecutionTimeNanos() > 0 ? rawResult.getExecutionTimeNanos() : fallbackNanos;
        
        return new HeuristicResult(
            algorithm, 
            rawResult.getAlgorithmName(), 
            selectedIds, 
            rawResult.getCoverage(), 
            solveNanos
        );
    }

    private static TestSuiteMinimizationAlgorithm createDelegate(MinimizerAlgorithm algorithm) {
        switch (algorithm) {
            case NAIVE:
                return new NaiveAlgorithm();
            case GREEDY_ESSENTIAL:
                return new GreedyEssentialAlgorithm();
            case GENETIC:
                return new GeneticAlgorithm();
            default:
                throw new IllegalArgumentException("Unsupported heuristic algorithm: " + algorithm);
        }
    }
}