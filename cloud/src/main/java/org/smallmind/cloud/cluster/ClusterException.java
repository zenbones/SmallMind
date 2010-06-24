package org.smallmind.cloud.cluster;

import org.smallmind.nutsnbolts.lang.FormattedException;

public class ClusterException extends FormattedException {

   public ClusterException () {

      super();
   }

   public ClusterException (String message, Object... args) {

      super(message, args);
   }

   public ClusterException (Throwable throwable, String message, Object... args) {

      super(throwable, message, args);
   }

   public ClusterException (Throwable throwable) {

      super(throwable);
   }
}
