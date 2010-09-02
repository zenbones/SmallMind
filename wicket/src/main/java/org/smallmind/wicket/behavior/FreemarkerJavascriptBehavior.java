package org.smallmind.wicket.behavior;

import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.smallmind.wicket.util.FreemarkerPackagedTextTemplate;

public class FreemarkerJavascriptBehavior extends AbstractBehavior {

   private Map<String, Object> rootModel;
   private Class<?> scopeClass;
   private String fileName;

   public FreemarkerJavascriptBehavior (Map<String, Object> rootModel) {

      this(null, null, rootModel);
   }

   public FreemarkerJavascriptBehavior (String fileName, Map<String, Object> rootModel) {

      this(null, fileName, rootModel);
   }

   public FreemarkerJavascriptBehavior (Class<?> scopeClass, String fileName, Map<String, Object> rootModel) {

      this.scopeClass = scopeClass;
      this.fileName = fileName;
      this.rootModel = rootModel;
   }

   public void bind (Component component) {

      component.add(TextTemplateHeaderContributor.forJavaScript(new FreemarkerPackagedTextTemplate((scopeClass != null) ? scopeClass : component.getClass(), (fileName == null) ? component.getClass().getSimpleName() + ".js" : fileName), new JavascriptModel(rootModel)));
   }

   private class JavascriptModel extends AbstractReadOnlyModel<Map<String, Object>> {

      private Map<String, Object> rootModel;

      public JavascriptModel (Map<String, Object> rootModel) {

         this.rootModel = rootModel;
      }

      public Map<String, Object> getObject () {

         return rootModel;
      }
   }
}
