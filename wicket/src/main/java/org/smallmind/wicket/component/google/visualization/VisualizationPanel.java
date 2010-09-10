package org.smallmind.wicket.component.google.visualization;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.smallmind.wicket.FormattedWicketRuntimeException;
import org.smallmind.wicket.behavior.JavascriptNamespaceBehavior;

public class VisualizationPanel extends Panel {

   public VisualizationPanel (String id, String divId, String jsonClass) {

      this(id, null, divId, jsonClass, null);
   }

   public VisualizationPanel (String id, String divId, String jsonClass, String options) {

      this(id, null, divId, jsonClass, options);
   }

   public VisualizationPanel (String id, IModel<String> javascriptModel, String divId, String jsonClass) {

      this(id, javascriptModel, divId, jsonClass, null);
   }

   public VisualizationPanel (String id, IModel<String> javascriptModel, String divId, String jsonClass, String options) {

      super(id);

      Label divLabel;
      Label scriptLabel;

      add(new JavascriptNamespaceBehavior());

      divLabel = new Label("visualizationDiv", new Model<String>("<div id=\"" + divId + "\"></div>"));
      divLabel.setEscapeModelStrings(false);

      scriptLabel = new Label("visualizationPanelScript", new VisualizationPanelScriptModel(javascriptModel, divId, jsonClass, options));
      scriptLabel.setEscapeModelStrings(false);

      add(divLabel);
      add(scriptLabel);
   }

   public String getJavascriptDataVariable () {

      return "data";
   }

   private class VisualizationPanelScriptModel extends AbstractReadOnlyModel<String> {

      private IModel<String> javascriptModel;
      private String divId;
      private String jsonClass;
      private String options;

      public VisualizationPanelScriptModel (IModel<String> javascriptModel, String divId, String jsonClass, String options) {

         this.javascriptModel = javascriptModel;
         this.divId = divId;
         this.jsonClass = jsonClass;
         this.options = options;
      }

      @Override
      public String getObject () {

         VisualizationBorder visualizationBorder;
         StringBuilder scriptBuilder = new StringBuilder();

         if ((visualizationBorder = findParent(VisualizationBorder.class)) == null) {
            throw new FormattedWicketRuntimeException("%s(%s) is not with the context of a %s", VisualizationPanel.class.getSimpleName(), getMarkupId(), VisualizationBorder.class.getSimpleName());
         }

         scriptBuilder.append("Namespace.Manager.Register('").append(getMarkupId()).append("');");
         scriptBuilder.append(getMarkupId()).append(".drawChart  = function (data) {");

         if (javascriptModel != null) {
            scriptBuilder.append(javascriptModel.getObject());
         }

         scriptBuilder.append("var chart = new ").append(jsonClass).append("(document.getElementById('").append(divId).append("'));");
         scriptBuilder.append("chart.draw(").append(getJavascriptDataVariable()).append(", ").append((options == null) ? "{}" : options).append(");");
         scriptBuilder.append('}');

         visualizationBorder.addRenderingPanel(getMarkupId());

         return scriptBuilder.toString();
      }
   }
}
