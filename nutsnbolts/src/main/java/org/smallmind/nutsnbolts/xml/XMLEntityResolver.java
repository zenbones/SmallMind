/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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

public class XMLEntityResolver implements EntityResolver {

  private static XMLEntityResolver ENTITY_RESOLVER;

  private ProtocolResolver protocolResolver;

  public synchronized static XMLEntityResolver getInstance () {

    if (ENTITY_RESOLVER == null) {
      ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());
    }

    return ENTITY_RESOLVER;
  }

  public XMLEntityResolver (ProtocolResolver protocolResolver) {

    this.protocolResolver = protocolResolver;
  }

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
    }
    catch (Exception exception) {
      throw new SAXException(exception);
    }

    return null;
  }
}
