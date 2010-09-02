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
