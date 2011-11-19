package org.smallmind.quorum.pool2;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.smallmind.nutsnbolts.lang.StackTrace;
import org.smallmind.quorum.pool2.event.ConnectionPoolEventListener;
import org.smallmind.quorum.pool2.event.ErrorReportingConnectionPoolEvent;
import org.smallmind.quorum.pool2.event.LeaseTimeReportingConnectionPoolEvent;
import org.smallmind.quorum.pool2.remote.RemoteConnectionPoolSurface;
import org.smallmind.quorum.transport.remote.RemoteEndpoint;

public class ConnectionPool<C> implements RemoteConnectionPoolSurface, RemoteEndpoint {

  private static final Class[] REMOTE_INTERFACES = new Class[] {RemoteConnectionPoolSurface.class};

  private final ConcurrentLinkedQueue<ConnectionPoolEventListener> connectionPoolEventListenerQueue = new ConcurrentLinkedQueue<ConnectionPoolEventListener>();
  private final ConnectionInstanceFactory<C> connectionInstanceFactory;
  private final ConnectionPinManager<C> connectionPinManager;
  private final String name;

  private ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();

  public ConnectionPool (String name, ConnectionInstanceFactory<C> connectionInstanceFactory) {

    this.name = name;
    this.connectionInstanceFactory = connectionInstanceFactory;

    connectionPinManager = new ConnectionPinManager<C>(this);
  }

  public ConnectionPool (String name, ConnectionInstanceFactory<C> connectionInstanceFactory, ConnectionPoolConfig connectionPoolConfig) {

    this(name, connectionInstanceFactory);

    this.connectionPoolConfig = connectionPoolConfig;
  }

  @Override
  public String getPoolName () {

    return name;
  }

  public ConnectionInstanceFactory<C> getConnectionInstanceFactory () {

    return connectionInstanceFactory;
  }

  public ConnectionPoolConfig getConnectionPoolConfig () {

    return connectionPoolConfig;
  }

  public void setConnectionPoolConfig (ConnectionPoolConfig connectionPoolConfig) {

    this.connectionPoolConfig = connectionPoolConfig;
  }

  public StackTrace[] getExistentialStackTraces () {

    return connectionPinManager.getExistentialStackTraces();
  }

  public Class[] getProxyInterfaces () {

    return REMOTE_INTERFACES;
  }

  @Override
  public void addConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPoolEventListenerQueue.add(listener);
  }

  @Override
  public void removeConnectionPoolEventListener (ConnectionPoolEventListener listener) {

    connectionPoolEventListenerQueue.remove(listener);
  }

  public void reportConnectionErrorOccurred (Exception exception) {

    ErrorReportingConnectionPoolEvent poolEvent = new ErrorReportingConnectionPoolEvent<C>(this, exception);

    for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
      listener.reportConnectionErrorOccurred(poolEvent);
    }
  }

  public void reportConnectionLeaseTimeNanos (long leaseTimeNanos) {

    LeaseTimeReportingConnectionPoolEvent poolEvent = new LeaseTimeReportingConnectionPoolEvent<C>(this, leaseTimeNanos);

    for (ConnectionPoolEventListener listener : connectionPoolEventListenerQueue) {
      listener.reportConnectionLeaseTime(poolEvent);
    }
  }

  @Override
  public void startup ()
    throws ConnectionPoolException {

    connectionPinManager.startup();
  }

  @Override
  public void shutdown ()
    throws ConnectionPoolException {

    connectionPinManager.shutdown();
  }

  public C getConnection ()
    throws ConnectionPoolException {

    try {

      return connectionPinManager.serve().serve();
    }
    catch (Exception exception) {
      throw new ConnectionPoolException(exception);
    }
  }

  public void returnInstance (ConnectionInstance<C> connectionInstance) {

    connectionPinManager.process(connectionInstance);
  }

  public void terminateInstance (ConnectionInstance<C> connectionInstance) {

    connectionPinManager.terminate(connectionInstance);
  }

  public void removePin (ConnectionPin<C> connectionPin) {

    connectionPinManager.remove(connectionPin);
  }

  @Override
  public int getPoolSize () {

    return connectionPinManager.getPoolSize();
  }

  @Override
  public int getFreeSize () {

    return connectionPinManager.getFreeSize();
  }

  @Override
  public int getProcessingSize () {

    return connectionPinManager.getProcessingSize();
  }

  @Override
  public boolean isTestOnConnect () {

    return connectionPoolConfig.isTestOnConnect();
  }

  @Override
  public void setTestOnConnect (boolean testOnConnect) {

    connectionPoolConfig.setTestOnConnect(testOnConnect);
  }

  @Override
  public boolean isTestOnAcquire () {

    return connectionPoolConfig.isTestOnAcquire();
  }

  @Override
  public void setTestOnAcquire (boolean testOnAcquire) {

    connectionPoolConfig.setTestOnAcquire(testOnAcquire);
  }

  @Override
  public boolean isReportLeaseTimeNanos () {

    return connectionPoolConfig.isReportLeaseTimeNanos();
  }

  @Override
  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    connectionPoolConfig.setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  @Override
  public boolean isExistentiallyAware () {

    return connectionPoolConfig.isExistentiallyAware();
  }

  @Override
  public void setExistentiallyAware (boolean existentiallyAware) {

    connectionPoolConfig.setExistentiallyAware(existentiallyAware);
  }

  @Override
  public long getConnectionTimeoutMillis () {

    return connectionPoolConfig.getConnectionTimeoutMillis();
  }

  @Override
  public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    connectionPoolConfig.setConnectionTimeoutMillis(connectionTimeoutMillis);
  }

  @Override
  public long getAcquireWaitTimeMillis () {

    return connectionPoolConfig.getAcquireWaitTimeMillis();
  }

  @Override
  public void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    connectionPoolConfig.setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  @Override
  public int getInitialPoolSize () {

    return connectionPoolConfig.getInitialPoolSize();
  }

  @Override
  public int getMinPoolSize () {

    return connectionPoolConfig.getMinPoolSize();
  }

  @Override
  public void setMinPoolSize (int minPoolSize) {

    connectionPoolConfig.setMinPoolSize(minPoolSize);
  }

  @Override
  public int getMaxPoolSize () {

    return connectionPoolConfig.getMaxPoolSize();
  }

  @Override
  public void setMaxPoolSize (int maxPoolSize) {

    connectionPoolConfig.setMaxPoolSize(maxPoolSize);
  }

  @Override
  public int getMaxLeaseTimeSeconds () {

    return connectionPoolConfig.getMaxLeaseTimeSeconds();
  }

  @Override
  public void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    connectionPoolConfig.setMaxLeaseTimeSeconds(maxLeaseTimeSeconds);
  }

  @Override
  public int getMaxIdleTimeSeconds () {

    return connectionPoolConfig.getMaxIdleTimeSeconds();
  }

  @Override
  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    connectionPoolConfig.setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  @Override
  public int getUnreturnedConnectionTimeoutSeconds () {

    return connectionPoolConfig.getUnreturnedConnectionTimeoutSeconds();
  }

  @Override
  public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

    connectionPoolConfig.setUnreturnedConnectionTimeoutSeconds(unreturnedConnectionTimeoutSeconds);
  }
}
