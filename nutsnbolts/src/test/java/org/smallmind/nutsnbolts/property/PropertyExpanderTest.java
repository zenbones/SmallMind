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
package org.smallmind.nutsnbolts.property;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.nutsnbolts.security.kms.Decryptor;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PropertyExpanderTest {

  public void testFallbackResolvesFromMapBeforeSystemProperty ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false);
    Map<String, Object> values = Map.of("service", "api", "env", "prod");

    Assert.assertEquals(expander.expand("https://${service}.${env}.example.com", values), "https://api.prod.example.com");
  }

  public void testFallbackFallsBackToSystemPropertyWhenMapMissesKey ()
    throws PropertyExpanderException {

    String key = "nutsnbolts.test.fallback." + System.nanoTime();

    System.setProperty(key, "sys-value");
    try {

      PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false);

      Assert.assertEquals(expander.expand("[${" + key + "}]", new HashMap<>()), "[sys-value]");
    } finally {
      System.clearProperty(key);
    }
  }

  public void testOverrideTakesSystemPropertyOverMap ()
    throws PropertyExpanderException {

    String key = "nutsnbolts.test.override." + System.nanoTime();

    System.setProperty(key, "winner");
    try {

      PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.OVERRIDE, false);
      Map<String, Object> values = Map.of(key, "loser");

      Assert.assertEquals(expander.expand("[${" + key + "}]", values), "[winner]");
    } finally {
      System.clearProperty(key);
    }
  }

  public void testNeverModeConsultsOnlyTheCallerMap ()
    throws PropertyExpanderException {

    String key = "nutsnbolts.test.never." + System.nanoTime();

    System.setProperty(key, "system-value");
    try {

      PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.NEVER, true);
      Map<String, Object> values = Map.of(key, "map-value");

      Assert.assertEquals(expander.expand("[${" + key + "}]", values), "[map-value]");
    } finally {
      System.clearProperty(key);
    }
  }

  public void testNestedPlaceholderResolvesInside ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.NEVER, false);
    Map<String, Object> values = Map.of("region", "east", "prefix.east", "useast1");

    Assert.assertEquals(expander.expand("server-${prefix.${region}}", values), "server-useast1");
  }

  @Test(expectedExceptions = PropertyExpanderException.class)
  public void testStrictModeRejectsUnresolvedPlaceholder ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.NEVER, false);

    expander.expand("${absent.key}", new HashMap<>());
  }

  public void testIgnoreUnresolvableLeavesKeyInPlace ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), true, SystemPropertyMode.NEVER, false);

    Assert.assertEquals(expander.expand("[${absent.key}]", new HashMap<>()), "[${absent.key}]");
  }

  @Test(expectedExceptions = PropertyExpanderException.class)
  public void testCircularReferenceIsDetected ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.NEVER, false);
    Map<String, Object> values = Map.of("a", "${b}", "b", "${a}");

    expander.expand("${a}", values);
  }

  @Test(expectedExceptions = PropertyExpanderException.class)
  public void testUnclosedPrefixIsRejected ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.NEVER, false);

    expander.expand("${unclosed", new HashMap<>());
  }

  public void testCustomDelimitersAreHonoured ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander(new PropertyClosure("<<", ">>"), false, SystemPropertyMode.NEVER, false);
    Map<String, Object> values = Map.of("env", "stage");

    Assert.assertEquals(expander.expand("<<env>>-deploy", values), "stage-deploy");
  }

  @Test(expectedExceptions = PropertyExpanderException.class)
  public void testBlankDelimiterIsRejected ()
    throws PropertyExpanderException {

    new PropertyClosure("", "}");
  }

  @Test(expectedExceptions = PropertyExpanderException.class)
  public void testSharedCharacterBetweenPrefixAndSuffixIsRejected ()
    throws PropertyExpanderException {

    new PropertyClosure("{x", "x}");
  }

  public void testEncryptedPlaceholderInvokesDecryptor ()
    throws PropertyExpanderException {

    Decryptor decryptor = encrypted -> "decrypted-" + encrypted;
    PropertyExpander expander = new PropertyExpander(new PropertyClosure(decryptor), false, SystemPropertyMode.NEVER, false);
    Map<String, Object> values = Map.of("secret", "cipher");

    Assert.assertEquals(expander.expand("[!{secret}]", values), "[decrypted-cipher]");
  }

  public void testNoArgConstructorUsesDefaults ()
    throws PropertyExpanderException {

    PropertyExpander expander = new PropertyExpander();

    Assert.assertEquals(expander.expand("plain-text"), "plain-text");
  }
}
