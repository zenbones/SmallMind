/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.web.jetty;

import java.io.IOException;
import java.io.InputStream;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeFactory;

public class SSLStore {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeFactory());

  private String resource;
  private String password;

  public byte[] getBytes ()
    throws IOException, ResourceException {

    Resource resourceImpl = RESOURCE_PARSER.parseResource(resource);
    byte[] resourceBuffer;

    try (InputStream resourceInputStream = resourceImpl.getInputStream()) {

      resourceBuffer = new byte[resourceInputStream.available()];
      int bytesRead = 0;

      while (bytesRead < resourceBuffer.length) {
        bytesRead += resourceInputStream.read(resourceBuffer, bytesRead, resourceBuffer.length - bytesRead);
      }
    }

    return resourceBuffer;
  }

  public String getResource () {

    return resource;
  }

  public void setResource (String resource) {

    this.resource = resource;
  }

  public String getPassword () {

    return password;
  }

  public void setPassword (String password) {

    this.password = password;
  }
}
