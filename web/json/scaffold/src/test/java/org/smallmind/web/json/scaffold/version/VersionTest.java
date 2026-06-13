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
package org.smallmind.web.json.scaffold.version;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link Version} default {@code toJson}/{@code fromJson} helpers, which round-trip a
 * {@link Versioned} bean through the shared JSON codec using the version-declared versioned class.
 */
@Test(groups = "unit")
public class VersionTest {

  public void testGetVersionedClass () {

    Assert.assertEquals(SampleVersion.V1.getVersionedClass(), SampleVersioned.class);
  }

  public void testToJsonAndFromJsonRoundTrip ()
    throws Exception {

    SampleVersioned sample = new SampleVersioned();
    sample.setValue("hello");

    String json = SampleVersion.V1.toJson(sample);

    Assert.assertTrue(json.contains("hello"), json);

    Versioned<SampleVersion> recovered = SampleVersion.V1.fromJson(json);

    Assert.assertTrue(recovered instanceof SampleVersioned, recovered.getClass().getName());
    Assert.assertEquals(((SampleVersioned)recovered).getValue(), "hello");
    Assert.assertEquals(recovered.getVersion(), SampleVersion.V1);
  }

  public enum SampleVersion implements Version<SampleVersion> {

    V1;

    @Override
    public Class<? extends Versioned<SampleVersion>> getVersionedClass () {

      return SampleVersioned.class;
    }
  }

  public static class SampleVersioned implements Versioned<SampleVersion> {

    private String value;

    public String getValue () {

      return value;
    }

    public void setValue (String value) {

      this.value = value;
    }

    @Override
    public SampleVersion getVersion () {

      return SampleVersion.V1;
    }
  }
}
