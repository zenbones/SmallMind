/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.resource;

import java.util.HashMap;

public class ResourceParser {

  private final HashMap<ResourceSchemes, ResourceFactory> factoryMap;

  public ResourceParser (ResourceFactory... factories) {

    factoryMap = new HashMap<ResourceSchemes, ResourceFactory>();

    if (factories != null) {
      for (ResourceFactory factory : factories) {
        addResourceFactory(factory);
      }
    }
  }

  public void addResourceFactory (ResourceFactory factory) {

    synchronized (factoryMap) {
      factoryMap.put(factory.getValidSchemes(), factory);
    }
  }

  public Resource parseResource (String resourceIdentifier)
    throws ResourceException {

    String scheme;
    int colonPos;

    scheme = ((colonPos = resourceIdentifier.indexOf(':')) < 0) ? ResourceType.FILE.getResourceScheme() : resourceIdentifier.substring(0, colonPos);

    synchronized (factoryMap) {
      for (ResourceSchemes resourceSchemes : factoryMap.keySet()) {
        if (resourceSchemes.containsScheme(scheme)) {
          return factoryMap.get(resourceSchemes).createResource(scheme, resourceIdentifier.substring(colonPos + 1));
        }
      }
    }

    throw new ResourceException("Could not locate a ResourceFactory for handling scheme(%s)", scheme);
  }
}
