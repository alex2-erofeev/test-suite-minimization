package ru.erofeev.fl.algorithm;

import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.model.MinimizationResult;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NaiveAlgorithm implements TestSuiteMinimizationAlgorithm {

    @Override
    public String getName() {
        return "Naive";
    }

    @Override
    public MinimizationResult run(CoverageMatrix matrix) {
        long start = System.nanoTime();
        
        int numTests = matrix.testCount();
        Set<Integer> selected = new LinkedHashSet<>();
        
        // 1. Сначала берем все тесты и вычисляем максимальное (полное) покрытие
        BitSet maxPossibleCoverage = new BitSet(matrix.elementCount());
        for (int t = 0; t < numTests; t++) {
            selected.add(t);
            maxPossibleCoverage.or(matrix.coverageForTest(t));
        }
        int maxCardinality = maxPossibleCoverage.cardinality();

        // 2. Обратный жадный проход (пытаемся удалить тесты по одному)
        for (int t = 0; t < numTests; t++) {
            selected.remove(t);
            
            // Считаем покрытие без этого теста
            BitSet currentCoverage = new BitSet(matrix.elementCount());
            for (int s : selected) {
                currentCoverage.or(matrix.coverageForTest(s));
            }
            
            // Если покрытие упало, возвращаем тест обратно
            if (currentCoverage.cardinality() < maxCardinality) {
                selected.add(t);
            }
        }

        // Формируем результат
        long end = System.nanoTime();
        List<String> names = new ArrayList<>();
        for (int t : selected) {
            names.add(matrix.tests().get(t).testName());
        }

        double coveragePercent = matrix.elementCount() == 0 ? 1.0 : 
                (double) maxCardinality / matrix.elementCount();

        return new MinimizationResult(
                getName(), 
                selected, 
                names, 
                coveragePercent, 
                end - start
        );
    }
}