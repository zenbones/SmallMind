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
package org.smallmind.persistence.cache;

import org.smallmind.persistence.AbstractDurable;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class VectorKeyTest {

  private static VectorArtifact artifact (VectorIndex... indices) {

    return new VectorArtifact("ns", indices);
  }

  public void testFieldNameUsedWhenAliasIsEmpty () {

    VectorKey<Sample> key = new VectorKey<>(artifact(new VectorIndex("status", "ACTIVE", "")), Sample.class);

    Assert.assertEquals(key.getKey(), "Sample:ns[status=ACTIVE]");
  }

  public void testAliasReplacesFieldNameWhenPresent () {

    VectorKey<Sample> key = new VectorKey<>(artifact(new VectorIndex("status", "ACTIVE", "st")), Sample.class);

    Assert.assertEquals(key.getKey(), "Sample:ns[st=ACTIVE]");
  }

  public void testNullIndexValueRendersAsNullLiteral () {

    VectorKey<Sample> key = new VectorKey<>(artifact(new VectorIndex("status", null, "")), Sample.class);

    Assert.assertEquals(key.getKey(), "Sample:ns[status=null]");
  }

  public void testMultipleIndicesAreCommaSeparated () {

    VectorKey<Sample> key = new VectorKey<>(artifact(new VectorIndex("a", 1, ""), new VectorIndex("b", 2, "")), Sample.class);

    Assert.assertEquals(key.getKey(), "Sample:ns[a=1,b=2]");
  }

  public void testNoIndicesProduceEmptyBrackets () {

    VectorKey<Sample> key = new VectorKey<>(artifact(), Sample.class);

    Assert.assertEquals(key.getKey(), "Sample:ns[]");
  }

  public void testClassificationIsAppendedWhenSupplied () {

    VectorKey<Sample> classified = new VectorKey<>(artifact(new VectorIndex("a", 1, "")), Sample.class, "#alt");
    VectorKey<Sample> plain = new VectorKey<>(artifact(new VectorIndex("a", 1, "")), Sample.class);

    Assert.assertEquals(classified.getKey(), "Sample:ns[a=1]#alt");
    Assert.assertEquals(plain.getKey(), "Sample:ns[a=1]");
  }

  public void testEqualityAndHashCodeFollowKeyString () {

    VectorKey<Sample> key = new VectorKey<>(artifact(new VectorIndex("a", 1, "")), Sample.class);

    Assert.assertEquals(key, new VectorKey<>(artifact(new VectorIndex("a", 1, "")), Sample.class));
    Assert.assertEquals(key.hashCode(), new VectorKey<>(artifact(new VectorIndex("a", 1, "")), Sample.class).hashCode());
    Assert.assertNotEquals(key, new VectorKey<>(artifact(new VectorIndex("a", 2, "")), Sample.class));
  }

  public void testGetElementClassReturnsConstructorArgument () {

    Assert.assertEquals(new VectorKey<>(artifact(), Sample.class).getElementClass(), Sample.class);
  }

  public static class Sample extends AbstractDurable<Long, Sample> {

    private Long id;

    @Override
    public Long getId () {

      return id;
    }

    @Override
    public void setId (Long id) {

      this.id = id;
    }
  }
}
