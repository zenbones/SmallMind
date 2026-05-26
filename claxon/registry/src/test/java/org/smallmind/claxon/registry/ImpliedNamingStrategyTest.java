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
package org.smallmind.claxon.registry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ImpliedNamingStrategyTest {

  public void testUnsetPrefixMapReturnsNull () {

    Assert.assertNull(new ImpliedNamingStrategy().from(String.class));
  }

  public void testEmptyPrefixMapReturnsNull () {

    Assert.assertNull(new ImpliedNamingStrategy().setPrefixMap(new HashMap<>()).from(String.class));
  }

  public void testNonMatchingPatternReturnsNull ()
    throws DotNotationException {

    Map<DotNotation, String> prefixMap = new HashMap<>();

    prefixMap.put(new DotNotation("com.example.*"), "app");

    Assert.assertNull(new ImpliedNamingStrategy().setPrefixMap(prefixMap).from(String.class));
  }

  public void testMatchingPatternReturnsPrefix ()
    throws DotNotationException {

    Map<DotNotation, String> prefixMap = new HashMap<>();

    prefixMap.put(new DotNotation("java.lang.*"), "jdk.lang");

    Assert.assertEquals(new ImpliedNamingStrategy().setPrefixMap(prefixMap).from(String.class), "jdk.lang");
  }

  public void testMoreSpecificPatternWinsOverWildcard ()
    throws DotNotationException {

    Map<DotNotation, String> prefixMap = new LinkedHashMap<>();

    prefixMap.put(new DotNotation("*"), "other");
    prefixMap.put(new DotNotation("java.lang.*"), "jdk.lang");

    Assert.assertEquals(new ImpliedNamingStrategy().setPrefixMap(prefixMap).from(String.class), "jdk.lang");
  }

  public void testSetPrefixMapReturnsThis () {

    ImpliedNamingStrategy strategy = new ImpliedNamingStrategy();

    Assert.assertSame(strategy.setPrefixMap(new HashMap<>()), strategy);
  }
}
