package ru.erofeev.fl.algorithm;

import ru.erofeev.fl.coverage.CoverageMatrix;
import ru.erofeev.fl.model.MinimizationResult;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GreedyEssentialAlgorithm implements TestSuiteMinimizationAlgorithm {

    @Override
    public String getName() {
        return "GreedyEssential";
    }

    @Override
    public MinimizationResult run(CoverageMatrix matrix) {
        long start = System.nanoTime();
        
        Set<Integer> selectedTests = new HashSet<>();
        BitSet uncoveredElements = new BitSet(matrix.elementCount());
        
        // 1. Кэшируем покрытие каждого теста и находим все достижимые элементы
        BitSet[] testMasks = new BitSet[matrix.testCount()];
        for (int i = 0; i < matrix.testCount(); i++) {
            testMasks[i] = matrix.coverageForTest(i);
            uncoveredElements.or(testMasks[i]); 
        }
        
        int maxPossibleCoverage = uncoveredElements.cardinality();

        // 2. Шаг Essential: ищем элементы, которые покрываются только ОДНИМ тестом
        for (int e = 0; e < matrix.elementCount(); e++) {
            if (uncoveredElements.get(e)) {
                List<Integer> coveringTests = matrix.testsCoveringElement(e);
                if (coveringTests != null && coveringTests.size() == 1) {
                    int essentialTest = coveringTests.get(0);
                    selectedTests.add(essentialTest);
                    // Удаляем из непокрытых всё, что покрывает этот тест
                    uncoveredElements.andNot(testMasks[essentialTest]);
                }
            }
        }

        // 3. Шаг Greedy: жадно выбираем тесты для оставшихся элементов
        while (!uncoveredElements.isEmpty()) {
            int bestTest = -1;
            int maxCoveredInStep = 0;

            for (int i = 0; i < matrix.testCount(); i++) {
                if (!selectedTests.contains(i)) {
                    // Пересечение: смотрим, сколько НЕПОКРЫТЫХ элементов покроет этот тест
                    BitSet intersection = (BitSet) testMasks[i].clone();
                    intersection.and(uncoveredElements);
                    
                    int coveredCount = intersection.cardinality();
                    if (coveredCount > maxCoveredInStep) {
                        maxCoveredInStep = coveredCount;
                        bestTest = i;
                    }
                }
            }

            if (bestTest == -1) break; // Защита от бесконечного цикла

            selectedTests.add(bestTest);
            uncoveredElements.andNot(testMasks[bestTest]);
        }

        // Формируем результат
        List<String> selectedNames = new ArrayList<>();
        for (int testIndex : selectedTests) {
            selectedNames.add(matrix.tests().get(testIndex).testName());
        }

        double coveragePercent = matrix.elementCount() == 0 ? 1.0 : 
                (double) maxPossibleCoverage / matrix.elementCount();
        
        long executionTime = System.nanoTime() - start;

        return new MinimizationResult(getName(), selectedTests, selectedNames, coveragePercent, executionTime);
    }
}