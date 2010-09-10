package org.smallmind.wicket.behavior;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

public class JavascriptNamespaceBehavior extends AbstractBehavior {

   @Override
   public void renderHead (IHeaderResponse response) {

      super.renderHead(response);
      response.renderJavascriptReference(new JavascriptResourceReference(JavascriptNamespaceBehavior.class, "JavascriptNamespaceBehavior.js"), JavascriptNamespaceBehavior.class.getName());
   }
}