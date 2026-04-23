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

import java.io.IOException;
import java.io.InputStream;
import org.smallmind.nutsnbolts.resource.Resource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * SAX {@link EntityResolver} that delegates entity resolution to a {@link ProtocolResolver} and returns the result as an {@link InputSource}.
 * A lazily created singleton backed by {@link SmallMindProtocolResolver} is available for convenient reuse.
 */
public class XMLEntityResolver implements EntityResolver {

  private static XMLEntityResolver ENTITY_RESOLVER;

  private final ProtocolResolver protocolResolver;

  /**
   * Creates an entity resolver backed by the supplied protocol resolver.
   *
   * @param protocolResolver the resolver used to locate resources by system identifier
   */
  public XMLEntityResolver (ProtocolResolver protocolResolver) {

    this.protocolResolver = protocolResolver;
  }

  /**
   * Returns the shared singleton instance configured with {@link SmallMindProtocolResolver}, creating it on first call.
   *
   * @return the singleton {@link XMLEntityResolver}
   */
  public synchronized static XMLEntityResolver getInstance () {

    if (ENTITY_RESOLVER == null) {
      ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());
    }

    return ENTITY_RESOLVER;
  }

  /**
   * Resolves an external entity by delegating to the configured {@link ProtocolResolver} and wrapping the result in an {@link InputSource}.
   *
   * @param publicId the public identifier of the external entity (unused)
   * @param systemId the system identifier of the external entity to resolve
   * @return an {@link InputSource} backed by the resolved resource, or {@code null} to fall back to default parser resolution
   * @throws SAXException if resolution fails
   * @throws IOException  if the resolved resource stream cannot be opened
   */
  public InputSource resolveEntity (String publicId, String systemId)
    throws SAXException, IOException {

    Resource entityResource;
    InputStream entityStream;

    try {
      if ((entityResource = protocolResolver.resolve(systemId)) != null) {
        if ((entityStream = entityResource.getInputStream()) != null) {
          return new InputSource(entityStream);
        }
      }
    } catch (Exception exception) {
      throw new SAXException(exception);
    }

    return null;
  }
}
