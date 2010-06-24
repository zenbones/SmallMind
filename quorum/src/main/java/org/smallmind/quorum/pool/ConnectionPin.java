package org.smallmind.quorum.pool;

import java.util.LinkedList;

public class ConnectionPin<C> {

   private enum State {

      FREE, SERVED, CLOSED
   }

   private DeconstructionWorker deconstructionWorker;
   private ConnectionInstance<C> connectionInstance;
   private State state;
   private boolean commissioned = true;

   public ConnectionPin (ConnectionPool connectionPool, ConnectionInstance<C> connectionInstance, int maxIdleTimeSeconds, int leaseTimeSeconds, int unreturnedConnectionTimeoutSeconds) {

      Thread workerThread;
      LinkedList<DeconstructionFuse> fuseList;

      this.connectionInstance = connectionInstance;

      state = State.FREE;

      fuseList = new LinkedList<DeconstructionFuse>();

      if (leaseTimeSeconds > 0) {
         fuseList.add(new LeaseTimeDeconstructionFuse(leaseTimeSeconds));
      }

      if (maxIdleTimeSeconds > 0) {
         fuseList.add(new MaxIdleTimeDeconstructionFuse(maxIdleTimeSeconds));
      }

      if (unreturnedConnectionTimeoutSeconds > 0) {
         fuseList.add(new UnreturnedConnectionTimeoutDeconstructionFuse(unreturnedConnectionTimeoutSeconds));
      }

      if (!fuseList.isEmpty()) {
         deconstructionWorker = new DeconstructionWorker(connectionPool, this, fuseList);
         workerThread = new Thread(deconstructionWorker);
         workerThread.setDaemon(true);
         workerThread.start();

         deconstructionWorker.free();
      }
   }

   protected void abort () {

      if (deconstructionWorker != null) {
         deconstructionWorker.abort();
      }
   }

   public void decommission () {

      commissioned = false;
   }

   public boolean isComissioned () {

      return commissioned;
   }

   public boolean isFree () {

      return state.equals(State.FREE);
   }

   public boolean isServed () {

      return state.equals(State.SERVED);
   }

   public boolean isClosed () {

      return state.equals(State.CLOSED);
   }

   public boolean contains (ConnectionInstance connectionInstance) {

      return this.connectionInstance == connectionInstance;
   }

   public boolean validate () {

      return connectionInstance.validate();
   }

   public C serve ()
      throws Exception {

      C connection;

      if (!state.equals(State.FREE)) {
         throw new ConnectionPoolException("An attempt to serve this connection while in state(%s)", state);
      }

      connection = connectionInstance.serve();
      state = State.SERVED;

      if (deconstructionWorker != null) {
         deconstructionWorker.serve();
      }

      return connection;
   }

   public void free () {

      state = State.FREE;

      if (deconstructionWorker != null) {
         deconstructionWorker.free();
      }
   }

   public void close ()
      throws Exception {

      if (!state.equals(State.CLOSED)) {
         state = State.CLOSED;
         connectionInstance.close();
      }
   }

   public void finalize () {

      try {
         close();
      }
      catch (Exception exception) {
         ConnectionPoolManager.logError(exception);
      }
      finally {
         abort();
      }
   }
}