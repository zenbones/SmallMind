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

import javax.naming.InvalidNameException;
import javax.naming.Name;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JavaNameTest {

  private final NameTranslator translator = new StubNameTranslator();

  private JavaName name (String... components)
    throws InvalidNameException {

    JavaName javaName = new JavaName(translator);

    for (String component : components) {
      javaName.add(component);
    }

    return javaName;
  }

  public void testComparisonOrdersByComponentCountBeforeContent ()
    throws InvalidNameException {

    // A shorter name always sorts first, even when its leading component would otherwise sort later.
    Assert.assertTrue(name("zebra").compareTo(name("alpha", "beta")) < 0);
    Assert.assertTrue(name("alpha", "beta").compareTo(name("zebra")) > 0);
  }

  public void testComparisonOfEqualSizedNamesIsComponentWise ()
    throws InvalidNameException {

    Assert.assertTrue(name("alpha", "beta").compareTo(name("alpha", "charlie")) < 0);
    Assert.assertTrue(name("alpha", "charlie").compareTo(name("alpha", "beta")) > 0);
    Assert.assertEquals(name("alpha", "beta").compareTo(name("alpha", "beta")), 0);
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void testComparisonAgainstNonJavaNameIsRejected ()
    throws InvalidNameException {

    name("alpha").compareTo(new Object());
  }

  public void testPrefixAndSuffixSlicesAreCorrect ()
    throws InvalidNameException {

    JavaName full = name("a", "b", "c", "d");

    Name prefix = full.getPrefix(2);
    Name suffix = full.getSuffix(2);

    Assert.assertEquals(prefix.size(), 2);
    Assert.assertEquals(prefix.get(0), "a");
    Assert.assertEquals(prefix.get(1), "b");

    Assert.assertEquals(suffix.size(), 2);
    Assert.assertEquals(suffix.get(0), "c");
    Assert.assertEquals(suffix.get(1), "d");

    Assert.assertTrue(full.getPrefix(0).isEmpty());
  }

  public void testSliceIsAnIndependentCopyOfTheBackingComponents ()
    throws InvalidNameException {

    // A slice copies the relevant components into its own backing list, so mutating the original after
    // the slice is taken leaves the slice untouched.
    JavaName full = name("a", "b", "c");
    Name prefix = full.getPrefix(2);

    full.remove(0);

    Assert.assertEquals(prefix.size(), 2);
    Assert.assertEquals(prefix.get(0), "a");
  }

  public void testStartsWithMatchesLeadingComponents ()
    throws InvalidNameException {

    JavaName full = name("a", "b", "c");

    Assert.assertTrue(full.startsWith(name("a", "b")));
    Assert.assertFalse(full.startsWith(name("b")));
    Assert.assertFalse(full.startsWith(name("a", "b", "c", "d")), "a name cannot start with something longer than itself");
  }

  public void testEndsWithMatchesTrailingComponents ()
    throws InvalidNameException {

    JavaName full = name("a", "b", "c");

    Assert.assertTrue(full.endsWith(name("b", "c")));
    Assert.assertTrue(full.endsWith(name("c")));
    Assert.assertFalse(full.endsWith(name("a")));
  }

  public void testAddInsertAndRemoveMutateInPlace ()
    throws InvalidNameException {

    JavaName javaName = name("a", "c");

    javaName.add(1, "b");
    Assert.assertEquals(javaName.size(), 3);
    Assert.assertEquals(javaName.get(1), "b");

    Object removed = javaName.remove(0);
    Assert.assertEquals(removed, "a");
    Assert.assertEquals(javaName.size(), 2);
    Assert.assertEquals(javaName.get(0), "b");
  }

  public void testAddAllAppendsSuffixComponentsInOrder ()
    throws InvalidNameException {

    JavaName base = name("a", "b");

    base.addAll(name("c", "d"));

    Assert.assertEquals(base.size(), 4);
    Assert.assertEquals(base.get(2), "c");
    Assert.assertEquals(base.get(3), "d");
  }

  public void testAddAllAtPositionInsertsComponentsInOrder ()
    throws InvalidNameException {

    JavaName base = name("a", "b");

    base.addAll(1, name("x", "y"));

    Assert.assertEquals(base.size(), 4);
    Assert.assertEquals(base.get(0), "a");
    Assert.assertEquals(base.get(1), "x");
    Assert.assertEquals(base.get(2), "y");
    Assert.assertEquals(base.get(3), "b");
  }

  public void testCloneProducesAnIndependentCopy ()
    throws InvalidNameException {

    JavaName original = name("a", "b");
    JavaName copy = (JavaName)original.clone();

    original.add("c");

    Assert.assertEquals(copy.size(), 2, "mutating the original must not affect a prior clone");
    Assert.assertEquals(copy.get(0), "a");
    Assert.assertEquals(copy.get(1), "b");
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
