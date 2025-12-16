/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.orm.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Currency;
import java.util.Objects;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * Hibernate {@link UserType} that stores {@link Currency} values as their three-letter ISO codes
 * in a {@code CHAR} column and reconstitutes them via {@link Currency#getInstance(String)}.
 */
public class CurrencyUserType implements UserType<Currency> {

  /**
   * {@inheritDoc}
   * <p>
   * Currencies are immutable so the value never changes.
   */
  @Override
  public boolean isMutable () {

    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@link Currency}.class
   */
  @Override
  public Class<Currency> returnedClass () {

    return Currency.class;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@link Types#CHAR}
   */
  @Override
  public int getSqlType () {

    return Types.CHAR;
  }

  /**
   * {@inheritDoc}
   *
   * @param currency the currency to hash
   * @return the hash code or zero for null
   */
  @Override
  public int hashCode (Currency currency) {

    return Objects.hashCode(currency);
  }

  /**
   * {@inheritDoc}
   *
   * @param x first currency
   * @param y second currency
   * @return true when both are equal or both null
   */
  @Override
  public boolean equals (Currency x, Currency y) {

    return Objects.equals(x, y);
  }

  /**
   * {@inheritDoc}
   *
   * @param value the value to copy
   * @return the same immutable instance
   */
  @Override
  public Currency deepCopy (Currency value) {

    return value;
  }

  /**
   * {@inheritDoc}
   *
   * @param cached the cached value
   * @param owner  entity that owns the value
   * @return the cached currency instance
   */
  @Override
  public Currency assemble (Serializable cached, Object owner) {

    return (Currency)cached;
  }

  /**
   * {@inheritDoc}
   *
   * @param value the currency to cache
   * @return the same instance because currencies are immutable
   */
  @Override
  public Serializable disassemble (Currency value) {

    return value;
  }

  /**
   * {@inheritDoc}
   *
   * @param detached detached state
   * @param managed  managed state (ignored)
   * @param owner    owning entity (ignored)
   * @return the detached value as the authoritative instance
   */
  @Override
  public Currency replace (Currency detached, Currency managed, Object owner) {

    return detached;
  }

  /**
   * Reads the currency code from the result set and converts it to a {@link Currency}.
   *
   * @param rs       result set positioned at the current row
   * @param position column index to read
   * @param session  current Hibernate session
   * @param owner    owning entity (unused)
   * @return {@code Currency} instance, or {@code null} if the column is SQL NULL
   * @throws SQLException if the JDBC driver reports a read error
   */
  @Override
  public Currency nullSafeGet (ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
    throws SQLException {

    String code = rs.getString(position);

    return rs.wasNull() ? null : Currency.getInstance(code);
  }

  /**
   * Writes the currency code to the prepared statement, or sets {@code NULL} if absent.
   *
   * @param st      prepared statement being populated
   * @param value   currency to write, or {@code null}
   * @param index   parameter index
   * @param session current Hibernate session
   * @throws SQLException if the JDBC driver reports a write error
   */
  @Override
  public void nullSafeSet (PreparedStatement st, Currency value, int index, SharedSessionContractImplementor session)
    throws SQLException {

    if (value == null) {
      st.setNull(index, Types.CHAR);
    } else {
      st.setString(index, value.getCurrencyCode());
    }
  }
}
