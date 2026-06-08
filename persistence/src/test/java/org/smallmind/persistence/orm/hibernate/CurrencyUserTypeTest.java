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
import java.sql.Types;
import java.util.Currency;
import org.hibernate.type.descriptor.WrapperOptions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link CurrencyUserType}, the Hibernate {@link org.hibernate.usertype.UserType} that stores a
 * {@link Currency} as its three-letter ISO code in a {@code CHAR} column. The JDBC {@link ResultSet} and
 * {@link PreparedStatement} are Mockito mocks, so no database is involved. The tests cover the money-correctness
 * contract: reading a currency code back into the right {@link Currency}, writing the right code (or SQL
 * {@code NULL}) for a value, round-trip stability, the SQL-{@code NULL} read branch, and the metadata and
 * copy/equality methods the type exposes.
 */
@Test(groups = "unit")
public class CurrencyUserTypeTest {

  private CurrencyUserType currencyUserType;
  private ResultSet resultSet;
  private PreparedStatement preparedStatement;
  private WrapperOptions wrapperOptions;

  @BeforeMethod
  public void setUp () {

    currencyUserType = new CurrencyUserType();
    resultSet = Mockito.mock(ResultSet.class);
    preparedStatement = Mockito.mock(PreparedStatement.class);
    wrapperOptions = Mockito.mock(WrapperOptions.class);
  }

  @Test(groups = "unit")
  public void testReturnedClassIsCurrency () {

    Assert.assertEquals(currencyUserType.returnedClass(), Currency.class, "the type must report Currency as its returned class");
  }

  @Test(groups = "unit")
  public void testGetSqlTypeIsChar () {

    Assert.assertEquals(currencyUserType.getSqlType(), Types.CHAR, "currency codes are stored as fixed-width CHAR");
  }

  @Test(groups = "unit")
  public void testIsNotMutable () {

    Assert.assertFalse(currencyUserType.isMutable(), "currencies are immutable, so the type must not be mutable");
  }

  @Test(groups = "unit")
  public void testDeepCopyReturnsTheSameInstance () {

    Currency usd = Currency.getInstance("USD");

    Assert.assertSame(currencyUserType.deepCopy(usd), usd, "an immutable currency may be deep-copied by reference");
  }

  @Test(groups = "unit")
  public void testDeepCopyOfNullIsNull () {

    Assert.assertNull(currencyUserType.deepCopy(null), "a null value deep-copies to null");
  }

  @Test(groups = "unit")
  public void testNullSafeGetResolvesUsd ()
    throws Exception {

    Mockito.when(resultSet.getString(1)).thenReturn("USD");
    Mockito.when(resultSet.wasNull()).thenReturn(false);

    Assert.assertEquals(currencyUserType.nullSafeGet(resultSet, 1, wrapperOptions), Currency.getInstance("USD"), "the USD code must resolve to the USD currency");
  }

  @Test(groups = "unit")
  public void testNullSafeGetResolvesEuro ()
    throws Exception {

    Mockito.when(resultSet.getString(2)).thenReturn("EUR");
    Mockito.when(resultSet.wasNull()).thenReturn(false);

    Assert.assertEquals(currencyUserType.nullSafeGet(resultSet, 2, wrapperOptions), Currency.getInstance("EUR"), "the EUR code must resolve to the euro currency");
  }

  @Test(groups = "unit")
  public void testNullSafeGetReadsTheGivenColumnPosition ()
    throws Exception {

    Mockito.when(resultSet.getString(7)).thenReturn("GBP");
    Mockito.when(resultSet.wasNull()).thenReturn(false);

    Assert.assertEquals(currencyUserType.nullSafeGet(resultSet, 7, wrapperOptions), Currency.getInstance("GBP"), "the type must read the column at the supplied position");
    Mockito.verify(resultSet).getString(7);
  }

