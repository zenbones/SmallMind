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
package org.smallmind.nutsnbolts.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Test(groups = "unit")
public class XMLEntityResolverTest {

  public void testResolverWrapsResolvedResourceInInputSource ()
    throws IOException, SAXException {

    XMLEntityResolver resolver = new XMLEntityResolver(new FixedResolver("known", "payload"));
    InputSource inputSource = resolver.resolveEntity(null, "known");

    Assert.assertNotNull(inputSource);

    byte[] buffer = inputSource.getByteStream().readAllBytes();
    Assert.assertEquals(new String(buffer, StandardCharsets.UTF_8), "payload");
  }

  public void testResolverReturnsNullWhenProtocolResolverReturnsNull ()
    throws IOException, SAXException {

    XMLEntityResolver resolver = new XMLEntityResolver(new FixedResolver("known", "payload"));

    Assert.assertNull(resolver.resolveEntity(null, "unknown"));
  }

  @Test(expectedExceptions = SAXException.class)
  public void testResolverWrapsProtocolFailureInSAXException ()
    throws IOException, SAXException {

    XMLEntityResolver resolver = new XMLEntityResolver(new ExplodingResolver());

    resolver.resolveEntity(null, "anything");
  }

  public void testSingletonIsLazilyInstantiated () {

    Assert.assertSame(XMLEntityResolver.getInstance(), XMLEntityResolver.getInstance());
  }

  private static class FixedResolver implements ProtocolResolver {

    private final String systemId;
    private final String body;

    FixedResolver (String systemId, String body) {

      this.systemId = systemId;
      this.body = body;
    }

    @Override
    public Resource resolve (String systemId) {

      if (this.systemId.equals(systemId)) {

        return new InMemoryResource("test", systemId, body);
      } else {

        return null;
      }
    }
  }

  private static class ExplodingResolver implements ProtocolResolver {

    @Override
    public Resource resolve (String systemId)
      throws ProtocolResolutionException {

      throw new ProtocolResolutionException("forced failure for(%s)", systemId);
    }
  }

  private static class InMemoryResource implements Resource {

    private final String scheme;
    private final String path;
    private final byte[] bytes;

    InMemoryResource (String scheme, String path, String body) {

      this.scheme = scheme;
      this.path = path;
      this.bytes = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getIdentifier () {

      return scheme + ":" + path;
    }

    @Override
    public String getScheme () {

      return scheme;
    }

    @Override
    public String getPath () {

      return path;
    }

    @Override
    public InputStream getInputStream ()
      throws ResourceException {

      return new ByteArrayInputStream(bytes);
    }
  }
}
