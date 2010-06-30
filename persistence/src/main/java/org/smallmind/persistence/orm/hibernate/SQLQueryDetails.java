package org.smallmind.persistence.orm.hibernate;

import org.hibernate.SQLQuery;

public abstract class SQLQueryDetails {

   public abstract String getSQLQueryString ();

   public abstract SQLQuery completeSQLQuery (SQLQuery sqlQuery);
}