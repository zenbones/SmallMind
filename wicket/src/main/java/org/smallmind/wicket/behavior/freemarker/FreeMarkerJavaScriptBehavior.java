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
package org.smallmind.wicket.behavior.freemarker;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.smallmind.wicket.util.FreeMarkerPackageTextTemplate;

public class FreeMarkerJavaScriptBehavior extends Behavior {

  private Map<String, Object> rootModel;
  private Class<?> scopeClass;
  private String fileName;

  public FreeMarkerJavaScriptBehavior (Map<String, Object> rootModel) {

    this(null, null, rootModel);
  }

  public FreeMarkerJavaScriptBehavior (String fileName, Map<String, Object> rootModel) {

    this(null, fileName, rootModel);
  }

  public FreeMarkerJavaScriptBehavior (Class<?> scopeClass, String fileName, Map<String, Object> rootModel) {

    this.scopeClass = scopeClass;
    this.fileName = fileName;
    this.rootModel = rootModel;
  }

  @Override
  public void renderHead (Component component, IHeaderResponse response) {

    Class<?> interpolatedClass = (scopeClass != null) ? scopeClass : component.getClass();
    String interpolatedFileName = (fileName == null) ? component.getClass().getSimpleName() + ".js" : fileName;

    response.renderJavaScript(new FreeMarkerPackageTextTemplate(interpolatedClass, interpolatedFileName).asString(rootModel), interpolatedClass.getName() + ":" + interpolatedFileName);
  }
}
