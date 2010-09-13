package org.smallmind.wicket.component.google.visualization.flot;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.smallmind.wicket.behavior.JavascriptNamespaceBehavior;
import org.smallmind.wicket.component.google.visualization.VisualizationPanel;

public class FlotVisualizationPanel extends VisualizationPanel {

   public FlotVisualizationPanel (String id) {

      this(id, null, null);
   }

   public FlotVisualizationPanel (String id, String options) {

      this(id, null, options);
   }

   public FlotVisualizationPanel (String id, IModel<String> javascriptModel) {

      this(id, javascriptModel, null);
   }

   public FlotVisualizationPanel (String id, IModel<String> javascriptModel, String options) {

      super(id, javascriptModel, "org.smallmind.wicket.google.visualization.Flot", options);

      add(new JavascriptNamespaceBehavior(new Model<String>("org.smallmind.wicket.google.visualization")));
      add(new AbstractBehavior() {

         @Override
         public void renderHead (IHeaderResponse response) {

            super.renderHead(response);
            response.renderJavascriptReference(new JavascriptResourceReference(FlotVisualizationPanel.class, "api/excanvas.min.js"));
            response.renderJavascriptReference(new JavascriptResourceReference(FlotVisualizationPanel.class, "api/jquery.js"));
            response.renderJavascriptReference(new JavascriptResourceReference(FlotVisualizationPanel.class, "api/jquery.flot.js"));
            response.renderJavascriptReference(new JavascriptResourceReference(FlotVisualizationPanel.class, "api/jquery.flot.stack.js"));
            response.renderJavascriptReference(new JavascriptResourceReference(FlotVisualizationPanel.class, "api/visualization.flot.js"));
         }
      });
   }
}
