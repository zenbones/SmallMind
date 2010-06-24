package org.smallmind.cloud.cluster;

public class ClusterManagementException extends ClusterException {

   public ClusterManagementException () {

      super();
   }

   public ClusterManagementException (String message, Object... args) {

      super(message, args);
   }

   public ClusterManagementException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ClusterManagementException (Throwable throwable) {

      super(throwable);
   }
}
