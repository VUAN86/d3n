package de.ascendro.f4m.service.analytics.module.statistic.query.base;

import static de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest.NEW_LINE;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.google.inject.Provider;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.jdbc.InjectConfig;
import de.ascendro.f4m.service.analytics.module.statistic.jdbc.StatisticStatementHandler;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public abstract class BaseUpdater<T extends BaseStatisticTable> implements ITableUpdater<T> {
    private Provider<Connection> provider;
    private HashSet<T> updateSet = new HashSet<>();
    private Class<T> tableClass;
    private long lastUpdate = System.currentTimeMillis();
    protected final Config config;

    public static final int BATCH_SIZE = 1000;
    protected Integer batchSize;

    public static final int UPDATE_TRIGGER = 60;
    protected Integer updateTrigger;

    private static final String ERROR_MESSAGE_SUBJECT = "Error executing statistic query statement";
    private static final String ERROR_MESSAGE_BODY = "Error message: {0} " + NEW_LINE + " Query list: {1}";

    @InjectLogger
    private static Logger LOGGER;

    private final NotificationCommon notificationUtil;

    @Inject
    protected BaseUpdater(Config config, NotificationCommon notificationUtil) {
        this.config = config;
        this.notificationUtil = notificationUtil;
        batchSize = config.getPropertyAsInteger(AnalyticsConfig.MYSQL_BATCH_SIZE);
        updateTrigger = config.getPropertyAsInteger(AnalyticsConfig.MYSQL_UPDATE_TRIGGER_IN_SECONDS);
        tableClass = getTableClass();
    }

    @Override
    public Connection getConnection() {
        return provider.get();
    }

    @Inject
    public void setConnection(@InjectConfig Provider<Connection> provider) {
        this.provider = provider;
    }

    @Override
    public void handleEvent(EventContent content) {
        try {
            processEvent(content);
        } catch (Exception e) {
            LOGGER.error("Error on event handle", e);
        }
    }

    protected abstract Class<T> getTableClass();

    protected <E extends BaseEvent> void process(EventContent content, Renderer<T, E> renderer) throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException, SQLException {
        E event = content.getEventData();

        preProcess(content, event);

        T table = tableClass.newInstance();
        renderer.render(table, event);
        LOGGER.debug("process table={}",table);
        addRecord(table);
    }

    @Override
    public synchronized void addRecord(T table) throws SQLException {
        updateSet.add(table);
        persist();
    }

    protected synchronized void persist() throws SQLException {
        if (updateSet.size() == batchSize) {
            executeBatch();
            updateSet.clear();
        }
    }

    @Override
    public synchronized void triggerBatchExecute() throws SQLException {
        if ((System.currentTimeMillis() - lastUpdate) / 1000 >= updateTrigger &&
                !updateSet.isEmpty()) {
            executeBatch();
            updateSet.clear();
        } else {
            LOGGER.debug("triggerBatchExecute ELSE ");
        }
    }

    protected abstract<E> void preProcess (EventContent content, E event) throws SQLException;


    protected synchronized void executeBatch() throws SQLException {
        StatisticStatementHandler.StatisticStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = getConnection();
            preparedStatement = StatisticStatementHandler.getStatementInstance(connection.prepareStatement(BaseStatisticTable.getUpdateStatement(tableClass)));
            LOGGER.debug("executeBatch updateSet={} START:",updateSet);
            HashMap<String, Object> settedValuesMap = new HashMap<>();

            for (T table:updateSet) {
                LOGGER.debug("executeBatch table={} preparedStatement={}", table, preparedStatement);
                table.prepareBatch(preparedStatement,settedValuesMap);
            }
            LOGGER.debug("executeBatch preparedStatement2={}", preparedStatement);
            int[] results = preparedStatement.executeBatch();
            LOGGER.debug("{} executed batch update for {} records", this.getClass().getSimpleName(), results.length);
        } catch (BatchUpdateException ex) {
            int[] updateCount = ex.getUpdateCounts();
            StringBuilder message = new StringBuilder();
            if (preparedStatement!=null) {
                final StatisticStatementHandler.StatisticStatement errorStatement = preparedStatement;
                IntStream.range(0, updateCount.length)
                        .filter(n -> updateCount[n] == Statement.EXECUTE_FAILED)
                        .forEach(n -> message.append("Query with error: ").append(NEW_LINE).append(errorStatement.getBatchQuery(n)).append(NEW_LINE));
            }
            String[] bodyParams = new String[2];
            bodyParams[0] = ex.getMessage();
            bodyParams[1] = message.toString();
            notificationUtil.sendEmailToAdmin(ERROR_MESSAGE_SUBJECT, null, ERROR_MESSAGE_BODY, bodyParams);
            LOGGER.error(ERROR_MESSAGE_SUBJECT, ex);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException sqlException) {
                    LOGGER.error("Error closing statement", sqlException);
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqlException) {
                    LOGGER.error("Error closing database connection", sqlException);
                }
            }
        }
        lastUpdate = System.currentTimeMillis();
    }

    protected int incrementIfTrue(Boolean value) {
        if (value==null)
            return 0;
        else
            return Boolean.compare(value, false);
    }

    protected long checkLong(Long value) {
        if (value==null)
            return 0;
        else
            return value;
    }

    protected int checkInt(Integer value) {
        if (value==null)
            return 0;
        else
            return value;
    }

    protected double checkDouble(Double value) {
        if (value==null)
            return 0;
        else
            return value;
    }

    protected BigDecimal checkBigDecimal(BigDecimal value) {
        if (value==null)
            return new BigDecimal(0);
        else
            return value;
    }
}
