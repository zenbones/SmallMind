package org.smallmind.wicket.component.google.visualization;

import java.util.HashSet;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.smallmind.wicket.behavior.JavascriptNamespaceBehavior;

public abstract class VisualizationBorder extends Border {

   private HashSet<String> panelIdSet = new HashSet<String>();

   public VisualizationBorder (String id) {

      super(id);

      Label scriptLabel;

      add(new JavascriptNamespaceBehavior());

      add(new AbstractBehavior() {

         @Override
         public void renderHead (IHeaderResponse response) {

            super.renderHead(response);
            response.renderJavascript("google.load('visualization', '" + VisualizationInfo.getVersion() + "');", VisualizationBorder.class.getName());
         }
      });

      scriptLabel = new Label("visualizationBorderScript", new VisualizationBorderScriptModel());
      scriptLabel.setEscapeModelStrings(false);

      add(scriptLabel);
   }

   public abstract DataTable getDataTable ();

   protected void addRenderingPanel (String id) {

      panelIdSet.add(id);
   }

   private class VisualizationBorderScriptModel extends AbstractReadOnlyModel<String> {

      @Override
      public String getObject () {

         DataTable dataTable;
         StringBuilder scriptBuilder = new StringBuilder();
         StringBuilder rowBuilder;

         dataTable = getDataTable();

         scriptBuilder.append("Namespace.Manager.Register('").append(getMarkupId()).append("');");
         scriptBuilder.append(getMarkupId()).append(".loadData  = function () {");
         scriptBuilder.append("data = new google.visualization.DataTable();");

         for (ColumnDescription columnDescription : dataTable.getColumnDescriptions()) {
            scriptBuilder.append("data.addColumn('").append(columnDescription.getType().getScriptVersion()).append("','").append(columnDescription.getLabel()).append("','").append(columnDescription.getId()).append("');");
         }

         for (TableRow tableRow : dataTable.getRows()) {
            rowBuilder = new StringBuilder("[");
            for (TableCell tableCell : tableRow.getCells()) {
               if (rowBuilder.length() > 1) {
                  rowBuilder.append(',');
               }
               rowBuilder.append(tableCell.getValue());
            }
            rowBuilder.append("]");

            scriptBuilder.append("data.addRow(").append(rowBuilder).append(");");
         }

         for (String panelId : panelIdSet) {
            scriptBuilder.append(panelId).append(".drawChart(data);");
         }

         scriptBuilder.append("};");
         scriptBuilder.append("google.setOnLoadCallback(").append(getMarkupId()).append(".loadData);");

         panelIdSet.clear();

         return scriptBuilder.toString();
      }
   }
}
