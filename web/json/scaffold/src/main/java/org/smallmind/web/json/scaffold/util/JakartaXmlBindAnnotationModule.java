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
package org.smallmind.web.json.scaffold.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.fasterxml.jackson.module.jakarta.xmlbind.PackageVersion;

public class JakartaXmlBindAnnotationModule extends Module {

  public enum Priority {PRIMARY, SECONDARY}

  protected Priority _priority = Priority.PRIMARY;
  protected JsonInclude.Include _nonNillableInclusion = JsonInclude.Include.ALWAYS;

  public JakartaXmlBindAnnotationModule () {

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

  public Priority getPriority () {

    return _priority;
  }

  public JakartaXmlBindAnnotationModule setPriority (Priority p) {

    _priority = p;
    return this;
  }

  public JsonInclude.Include getNonNillableInclusion () {

    return _nonNillableInclusion;
  }

  public JakartaXmlBindAnnotationModule setNonNillableInclusion (JsonInclude.Include nonNillableInclusion) {

    _nonNillableInclusion = nonNillableInclusion;

    return this;
  }
}
