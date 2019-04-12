package de.ascendro.f4m.service.analytics.module.statistic.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class StatisticStatementHandler implements InvocationHandler {

    private final PreparedStatement delegate;
    private int count = 0;
    private ArrayList<String> queryList = new ArrayList<>();

    private static final String BATCH_COUNT_METHOD = "getBatchCount";
    private static final String BATCH_ADD_METHOD = "addBatch";
    private static final String GET_BATCH_QUERY_METHOD = "getBatchQuery";

    private StatisticStatementHandler(PreparedStatement delegate) {
        this.delegate = delegate;
    }

    public static StatisticStatement getStatementInstance(PreparedStatement delegate) {
        return (StatisticStatement)Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                new Class[] {StatisticStatement.class}, new StatisticStatementHandler(delegate));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (BATCH_COUNT_METHOD.equals(method.getName())) {
            return count;
        }
        if (GET_BATCH_QUERY_METHOD.equals(method.getName())) {
            return queryList.get((int)args[0]);
        }
        String query = "NA";
        if (BATCH_ADD_METHOD.equals(method.getName())) {
            query = delegate.toString();
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        } finally {
            if (BATCH_ADD_METHOD.equals(method.getName())) {
                queryList.add(query);
                ++count;
            }
        }
    }

    public interface StatisticStatement extends PreparedStatement {
        String getBatchQuery(int i);
        int getBatchCount();
    }
}