  @Test(groups = "unit")
  public void testNullSafeGetReturnsNullWhenColumnIsSqlNull ()
    throws Exception {

    Mockito.when(resultSet.getString(1)).thenReturn(null);
    Mockito.when(resultSet.wasNull()).thenReturn(true);

    Assert.assertNull(currencyUserType.nullSafeGet(resultSet, 1, wrapperOptions), "a SQL NULL column must resolve to a null currency");
  }

  @Test(groups = "unit")
  public void testNullSafeSetWritesTheCurrencyCode ()
    throws Exception {

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

    currencyUserType.nullSafeSet(preparedStatement, Currency.getInstance("USD"), 1, wrapperOptions);

    Mockito.verify(preparedStatement).setString(Mockito.eq(1), codeCaptor.capture());
    Assert.assertEquals(codeCaptor.getValue(), "USD", "the USD currency must be written as its ISO code");
  }

  @Test(groups = "unit")
  public void testNullSafeSetWritesTheGivenPosition ()
    throws Exception {

    currencyUserType.nullSafeSet(preparedStatement, Currency.getInstance("EUR"), 4, wrapperOptions);

    Mockito.verify(preparedStatement).setString(4, "EUR");
    Mockito.verify(preparedStatement, Mockito.never()).setNull(Mockito.anyInt(), Mockito.anyInt());
  }

  @Test(groups = "unit")
  public void testNullSafeSetWritesSqlNullForNullValue ()
    throws Exception {

    currencyUserType.nullSafeSet(preparedStatement, null, 1, wrapperOptions);

    Mockito.verify(preparedStatement).setNull(1, Types.CHAR);
    Mockito.verify(preparedStatement, Mockito.never()).setString(Mockito.anyInt(), Mockito.anyString());
  }

  @Test(groups = "unit")
  public void testRoundTripPreservesTheCurrency ()
    throws Exception {

    ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
    Currency written = Currency.getInstance("JPY");

    currencyUserType.nullSafeSet(preparedStatement, written, 1, wrapperOptions);
    Mockito.verify(preparedStatement).setString(Mockito.eq(1), codeCaptor.capture());

    Mockito.when(resultSet.getString(1)).thenReturn(codeCaptor.getValue());
    Mockito.when(resultSet.wasNull()).thenReturn(false);

    Assert.assertEquals(currencyUserType.nullSafeGet(resultSet, 1, wrapperOptions), written, "a value written and then read back must yield the same currency");
  }

  @Test(groups = "unit")
  public void testEqualsIsTrueForEqualCurrencies () {

    Assert.assertTrue(currencyUserType.equals(Currency.getInstance("USD"), Currency.getInstance("USD")), "the same currency must compare equal");
  }

  @Test(groups = "unit")
  public void testEqualsIsFalseForDifferentCurrencies () {

    Assert.assertFalse(currencyUserType.equals(Currency.getInstance("USD"), Currency.getInstance("EUR")), "different currencies must not compare equal");
  }

  @Test(groups = "unit")
  public void testEqualsIsTrueForTwoNulls () {

    Assert.assertTrue(currencyUserType.equals(null, null), "two null currencies must compare equal");
  }

  @Test(groups = "unit")
  public void testEqualsIsFalseForNullAndValue () {

    Assert.assertFalse(currencyUserType.equals(null, Currency.getInstance("USD")), "a null and a non-null currency must not compare equal");
  }

  @Test(groups = "unit")
  public void testHashCodeMatchesForEqualCurrencies () {

    Currency usd = Currency.getInstance("USD");

    Assert.assertEquals(currencyUserType.hashCode(usd), currencyUserType.hashCode(Currency.getInstance("USD")), "equal currencies must hash alike");
  }

  @Test(groups = "unit")
  public void testDisassembleAndAssembleRoundTrip () {

    Currency usd = Currency.getInstance("USD");

    Assert.assertEquals(currencyUserType.assemble(currencyUserType.disassemble(usd), null), usd, "a disassembled currency must reassemble to an equal value");
  }
}
