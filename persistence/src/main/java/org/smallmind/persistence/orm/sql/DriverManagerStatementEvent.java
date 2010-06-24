package org.smallmind.persistence.orm.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.PooledConnection;
import javax.sql.StatementEvent;

public class DriverManagerStatementEvent extends StatementEvent {

   private String statementId;

   public DriverManagerStatementEvent (PooledConnection connection, PreparedStatement statement, String statementId) {

      super(connection, statement);

      this.statementId = statementId;
   }

   public DriverManagerStatementEvent (PooledConnection connection, PreparedStatement statement, SQLException exception, String statementId) {

      super(connection, statement, exception);

      this.statementId = statementId;
   }

   public String getStatementId () {

      return statementId;
   }
}
