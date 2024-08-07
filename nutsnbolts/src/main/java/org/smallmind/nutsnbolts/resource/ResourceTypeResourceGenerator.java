/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.resource;

import java.lang.reflect.Constructor;
import java.util.LinkedList;

public class ResourceTypeResourceGenerator implements ResourceGenerator {

  private static final Class[] SIGNATURE = new Class[] {String.class};

  private static final ResourceSchemes VALID_SCHEMES;

  static {

    String[] schemes;
    LinkedList<String> schemeList;

    schemeList = new LinkedList<>();
    for (ResourceType resourceType : ResourceType.values()) {
      schemeList.add(resourceType.getResourceScheme());
    }

    schemes = new String[schemeList.size()];
    schemeList.toArray(schemes);
    VALID_SCHEMES = new ResourceSchemes(schemes);
  }

  public ResourceSchemes getValidSchemes () {

    return VALID_SCHEMES;
  }

  public Resource createResource (String scheme, String path)
    throws ResourceException {

    Constructor<? extends Resource> resourceConstructor;

    for (ResourceType resourceType : ResourceType.values()) {
      if (resourceType.getResourceScheme().equals(scheme)) {
        try {
          resourceConstructor = resourceType.getResourceClass().getConstructor(SIGNATURE);
          return resourceConstructor.newInstance(path);
        } catch (Exception exception) {
          throw new ResourceException(exception);
        }
      }
    }

    throw new ResourceException("This factory does not handle the references scheme(%s)", scheme);
  }
}
