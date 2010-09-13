package org.smallmind.wicket.component.google.visualization;

import java.util.HashSet;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

      add(new JavascriptNamespaceBehavior(new AbstractReadOnlyModel<String>() {

         @Override
         public String getObject () {

            return getMarkupId();
         }
      }));
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

         StringBuilder scriptBuilder = new StringBuilder();

         scriptBuilder.append(buildTableScript());
         scriptBuilder.append(getMarkupId()).append(".loadData  = function () {");

         for (String panelId : panelIdSet) {
            scriptBuilder.append(panelId).append(".drawChart(").append(getMarkupId()).append('.').append("data);");
         }

         scriptBuilder.append("};");
         scriptBuilder.append("google.setOnLoadCallback(").append(getMarkupId()).append(".loadData);");

         panelIdSet.clear();

         return scriptBuilder.toString();
      }
   }

   public void loadData (AjaxRequestTarget target) {

      target.appendJavascript(buildTableScript());
      target.appendJavascript(getMarkupId() + ".loadData();");
   }

   private String buildTableScript () {

      DataTable dataTable;
      StringBuilder scriptBuilder = new StringBuilder();
      StringBuilder rowBuilder;
      int columnIndex = 0;

      dataTable = getDataTable();

      scriptBuilder.append(getMarkupId()).append('.').append("data = new google.visualization.DataTable();");

      for (ColumnDescription columnDescription : dataTable.getColumnDescriptions()) {
         scriptBuilder.append(getMarkupId()).append(".data.addColumn('").append(columnDescription.getType().getScriptVersion()).append("','").append(columnDescription.getLabel()).append("','").append(columnDescription.getId()).append("');");
         if (columnDescription.hasProperties()) {
            scriptBuilder.append(getMarkupId()).append(".data.setColumnProperties(").append(columnIndex).append(',').append(columnDescription.getPropertiesAsJson()).append(");");
         }
         columnIndex++;
      }

      for (TableRow tableRow : dataTable.getRows()) {
         rowBuilder = new StringBuilder("[");
         for (TableCell tableCell : tableRow.getCells()) {
            if (rowBuilder.length() > 1) {
               rowBuilder.append(',');
            }

            if ((tableCell.getFormattedValue() == null) && (!tableCell.hasProperties())) {
               rowBuilder.append(tableCell.getValue().forScript());
            }
            else {
               rowBuilder.append("{v: ").append(tableCell.getValue().forScript());

               if (tableCell.getFormattedValue() != null) {
                  rowBuilder.append(",f: '").append(tableCell.getFormattedValue()).append('\'');
               }

               if (tableCell.hasProperties()) {
                  rowBuilder.append(",p: ").append(tableCell.getPropertiesAsJson());
               }

               rowBuilder.append('}');
            }
         }
         rowBuilder.append("]");

         scriptBuilder.append(getMarkupId()).append(".data.addRow(").append(rowBuilder).append(");");
      }

      return scriptBuilder.toString();
   }
}
