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
package org.smallmind.quorum.namespace;

import javax.naming.Name;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JavaNameParserTest {

  private final JavaNameParser parser = new JavaNameParser(new StubNameTranslator());

  public void testEmptyStringParsesToEmptyName ()
    throws NamingException {

    Assert.assertEquals(parser.parse("").size(), 0);
  }

  public void testSchemeSegmentIsSplitIntoSchemeAndRemainder ()
    throws NamingException {

    Name parsed = parser.parse("java:comp");

    Assert.assertEquals(parsed.size(), 2);
    Assert.assertEquals(parsed.get(0), "java:");
    Assert.assertEquals(parsed.get(1), "comp");
  }

  public void testSchemeOnlySegmentYieldsASingleComponent ()
    throws NamingException {

    Name parsed = parser.parse("java:");

    Assert.assertEquals(parsed.size(), 1);
    Assert.assertEquals(parsed.get(0), "java:");
  }

  public void testMultiSegmentPathIsParsedInOrder ()
    throws NamingException {

    Name parsed = parser.parse("java:comp/env/jdbc");

    Assert.assertEquals(parsed.size(), 4);
    Assert.assertEquals(parsed.get(0), "java:");
    Assert.assertEquals(parsed.get(1), "comp");
    Assert.assertEquals(parsed.get(2), "env");
    Assert.assertEquals(parsed.get(3), "jdbc");
  }

  public void testFirstSegmentWithoutColonIsKeptWhole ()
    throws NamingException {

    Name parsed = parser.parse("foo/bar");

    Assert.assertEquals(parsed.size(), 2);
    Assert.assertEquals(parsed.get(0), "foo");
    Assert.assertEquals(parsed.get(1), "bar");
  }

  public void testUnparseJoinsComponentsWithSlashes ()
    throws NamingException {

    Assert.assertEquals(parser.unparse(parser.parse("foo/bar/baz")), "foo/bar/baz");
    Assert.assertEquals(parser.unparse(parser.parse("")), "");
  }

  public void testUnparseReinsertsASeparatorAfterTheScheme ()
    throws NamingException {

    // The scheme is parsed into its own component, so re-joining inserts a slash that was not present
    // in the original string. Round-tripping a schemed name is therefore intentionally asymmetric.
    Assert.assertEquals(parser.unparse(parser.parse("java:comp")), "java:/comp");
  }

  private static class StubNameTranslator extends NameTranslator {

    private StubNameTranslator () {

      super(null);
    }

    @Override
    public JavaName fromInternalNameToExternalName (Name internalName) {

      throw new UnsupportedOperationException();
    }

    @Override
    public String fromExternalNameToExternalString (JavaName internalName) {

      throw new UnsupportedOperationException();
    }

    @Override
    public String fromAbsoluteExternalStringToInternalString (String externalName) {

      throw new UnsupportedOperationException();
    }

    @Override
    public String fromExternalStringToInternalString (String externalName) {

      throw new UnsupportedOperationException();
    }
  }
}
