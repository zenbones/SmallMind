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

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class JavascriptBehavior extends AbstractBehavior {

   private Class scopeClass;
   private JavascriptModel javascriptModel;
   private String fileName;

   public JavascriptBehavior (String fileName, Map<String, Object> substitutionMap) {

      this(null, fileName, substitutionMap);
   }

   public JavascriptBehavior (Class scopeClass, String fileName, Map<String, Object> substitutionMap) {

      this.scopeClass = scopeClass;
      this.fileName = fileName;

      javascriptModel = new JavascriptModel(substitutionMap);
   }

   public void bind (Component component) {

      component.add(TextTemplateHeaderContributor.forJavaScript((scopeClass != null) ? scopeClass : component.getClass(), (fileName == null) ? component.getClass().getSimpleName() + ".js" : fileName, javascriptModel));
   }

   private class JavascriptModel extends AbstractReadOnlyModel<Map<String, Object>> {

      private Map<String, Object> substitutionMap;

      public JavascriptModel (Map<String, Object> substitutionMap) {

         this.substitutionMap = substitutionMap;
      }

      public Map<String, Object> getObject () {

         return substitutionMap;
      }
   }
}
