package de.ascendro.f4m.service.analytics.module.statistic.model.base;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;

public abstract class BaseStatisticTable {
    private static final String TABLE_NAME_FIELD = "TABLE_NAME";
    private static final String TABLE_KEY_FIELD = "KEY_FIELD";
    private static final String TABLE_FIELD_PREFIX = "FIELD_";
    private static final String TABLE_SUBTRACT_FIELD_PREFIX = "FIELD_SUBTRACT_";


    protected static final HashMap<Class<?>, LinkedList<String>> fieldsMap = new HashMap<>();
    protected static final HashMap<Class<?>, HashMap<String, String>> customFieldsMap = new HashMap<>();
    protected static final HashMap<Class<?>, LinkedList<String>> keysMap = new HashMap<>();
    protected final HashMap<String, Object> valuesMap = new HashMap<>();
    protected static final HashMap<Class<?>, String> updateQueries = new HashMap<>();

    protected static <T> LinkedList<String> getFieldList(Class<T> entityClass) {
        return fieldsMap.computeIfAbsent(entityClass, k -> {
            LinkedList<String> fieldList = new LinkedList<>();

            Arrays.stream(getDeclaredFields(entityClass))
                    .filter(f -> f.getName().startsWith(TABLE_FIELD_PREFIX))
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            final String fieldName = (String) f.get(null);
                            fieldList.add(fieldName);
                        } catch (IllegalAccessException e) {
                            throw new F4MAnalyticsFatalErrorException("Error accessing table field", e);
                        }
                    });
            return fieldList;
        });
    }

    public static <T> LinkedList<String> getKeyList(Class<T> entityClass) {
        return keysMap.computeIfAbsent(entityClass, k -> {
            LinkedList<String> keyList = new LinkedList<>();

            Arrays.stream(getDeclaredFields(entityClass))
                    .filter(f -> f.getName().startsWith(TABLE_KEY_FIELD))
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            final String fieldName = (String) f.get(null);
                            keyList.add(fieldName);
                        } catch (IllegalAccessException e) {
                            throw new F4MAnalyticsFatalErrorException("Table key fields are not accessible", e);
                        }
                    });
            return keyList;
        });
    }

    protected static <T> HashMap<String, String> getCustomFieldList(Class<T> entityClass) {
        return customFieldsMap.computeIfAbsent(entityClass, k -> new HashMap<>());
    }

    protected static String getOldValueField(String fieldName) {
        return "@old_" + fieldName;
    }

    private static String getOldValueBackupStatement(String fieldName) {
        return fieldName + " = " + getOldValueField(fieldName) + ":=" + fieldName;
    }

    public static <T> String getTableName(Class<T> entityClass) {
        String tableName;
        try {
            Field field = entityClass.getDeclaredField(TABLE_NAME_FIELD);
            field.setAccessible(true);
            tableName = (String) field.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new F4MAnalyticsFatalErrorException("Invalid table class. Static field TABLE_NAME not present or not accessible.", e);
        }

        return tableName;
    }

    private static char getOperator(String fieldName) {
        if (fieldName.startsWith(TABLE_SUBTRACT_FIELD_PREFIX)) {
            return '-';
        } else {
            return '+';
        }
    }

    public static synchronized <T> String getUpdateStatement(Class<T> entityClass) {
        String tableName = getTableName(entityClass);

        final LinkedList<String> fieldList = getFieldList(entityClass);
        final LinkedList<String> keyList = getKeyList(entityClass);
        final HashMap<String, String> customFields = getCustomFieldList(entityClass);

        final String queryKeyFilter = keyList.stream()
                .map(f -> Arrays.asList(f + " = ?"))
                .flatMap(List::stream).collect(Collectors.joining(" AND "));

        if (StringUtils.isBlank(queryKeyFilter)) {
            throw new F4MAnalyticsFatalErrorException("Table key fields are not defined");
        }

        return updateQueries.computeIfAbsent(entityClass, k -> "UPDATE " + tableName + " SET " +
                fieldList.stream()
                        .map(fieldName -> Arrays.asList(getOldValueBackupStatement(fieldName),
                            fieldName + " = " +
                            ((customFields.get(fieldName) != null) ? customFields.get(fieldName) :
                                    fieldName + " " + getOperator(fieldName) + " ?")))
                        .flatMap(List::stream).collect(Collectors.joining(", ")) +
                " WHERE " + queryKeyFilter);
    }

    private int prepareFields(PreparedStatement preparedStatement) throws SQLException {
        int index = 1;
        for (String fieldName : getFieldList(this.getClass())) {
            Object value = valuesMap.get(fieldName);
            if (value == null)
                preparedStatement.setLong(index++, 0);
            else if (value instanceof BigDecimal)
                preparedStatement.setBigDecimal(index++, (BigDecimal) value);
            else if (value instanceof Long)
                preparedStatement.setLong(index++, (Long) value);
            else if (value instanceof Double)
                preparedStatement.setDouble(index++, (Double) value);
            else
                throw new F4MAnalyticsFatalErrorException("Value type not implemented");
        }

        return index;
    }

    public synchronized void prepareBatch(PreparedStatement preparedStatement) throws SQLException {
        if (getFieldList(this.getClass()) == null)
            throw new F4MAnalyticsFatalErrorException("Fields map not initialized. Call getUpdateStatement() first.");

        int index = prepareFields(preparedStatement);

        //Set key field values
        for (String keyFieldName : getKeyList(this.getClass())) {
            Object keyValue = valuesMap.get(keyFieldName);
            if (keyValue == null)
                throw new F4MAnalyticsFatalErrorException("Table key value is null");
            if (keyValue instanceof String)
                preparedStatement.setString(index++, (String) keyValue);
            else if (keyValue instanceof Long)
                preparedStatement.setLong(index++, (Long) keyValue);
        }
        preparedStatement.addBatch();
    }

    public void clearValues() {
        valuesMap.clear();
    }

    public void setValue(String field, Number number) {
        valuesMap.put(field, number);
    }

    public void setValue(String field, String value) {
        valuesMap.put(field, value);
    }

    public BaseStatisticTable appendedValue(String field, Number number) {
        valuesMap.put(field, number);
        return this;
    }

    private static Field[] getDeclaredFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Arrays.sort(fields, (field1, field2) -> {
            Order order1 = field1.getAnnotation(Order.class);
            Order order2 = field2.getAnnotation(Order.class);

            if (order1 != null && order2 != null) {
                return order1.value() - order2.value();
            } else if (order1 != null) {
                return -1;
            } else if (order2 != null) {
                return 1;
            }
            return field1.getName().compareTo(field2.getName());
        });

        return fields;
    }

}
