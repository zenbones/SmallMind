package org.smallmind.persistence.orm;

public abstract class ProxySession {

   private String dataSource;
   private boolean enforceBoundary;
   private boolean allowCascade;

   public ProxySession(String dataSource, boolean enforceBoundary, boolean allowCascade) {

      this.dataSource = dataSource;
      this.enforceBoundary = enforceBoundary;
      this.allowCascade = allowCascade;
   }

   public void register() {

      SessionManager.registerSession(dataSource, this);
   }

   public String getDataSource() {

      return dataSource;
   }

   public boolean willEnforceBoundary() {

      return enforceBoundary;
   }

   public boolean willAllowCascade() {

      return allowCascade;
   }

   public abstract Object getNativeSession();

   public abstract void setIgnoreBoundaryEnforcement(boolean ignoreBoundaryEnforcement);

   public abstract ProxyTransaction beginTransaction();

   public abstract ProxyTransaction currentTransaction();

   public abstract void flush();

   public abstract boolean isClosed();

   public abstract void close();
}
