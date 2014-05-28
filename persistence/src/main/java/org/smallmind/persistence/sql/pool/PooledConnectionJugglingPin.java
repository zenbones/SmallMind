/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.sql.pool;

import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import org.smallmind.quorum.juggler.AbstractJugglingPin;
import org.smallmind.quorum.juggler.JugglerResourceException;

public class PooledConnectionJugglingPin extends AbstractJugglingPin<PooledConnection> {

  private ConnectionPoolDataSource dataSource;

  public PooledConnectionJugglingPin (ConnectionPoolDataSource dataSource) {

    this.dataSource = dataSource;
  }

  @Override
  public PooledConnection obtain ()
    throws JugglerResourceException {

    try {

      return dataSource.getPooledConnection();
    }
    catch (SQLException sqlException) {
      throw new JugglerResourceException(sqlException);
    }
  }

  @Override
  public boolean recover () {

    try {
      dataSource.getPooledConnection().close();

      return true;
    }
    catch (SQLException sqlException) {

      return false;
    }
  }

  @Override
  public String describe () {

    return dataSource.toString();
  }
}
