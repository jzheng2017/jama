package nl.jiankai.operators;

import nl.jiankai.api.Migration;

import java.util.Map;

public interface MigrationOperator {
    void migrate(Migration migration);
}
