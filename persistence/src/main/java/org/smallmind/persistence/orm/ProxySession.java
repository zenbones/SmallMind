package org.smallmind.persistence.orm;

public abstract class ProxySession {

   private String dataSource;
   private boolean enforceBoundary;

   public ProxySession () {

      this(null, false);
   }

   public ProxySession (String dataSource) {

      this(dataSource, false);
   }

   public ProxySession (String dataSource, boolean enforceBoundary) {

      this.dataSource = dataSource;
      this.enforceBoundary = enforceBoundary;

      SessionManager.registerSession(dataSource, this);
   }

   public String getDataSource () {

      return dataSource;
   }

   public boolean willEnforceBoundary () {

      return enforceBoundary;
   }

   public abstract ProxyTransaction beginTransaction ();

   public abstract ProxyTransaction currentTransaction ();

   public abstract void flush ();

   public abstract boolean isClosed ();

   public abstract void close ();
}
