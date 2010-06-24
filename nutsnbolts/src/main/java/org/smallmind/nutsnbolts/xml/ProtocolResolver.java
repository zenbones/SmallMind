package org.smallmind.nutsnbolts.xml;

import org.smallmind.nutsnbolts.resource.Resource;

public interface ProtocolResolver {

   public abstract Resource resolve (String systemId)
      throws ProtocolResolutionException;
}
