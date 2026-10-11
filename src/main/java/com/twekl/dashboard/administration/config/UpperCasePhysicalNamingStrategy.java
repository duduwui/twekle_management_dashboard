package com.twekl.dashboard.administration.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Locale;

/**
 * Physical naming strategy enforcing ALL UPPERCASE snake_case identifiers
 * for database tables, sequences, and columns.
 */
public class UpperCasePhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        Identifier identifier = super.toPhysicalTableName(logicalName, jdbcEnvironment);
        return toUpper(identifier);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        Identifier identifier = super.toPhysicalColumnName(logicalName, jdbcEnvironment);
        return toUpper(identifier);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        Identifier identifier = super.toPhysicalSequenceName(logicalName, jdbcEnvironment);
        return toUpper(identifier);
    }

    private Identifier toUpper(Identifier identifier) {
        if (identifier == null) {
            return null;
        }
        return Identifier.toIdentifier(identifier.getText().toUpperCase(Locale.ROOT), identifier.isQuoted());
    }
}

