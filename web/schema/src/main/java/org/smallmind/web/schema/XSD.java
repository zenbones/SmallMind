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
package org.smallmind.web.schema;

import java.util.List;

/**
 * Represents a W3C XML Schema (XSD) resource and its associated XML implementations to validate.
 */
public class XSD {

  private String path;
  private List<String> impls;

  /**
   * Returns the classpath-relative path to the XSD resource.
   *
   * @return the XSD resource path
   */
  public String getPath () {

    return path;
  }

  /**
   * Sets the classpath-relative path to the XSD resource.
   *
   * @param path the XSD resource path
   */
  public void setPath (String path) {

    this.path = path;
  }

  /**
   * Retrieves the list of XML resource paths that should be validated against this schema.
   *
   * @return the XML implementation paths
   */
  public List<String> getImpls () {

    return impls;
  }

  /**
   * Declares the XML resources that should be validated against this schema.
   *
   * @param impls the XML implementation paths
   */
  public void setImpls (List<String> impls) {

    this.impls = impls;
  }
}
