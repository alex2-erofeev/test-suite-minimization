package ru.erofeev.fl.parser;

import ru.erofeev.fl.coverage.CoverageMatrix;

/** Единственная реализация — JacocoParser. */
public interface CoverageParser {
    CoverageMatrix parse(String dataDir) throws Exception;
}