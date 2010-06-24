package org.smallmind.wicket.behavior;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class JavascriptBehavior extends AbstractBehavior {

   private Class scopeClass;
   private JavascriptModel javascriptModel;
   private String javascriptFileName;

   public JavascriptBehavior (String javascriptFileName, Map<String, String> substitutionMap) {

      this(null, javascriptFileName, substitutionMap);
   }

   public JavascriptBehavior (Class scopeClass, String javascriptFileName, Map<String, String> substitutionMap) {

      this.scopeClass = scopeClass;
      this.javascriptFileName = javascriptFileName;

      javascriptModel = new JavascriptModel(substitutionMap);
   }

   public void bind (Component component) {

      component.add(TextTemplateHeaderContributor.forJavaScript((scopeClass != null) ? scopeClass : component.getClass(), javascriptFileName, javascriptModel));
   }

   private class JavascriptModel extends AbstractReadOnlyModel {

      private Map<String, String> substitutionMap;

      public JavascriptModel (Map<String, String> substitutionMap) {

         this.substitutionMap = substitutionMap;
      }

      public Object getObject () {

         return substitutionMap;
      }
   }
}
