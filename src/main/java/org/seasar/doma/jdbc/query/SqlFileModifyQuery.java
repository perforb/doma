/*
 * Copyright 2004-2010 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.jdbc.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.seasar.doma.internal.expr.ExpressionEvaluator;
import org.seasar.doma.internal.expr.Value;
import org.seasar.doma.internal.jdbc.sql.NodePreparedSqlBuilder;
import org.seasar.doma.internal.jdbc.sql.SqlContext;
import org.seasar.doma.internal.jdbc.sql.node.ExpandNode;
import org.seasar.doma.internal.jdbc.sql.node.PopulateNode;
import org.seasar.doma.jdbc.PreparedSql;
import org.seasar.doma.jdbc.SqlExecutionSkipCause;
import org.seasar.doma.jdbc.SqlFile;
import org.seasar.doma.jdbc.SqlKind;
import org.seasar.doma.jdbc.SqlLogType;
import org.seasar.doma.jdbc.entity.EntityType;

/**
 * @author taedium
 * 
 */
public abstract class SqlFileModifyQuery extends AbstractQuery implements
        ModifyQuery {

    protected static final String[] EMPTY_STRINGS = new String[] {};

    protected final SqlKind kind;

    protected String sqlFilePath;

    protected final Map<String, Value> parameters = new LinkedHashMap<String, Value>();

    protected PreparedSql sql;

    protected boolean optimisticLockCheckRequired;

    protected SqlLogType sqlLogType;

    protected String[] includedPropertyNames = EMPTY_STRINGS;

    protected String[] excludedPropertyNames = EMPTY_STRINGS;

    protected boolean executable;

    protected SqlExecutionSkipCause sqlExecutionSkipCause = SqlExecutionSkipCause.STATE_UNCHANGED;

    protected SqlFileModifyQuery(SqlKind kind) {
        assertNotNull(kind);
        this.kind = kind;
    }

    protected void prepareOptions() {
        if (queryTimeout <= 0) {
            queryTimeout = config.getQueryTimeout();
        }
    }

    protected void prepareSql() {
        SqlFile sqlFile = config.getSqlFileRepository().getSqlFile(method,
                sqlFilePath, config.getDialect());
        ExpressionEvaluator evaluator = new ExpressionEvaluator(parameters,
                config.getDialect().getExpressionFunctions(),
                config.getClassHelper());
        NodePreparedSqlBuilder sqlBuilder = new NodePreparedSqlBuilder(config,
                kind, sqlFile.getPath(), evaluator, sqlLogType,
                this::expandColumns, this::populateValues);
        sql = sqlBuilder.build(sqlFile.getSqlNode(), this::comment);
    }

    protected List<String> expandColumns(ExpandNode node) {
        throw new UnsupportedOperationException();
    }

    protected void populateValues(PopulateNode node, SqlContext context) {
        throw new UnsupportedOperationException();
    }

    public void setSqlFilePath(String sqlFilePath) {
        this.sqlFilePath = sqlFilePath;
    }

    public void addParameter(String name, Class<?> type, Object value) {
        assertNotNull(name, type);
        addParameterInternal(name, type, value);
    }

    public void addParameterInternal(String name, Class<?> type, Object value) {
        parameters.put(name, new Value(type, value));
    }

    public void setSqlLogType(SqlLogType sqlLogType) {
        this.sqlLogType = sqlLogType;
    }

    public void setIncludedPropertyNames(String... includedPropertyNames) {
        this.includedPropertyNames = includedPropertyNames;
    }

    public void setExcludedPropertyNames(String... excludedPropertyNames) {
        this.excludedPropertyNames = excludedPropertyNames;
    }

    @Override
    public PreparedSql getSql() {
        return sql;
    }

    @Override
    public boolean isOptimisticLockCheckRequired() {
        return optimisticLockCheckRequired;
    }

    @Override
    public boolean isExecutable() {
        return executable;
    }

    @Override
    public SqlExecutionSkipCause getSqlExecutionSkipCause() {
        return sqlExecutionSkipCause;
    }

    @Override
    public boolean isAutoGeneratedKeysSupported() {
        return false;
    }

    @Override
    public SqlLogType getSqlLogType() {
        return sqlLogType;
    }

    public abstract <E> void setEntityAndEntityType(String name, E entity,
            EntityType<E> entityType);

    @Override
    public String toString() {
        return sql != null ? sql.toString() : null;
    }

}
