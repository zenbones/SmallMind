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
package org.smallmind.web.jetty;

import java.io.IOException;
import java.io.InputStream;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.nutsnbolts.resource.ResourceParser;
import org.smallmind.nutsnbolts.resource.ResourceTypeResourceGenerator;

/**
 * Holds the location and password of a keystore or truststore and reads its raw bytes for use with Jetty SSL configuration.
 */
public class SSLStore {

  private static final ResourceParser RESOURCE_PARSER = new ResourceParser(new ResourceTypeResourceGenerator());

  private String resource;
  private String password;

  /**
   * Resolves the configured resource location and reads its entire contents into a byte array.
   *
   * @return the raw bytes of the keystore or truststore
   * @throws IOException       if reading the resource input stream fails
   * @throws ResourceException if the resource location cannot be parsed or resolved
   */
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

  /**
   * Returns the resource identifier used to locate the keystore or truststore.
   *
   * @return the resource location string
   */
  public String getResource () {

    return resource;
  }

  /**
   * Sets the resource identifier pointing to the keystore or truststore to load.
   *
   * @param resource the resource location understood by {@link ResourceParser}
   */
  public void setResource (String resource) {

    this.resource = resource;
  }

  /**
   * Returns the password used to open the store.
   *
   * @return the store password
   */
  public String getPassword () {

    return password;
  }

  /**
   * Sets the password used to open the keystore or truststore.
   *
   * @param password the store password
   */
  public void setPassword (String password) {

    this.password = password;
  }
}
