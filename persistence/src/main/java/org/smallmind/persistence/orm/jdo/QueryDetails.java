package org.smallmind.persistence.orm.jdo;

import java.util.Map;

public abstract class QueryDetails {

   public boolean getIgnoreCache () {

      return true;
   }

   public Class[] getImports () {

      return null;
   }

   public abstract Map getParameterMap ();

   public abstract String getQuery ();
}
