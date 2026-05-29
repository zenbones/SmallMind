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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class XMLURIResolverTest {

  public void testResolverWrapsResolvedResourceInStreamSource ()
    throws Exception {

    XMLURIResolver resolver = new XMLURIResolver(new FixedResolver("known", "<xsl/>"));
    Source source = resolver.resolve("known", null);

    Assert.assertTrue(source instanceof StreamSource);

    byte[] bytes = ((StreamSource)source).getInputStream().readAllBytes();
    Assert.assertEquals(new String(bytes, StandardCharsets.UTF_8), "<xsl/>");
  }

  public void testResolverReturnsNullWhenProtocolResolverReturnsNull ()
    throws TransformerException {

    XMLURIResolver resolver = new XMLURIResolver(new FixedResolver("known", "x"));

    Assert.assertNull(resolver.resolve("unknown", null));
  }

  @Test(expectedExceptions = TransformerException.class)
  public void testResolverWrapsProtocolFailureInTransformerException ()
    throws TransformerException {

    XMLURIResolver resolver = new XMLURIResolver(new ExplodingResolver());

    resolver.resolve("anything", null);
  }

  public void testSingletonIsLazilyInstantiated () {

    Assert.assertSame(XMLURIResolver.getInstance(), XMLURIResolver.getInstance());
  }

  private static class FixedResolver implements ProtocolResolver {

    private final String href;
    private final String body;

    FixedResolver (String href, String body) {

      this.href = href;
      this.body = body;
    }

    @Override
    public Resource resolve (String systemId) {

      if (href.equals(systemId)) {

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
