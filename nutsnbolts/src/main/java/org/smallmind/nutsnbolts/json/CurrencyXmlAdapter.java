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
package org.smallmind.nutsnbolts.json;

import java.util.Currency;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter that marshals {@link Currency} values to ISO currency codes and back.
 */
public class CurrencyXmlAdapter extends XmlAdapter<String, Currency> {

  /**
   * Converts an ISO currency code into a {@link Currency} instance.
   *
   * @param code the textual currency code, case-insensitive
   * @return the matching currency, or {@code null} when the code is {@code null}
   */
  @Override
  public Currency unmarshal (String code) {

    return (code == null) ? null : Currency.getInstance(code.toUpperCase());
  }

  /**
   * Marshals a {@link Currency} into its ISO currency code.
   *
   * @param currency the currency to marshal
   * @return the ISO code, or {@code null} when the currency is {@code null}
   */
  @Override
  public String marshal (Currency currency) {

    return (currency == null) ? null : currency.getCurrencyCode();
  }
}
