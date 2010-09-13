package org.smallmind.wicket.behavior;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;

public class JavascriptNamespaceBehavior extends AbstractBehavior {

   private IModel<String> namespaceModel;

   public JavascriptNamespaceBehavior () {

      this(null);
   }

   public JavascriptNamespaceBehavior (IModel<String> namespaceModel) {

      this.namespaceModel = namespaceModel;
   }

   @Override
   public void renderHead (IHeaderResponse response) {

      super.renderHead(response);
      response.renderJavascriptReference(new JavascriptResourceReference(JavascriptNamespaceBehavior.class, "JavascriptNamespaceBehavior.js"), JavascriptNamespaceBehavior.class.getName());

      if (namespaceModel != null) {
         response.renderJavascript("Namespace.Manager.Register('" + namespaceModel.getObject() + "');", JavascriptNamespaceBehavior.class.getName() + '.' + namespaceModel.getObject());
      }
   }
}