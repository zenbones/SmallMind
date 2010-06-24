package org.smallmind.persistence.orm.hibernate;

import org.hibernate.Query;

public abstract class QueryDetails {

   public abstract String getQueryString ();

   public abstract Query completeQuery (Query query);
}
