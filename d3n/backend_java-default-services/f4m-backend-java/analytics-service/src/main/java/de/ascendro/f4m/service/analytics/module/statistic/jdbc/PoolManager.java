package de.ascendro.f4m.service.analytics.module.statistic.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.slf4j.Logger;

import de.ascendro.f4m.service.analytics.logging.InjectLogger;

public class PoolManager {

    @InjectLogger
    private static Logger LOGGER;

    private final ConnectionPoolDataSource dataSource;
    private final int maxConnections;
    private final int timeout;
    private Semaphore semaphore;
    private Queue<PooledConnection> recycledConnections;
    private int activeConnections;
    private PoolConnectionEventListener poolConnectionEventListener;
    private boolean isDisposed;
    private boolean anyConnectionAttemptMade;

    public static class TimeoutException extends RuntimeException {
        private static final long serialVersionUID = 1;

        public TimeoutException() {
            super("Timeout while waiting for a free database connection.");
        }
    }

    public PoolManager(ConnectionPoolDataSource dataSource, int maxConnections) {
        this(dataSource, maxConnections, 60);
    }

    public PoolManager(ConnectionPoolDataSource dataSource, int maxConnections,
                       int timeout) {
        this.dataSource = dataSource;
        this.maxConnections = maxConnections;
        this.timeout = timeout;

        if (maxConnections < 1)
            throw new IllegalArgumentException("Invalid maxConnections value.");
        semaphore = new Semaphore(maxConnections, true);
        recycledConnections = new LinkedList<>();
        poolConnectionEventListener = new PoolConnectionEventListener();
        anyConnectionAttemptMade = false;
    }

    public synchronized void dispose() throws SQLException {
        if (isDisposed)
            return;
        isDisposed = true;
        SQLException e = null;
        while (!recycledConnections.isEmpty()) {
            PooledConnection pooledConnection = recycledConnections.remove();
            try {
                pooledConnection.close();
            } catch (SQLException e2) {
                if (e == null)
                    e = e2;
            }
        }
        if (e != null)
            throw e;
    }

    public Connection getConnection() throws SQLException {
    	anyConnectionAttemptMade = true;
        // This routine is unsynchronized, because semaphore.tryAcquire() may
        // block.
        synchronized (this) {
            if (isDisposed)
                throw new IllegalStateException(
                        "Connection pool has been disposed.");
        }
        try {
            if (!semaphore.tryAcquire(timeout, TimeUnit.SECONDS))
                throw new TimeoutException();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    "Interrupted while waiting for a database connection.", e);
        }
        boolean ok = false;
        try {
            Connection connection = getConnection2();
            ok = true;
            return connection;
        } finally {
            if (!ok)
                semaphore.release();
        }
    }

    private synchronized Connection getConnection2() throws SQLException {
        if (isDisposed)
            throw new IllegalStateException(
                    "Connection pool has been disposed."); // test again with
        // lock
        PooledConnection pooledConnection;
        if (!recycledConnections.isEmpty()) {
            pooledConnection = recycledConnections.remove();
        } else {
            pooledConnection = dataSource.getPooledConnection();
        }
        Connection connection = pooledConnection.getConnection();
        activeConnections++;
        pooledConnection.addConnectionEventListener(poolConnectionEventListener);
        assertInnerState();
        return connection;
    }

    private synchronized void recycleConnection(PooledConnection pooledConnection) {
        if (isDisposed) {
            disposeConnection(pooledConnection);
            return;
        }
        if (activeConnections <= 0)
            throw new AssertionError();
        activeConnections--;
        semaphore.release();
        recycledConnections.add(pooledConnection);
        assertInnerState();
    }

    private synchronized void disposeConnection(PooledConnection pooledConnection) {
        if (activeConnections <= 0)
            throw new AssertionError();
        activeConnections--;
        semaphore.release();
        closeConnectionNoEx(pooledConnection);
        assertInnerState();
    }

    private void closeConnectionNoEx(PooledConnection pooledConnection) {
        try {
            pooledConnection.close();
        } catch (SQLException e) {
            LOGGER.warn("Error while closing database connection", e);
        }
    }

    private void assertInnerState() {
        if (activeConnections < 0)
            throw new AssertionError();
        if (activeConnections + recycledConnections.size() > maxConnections)
            throw new AssertionError();
        if (activeConnections + semaphore.availablePermits() > maxConnections)
            throw new AssertionError();
    }

    private class PoolConnectionEventListener implements
            ConnectionEventListener {
        @Override
		public void connectionClosed(ConnectionEvent event) {
            PooledConnection pooledConnection = (PooledConnection) event.getSource();
            pooledConnection.removeConnectionEventListener(this);
            recycleConnection(pooledConnection);
        }

        @Override
		public void connectionErrorOccurred(ConnectionEvent event) {
            PooledConnection pooledConnection = (PooledConnection) event.getSource();
            pooledConnection.removeConnectionEventListener(this);
            disposeConnection(pooledConnection);
        }
    }

    public synchronized int getActiveConnections() {
        return activeConnections;
    }

    public synchronized int getRecycledConnections() {
        return recycledConnections.size();
    }

	public boolean isAnyConnectionAttemptMade() {
		return anyConnectionAttemptMade;
	}
}
