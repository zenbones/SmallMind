/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.wicket.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.template.PackageTextTemplate;

public class CssBehavior extends Behavior {

  private Map<String, Object> substitutionMap;
  private Class scopeClass;
  private String fileName;

  public CssBehavior () {

    this(null, null, null);
  }

  public CssBehavior (Properties cssProperties) {

    this(null, null, cssProperties);
  }

  public CssBehavior (String fileName) {

    this(null, fileName, null);
  }

  public CssBehavior (String fileName, Properties cssProperties) {

    this(null, fileName, cssProperties);
  }

  public CssBehavior (Class scopeClass) {

    this(scopeClass, null, null);
  }

  public CssBehavior (Class scopeClass, Properties cssProperties) {

    this(scopeClass, null, cssProperties);
  }

  public CssBehavior (Class scopeClass, String fileName, Properties cssProperties) {

    this.scopeClass = scopeClass;
    this.fileName = fileName;

    if (cssProperties != null) {
      substitutionMap = new HashMap<String, Object>();

      for (Map.Entry<Object, Object> propertyEntry : cssProperties.entrySet()) {
        substitutionMap.put(propertyEntry.getKey().toString(), propertyEntry.getValue());
      }
    }
  }

  @Override
  public void renderHead (Component component, IHeaderResponse response) {

    Class<?> interpolatedClass = (scopeClass != null) ? scopeClass : component.getClass();
    String interpolatedFileName = (fileName != null) ? fileName : (scopeClass != null) ? scopeClass.getSimpleName() + ".css" : component.getClass().getSimpleName() + ".css";

    response.render(CssHeaderItem.forCSS(new PackageTextTemplate(interpolatedClass, interpolatedFileName).asString(substitutionMap), interpolatedClass.getName() + ":" + interpolatedFileName));
  }
}