package org.smallmind.wicket.behavior;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

public class JavascriptNamespaceBehavior extends AbstractBehavior {

   private String namespace;

   public JavascriptNamespaceBehavior () {

      this(null);
   }

   public JavascriptNamespaceBehavior (String namespace) {

      this.namespace = namespace;
   }

   @Override
   public void renderHead (IHeaderResponse response) {

      super.renderHead(response);
      response.renderJavascriptReference(new JavascriptResourceReference(JavascriptNamespaceBehavior.class, "JavascriptNamespaceBehavior.js"), JavascriptNamespaceBehavior.class.getName());

      if (namespace != null) {
         response.renderJavascript("Namespace.Manager.Register('" + namespace + "');", JavascriptNamespaceBehavior.class.getName() + '.' + namespace);
      }
   }
}