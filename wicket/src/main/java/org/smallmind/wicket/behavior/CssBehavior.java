/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.wicket.behavior;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class CssBehavior extends AbstractBehavior {

   private Class scopeClass;
   private CssModel cssModel;
   private String cssFileName;

   public CssBehavior () {

      this(null, null, null);
   }

   public CssBehavior (Properties cssProperties) {

      this(null, null, cssProperties);
   }

   public CssBehavior (String cssFileName) {

      this(null, cssFileName, null);
   }

   public CssBehavior (String cssFileName, Properties cssProperties) {

      this(null, cssFileName, cssProperties);
   }

   public CssBehavior (Class scopeClass) {

      this(scopeClass, null, null);
   }

   public CssBehavior (Class scopeClass, Properties cssProperties) {

      this(scopeClass, null, cssProperties);
   }

   public CssBehavior (Class scopeClass, String cssFileName, Properties cssProperties) {

      this.scopeClass = scopeClass;
      this.cssFileName = cssFileName;

      if (cssProperties != null) {
         cssModel = new CssModel(cssProperties);
      }
   }

   public void bind (Component component) {

      component.add(TextTemplateHeaderContributor.forCss((scopeClass != null) ? scopeClass : component.getClass(), (cssFileName != null) ? cssFileName : (scopeClass != null) ? scopeClass.getSimpleName() + ".css" : component.getClass().getSimpleName() + ".css", cssModel));
   }

   private class CssModel extends AbstractReadOnlyModel<Map<String, Object>> {

      private Map<String, Object> substitutionMap;

      public CssModel (Properties cssProperties) {

         substitutionMap = new HashMap<String, Object>();

         for (Map.Entry<Object, Object> propertyEntry : cssProperties.entrySet()) {
            substitutionMap.put(propertyEntry.getKey().toString(), propertyEntry.getValue());
         }
      }

      public Map<String, Object> getObject () {

         return substitutionMap;
      }
   }
}