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
package org.smallmind.file.ephemeral;

import java.net.URI;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EphemeralURIUtilityTest {

  private EphemeralFileSystem ephemeralFileSystem;

  @BeforeClass
  public void beforeClass () {

    EphemeralFileSystemProvider provider = new EphemeralFileSystemProvider("ephemeral");

    ephemeralFileSystem = (EphemeralFileSystem)provider.getFileSystem(URI.create("ephemeral:///"));
  }

  public void testCheckUriValid () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("ephemeral:///"));
  }

  public void testCheckUriSchemeIsCaseInsensitive () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("EPHEMERAL:///"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWrongScheme () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("other:///"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithAuthority () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("ephemeral://host/"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWrongPath () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("ephemeral:///not-root"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithQuery () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("ephemeral:///?key=value"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithFragment () {

    EphemeralURIUtility.checkUri("ephemeral", URI.create("ephemeral:///#frag"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriRelative () {

    EphemeralURIUtility.fromUri(ephemeralFileSystem, URI.create("relative/path"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriOpaque () {

    EphemeralURIUtility.fromUri(ephemeralFileSystem, URI.create("ephemeral:opaque"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriWrongScheme () {

    EphemeralURIUtility.fromUri(ephemeralFileSystem, URI.create("other:///a/b"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriWithFragment () {

    EphemeralURIUtility.fromUri(ephemeralFileSystem, URI.create("ephemeral:///a#frag"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriWithQuery () {

    EphemeralURIUtility.fromUri(ephemeralFileSystem, URI.create("ephemeral:///a?key=value"));
  }
}
