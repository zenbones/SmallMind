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
package org.smallmind.web.json.scaffold.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.fasterxml.jackson.module.jakarta.xmlbind.PackageVersion;

/**
 * Jackson module that registers the Jakarta XML Bind annotation introspector with configurable
 * priority and inclusion handling for non-nillable properties.
 */
public class JakartaXmlBindAnnotationModule extends Module {

  public enum Priority {PRIMARY, SECONDARY}

  protected Priority _priority = Priority.PRIMARY;
  protected JsonInclude.Include _nonNillableInclusion = JsonInclude.Include.ALWAYS;

  /**
   * Default constructor.
   */
  public JakartaXmlBindAnnotationModule () {

  }

  /**
   * @return module name for registration
   */
  @Override
  public String getModuleName () {

    return getClass().getSimpleName();
  }

  /**
   * @return module version
   */
  @Override
  public Version version () {

    return PackageVersion.VERSION;
  }

  /**
   * Registers the Jakarta XML Bind introspector with the desired priority and inclusion rule.
   *
   * @param context Jackson setup context
   */
  @Override
  public void setupModule (SetupContext context) {

    JakartaXmlBindAnnotationIntrospector intr = new JakartaXmlBindAnnotationIntrospector(context.getTypeFactory());

    intr.setNonNillableInclusion(_nonNillableInclusion);
    switch (_priority) {
      case PRIMARY:
        context.insertAnnotationIntrospector(intr);
        break;
      case SECONDARY:
        context.appendAnnotationIntrospector(intr);
        break;
    }
  }

  /**
   * @return current registration priority
   */
  public Priority getPriority () {

    return _priority;
  }

  /**
   * Sets the registration priority of the Jakarta introspector.
   *
   * @param p priority to apply
   * @return this module for chaining
   */
  public JakartaXmlBindAnnotationModule setPriority (Priority p) {

    _priority = p;
    return this;
  }

  /**
   * @return inclusion policy for non-nillable properties
   */
  public JsonInclude.Include getNonNillableInclusion () {

    return _nonNillableInclusion;
  }

  /**
   * Sets the inclusion policy for non-nillable properties.
   *
   * @param nonNillableInclusion inclusion rule
   * @return this module for chaining
   */
  public JakartaXmlBindAnnotationModule setNonNillableInclusion (JsonInclude.Include nonNillableInclusion) {

    _nonNillableInclusion = nonNillableInclusion;

    return this;
  }
}
