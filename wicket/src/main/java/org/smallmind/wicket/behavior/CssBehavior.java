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