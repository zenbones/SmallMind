package org.smallmind.persistence.orm.hibernate;

import org.hibernate.dialect.InnoDBStorageEngine;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.MySQLStorageEngine;

public class MySQL8InnoDBDialect extends MySQL8Dialect {

  public MySQL8InnoDBDialect () {

  }

  @Override
  protected MySQLStorageEngine getDefaultMySQLStorageEngine () {

    return InnoDBStorageEngine.INSTANCE;
  }
}



