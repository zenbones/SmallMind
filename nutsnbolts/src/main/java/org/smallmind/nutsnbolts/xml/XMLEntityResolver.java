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
