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
package org.smallmind.file.jailed;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JailedURIUtilityTest {

  private JailedFileSystem jailedFileSystem;

  @BeforeClass
  public void beforeClass () {

    JailedFileSystemProvider provider = new JailedFileSystemProvider("jailed", new RootedPathTranslator(FileSystems.getDefault().getPath(System.getProperty("user.dir"))));

    jailedFileSystem = (JailedFileSystem)provider.getFileSystem(URI.create("jailed:///"));
  }

  public void testCheckUriValid () {

    JailedURIUtility.checkUri("jailed", URI.create("jailed:///"));
  }

  public void testCheckUriSchemeIsCaseInsensitive () {

    JailedURIUtility.checkUri("jailed", URI.create("JAILED:///"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWrongScheme () {

    JailedURIUtility.checkUri("jailed", URI.create("other:///"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithAuthority () {

    JailedURIUtility.checkUri("jailed", URI.create("jailed://host/"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWrongPath () {

    JailedURIUtility.checkUri("jailed", URI.create("jailed:///not-root"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithQuery () {

    JailedURIUtility.checkUri("jailed", URI.create("jailed:///?key=value"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCheckUriWithFragment () {

    JailedURIUtility.checkUri("jailed", URI.create("jailed:///#frag"));
  }

  public void testFromUriValid () {

    Path path = JailedURIUtility.fromUri(jailedFileSystem, URI.create("jailed:///a/b/c"));

    Assert.assertEquals(path.toString(), "/a/b/c");
    Assert.assertTrue(path.isAbsolute());
    Assert.assertSame(path.getFileSystem(), jailedFileSystem);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriRelative () {

    JailedURIUtility.fromUri(jailedFileSystem, URI.create("relative/path"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriOpaque () {

    JailedURIUtility.fromUri(jailedFileSystem, URI.create("jailed:opaque"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriWrongScheme () {

    JailedURIUtility.fromUri(jailedFileSystem, URI.create("other:///a/b"));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromUriWithFragment () {

    JailedURIUtility.fromUri(jailedFileSystem, URI.create("jailed:///a#frag"));
  }
}
