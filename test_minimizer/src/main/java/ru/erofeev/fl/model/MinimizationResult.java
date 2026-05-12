package ru.erofeev.fl.model;

import java.util.List;
import java.util.Set;

public class MinimizationResult {

    private final String algorithmName;
    private final Set<Integer> selectedTests;
    private final List<String> selectedTestNames;
    private final double coverage;
    private final long executionTimeNanos;
    private String peakCpu;

    public MinimizationResult(String algorithmName,
                              Set<Integer> selectedTests,
                              List<String> selectedTestNames,
                              double coverage,
                              long executionTimeNanos) {
        this.algorithmName = algorithmName;
        this.selectedTests = selectedTests;
        this.selectedTestNames = selectedTestNames;
        this.coverage = coverage;
        this.executionTimeNanos = executionTimeNanos;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public Set<Integer> getSelectedTests() {
        return selectedTests;
    }

    public List<String> getSelectedTestNames() {
        return selectedTestNames;
    }

    public double getCoverage() {
        return coverage;
    }

    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }

    public int getSelectedCount() {
        return selectedTests.size();
    }

    public double getExecutionTimeMillis() {
        return executionTimeNanos / 1_000_000.0;
    }

    public String getPeakCpu() {
        return peakCpu != null ? peakCpu : "n/a";
    }

    public void setPeakCpu(String peakCpu) {
        this.peakCpu = peakCpu;
    }
}