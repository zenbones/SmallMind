package org.smallmind.nutsnbolts.xml;

import org.smallmind.nutsnbolts.resource.ClasspathResource;
import org.smallmind.nutsnbolts.resource.Resource;

public class SmallMindProtocolResolver implements ProtocolResolver {

   private static SmallMindProtocolResolver PROTOCOL_RESOLVER;
   private static final String INTERNAL_PROTOCOL = "smallmind://";

   public synchronized static SmallMindProtocolResolver getInstance () {

      if (PROTOCOL_RESOLVER == null) {
         PROTOCOL_RESOLVER = new SmallMindProtocolResolver();
      }
      return PROTOCOL_RESOLVER;
   }

   public Resource resolve (String systemId)
      throws ProtocolResolutionException {

      if (systemId.startsWith(INTERNAL_PROTOCOL)) {
         return new ClasspathResource(systemId.substring(INTERNAL_PROTOCOL.length()));
      }

      return null;
   }
}

