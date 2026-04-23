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
package org.smallmind.file.jailed;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.smallmind.nutsnbolts.context.Context;

/**
 * Thread- or request-scoped context object that carries the jail root path used by
 * {@link ContextSensitiveRootedPathTranslator} to determine the boundary of the file
 * system jail at translation time.
 *
 * <p>Instances of this class are typically installed into a
 * {@link org.smallmind.nutsnbolts.context.ContextFactory} before performing any
 * jailed file-system operation, allowing the jail boundary to vary per caller or
 * per request without changing the underlying {@link JailedFileSystem} configuration.
 *
 * <p>The class is JAXB-annotated and can be unmarshalled from XML when the root
 * element is {@code <test>} in the {@code http://org.smallmind/file/jailed} namespace.
 *
 * @see ContextSensitiveRootedPathTranslator
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "test", namespace = "http://org.smallmind/file/jailed")
public class RootedFileSystemContext implements Context {

  /**
   * The native root directory string that defines the jail boundary for this context.
   */
  private String root;

  /**
   * Returns the native root directory path that defines the jail boundary for the
   * current context.
   *
   * <p>A {@code null} return value is treated by {@link ContextSensitiveRootedPathTranslator}
   * as an authorization failure, resulting in a {@link SecurityException}.
   *
   * @return the root directory path string, or {@code null} if not configured
   */
  @XmlElement(name = "root")
  public String getRoot () {

    return root;
  }

  /**
   * Sets the native root directory path that defines the jail boundary for the
   * current context.
   *
   * @param root the root directory path string to use as the jail boundary;
   *             may be {@code null} to indicate no authorization
   */
  public void setRoot (String root) {

    this.root = root;
  }
}
