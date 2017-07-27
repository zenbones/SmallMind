/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jersey.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.fasterxml.jackson.module.jaxb.PackageVersion;

/**
 * Module that can be registered to add support for JAXB annotations.
 * It does basically equivalent of
 * <pre>
 *   objectMapper.setAnnotationIntrospector(...);
 * </pre>
 * with combination of {@link JaxbAnnotationIntrospector} and existing
 * default introspector(s) (if any), depending on configuration
 * (by default, JAXB annotations are used as {@link com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule.Priority#PRIMARY}
 * annotations).
 */
public class JaxbAnnotationModule extends Module {

  /**
   * Enumeration that defines how we use JAXB Annotations: either
   * as "primary" annotations (before any other already configured
   * introspector -- most likely default JacksonAnnotationIntrospector) or
   * as "secondary" annotations (after any other already configured
   * introspector(s)).
   * <p>
   * Default choice is <b>PRIMARY</b>
   * <p>
   * Note that if you want to use JAXB annotations as the only annotations,
   * you must directly set annotation introspector by calling
   * {@link com.fasterxml.jackson.databind.ObjectMapper#setAnnotationIntrospector}.
   */
  public enum Priority {
    PRIMARY, SECONDARY;
  }

  /**
   * Priority to use when registering annotation introspector: default
   * value is {@link com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule.Priority#PRIMARY}.
   */
  protected Priority _priority = Priority.PRIMARY;
  protected JsonInclude.Include _nonNillableInclusion = JsonInclude.Include.ALWAYS;

    /*
    /**********************************************************
    /* Life cycle
    /**********************************************************
     */

  public JaxbAnnotationModule () {

  }

  @Override
  public String getModuleName () {

    return getClass().getSimpleName();
  }

  @Override
  public Version version () {

    return PackageVersion.VERSION;
  }

  @Override
  public void setupModule (SetupContext context) {

    JaxbAnnotationIntrospector intr = new JaxbAnnotationIntrospector(context.getTypeFactory());

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

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

  public Priority getPriority () {

    return _priority;
  }

  /**
   * Method for defining whether JAXB annotations should be added
   * as primary or secondary annotations (compared to already registered
   * annotations).
   * <p>
   * NOTE: method MUST be called before registering the module -- calling
   * afterwards will not have any effect on previous registrations.
   */
  public JaxbAnnotationModule setPriority (Priority p) {

    _priority = p;
    return this;
  }

  public JsonInclude.Include getNonNillableInclusion () {

    return _nonNillableInclusion;
  }

  public JaxbAnnotationModule setNonNillableInclusion (JsonInclude.Include nonNillableInclusion) {

    _nonNillableInclusion = nonNillableInclusion;

    return this;
  }
}

