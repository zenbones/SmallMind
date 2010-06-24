package org.smallmind.wicket.behavior;

import java.util.Properties;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

public class CssBehavior extends AbstractBehavior {

   private Class scopeClass;
   private CssModel cssModel;
   private String cssFileName;

   public CssBehavior (String cssFileName, Properties cssProperties) {

      this(null, cssFileName, cssProperties);
   }

   public CssBehavior (Class scopeClass, String cssFileName, Properties cssProperties) {

      this.scopeClass = scopeClass;
      this.cssFileName = cssFileName;

      cssModel = new CssModel(cssProperties);
   }

   public void bind (Component component) {

      component.add(TextTemplateHeaderContributor.forCss((scopeClass != null) ? scopeClass : component.getClass(), cssFileName, cssModel));
   }

   private class CssModel extends AbstractReadOnlyModel {

      private Properties cssProperties;

      public CssModel (Properties cssProperties) {

         this.cssProperties = cssProperties;
      }

      public Object getObject () {

         return cssProperties;
      }
   }
}