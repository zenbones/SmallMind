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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Currency;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * Hibernate {@link UserType} that stores {@link Currency} values as their three-letter ISO codes
 * in a {@code CHAR} column and reconstitutes them via {@link Currency#getInstance(String)}.
 */
public class CurrencyUserType implements UserType<Currency> {

  /**
   * {@inheritDoc}
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
   * @param value the value to copy
   * @return the same immutable instance
   */
  @Override
  public Currency deepCopy (Currency value) {

    return value;
  }

  /**
   * {@inheritDoc}
   * Reads a three-letter ISO currency code from the configured column position and
   * resolves it to a {@link Currency} instance.
   *
   * @param rs       the result set to read from
   * @param position the 1-based column position
   * @param options  Hibernate wrapper options
   * @return the resolved {@link Currency}, or {@code null} if the column is SQL {@code NULL}
   * @throws SQLException if JDBC access fails
   */
  @Override
  public Currency nullSafeGet (ResultSet rs, int position, WrapperOptions options)
    throws SQLException {

    String code = rs.getString(position);

    return rs.wasNull() ? null : Currency.getInstance(code);
  }

  /**
   * {@inheritDoc}
   * Writes the {@link Currency#getCurrencyCode() ISO currency code} for the value at the configured
   * column position, or writes SQL {@code NULL} when the value is {@code null}.
   *
   * @param st       the prepared statement to write to
   * @param value    the currency value to persist
   * @param position the 1-based parameter position
   * @param options  Hibernate wrapper options
   * @throws SQLException if JDBC access fails
   */
  @Override
  public void nullSafeSet (PreparedStatement st, Currency value, int position, WrapperOptions options)
    throws SQLException {

    if (value == null) {
      st.setNull(position, Types.CHAR);
    } else {
      st.setString(position, value.getCurrencyCode());
    }
  }
}
