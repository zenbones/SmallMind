package org.smallmind.wicket.component.google.visualization;

import java.util.HashSet;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableCell;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.DateTimeValue;
import com.google.visualization.datasource.datatable.value.DateValue;
import com.google.visualization.datasource.datatable.value.TextValue;
import com.google.visualization.datasource.datatable.value.TimeOfDayValue;
import com.google.visualization.datasource.datatable.value.Value;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
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
            scriptBuilder.append("data.addColumn('").append(columnDescription.getType().getTypeCodeLowerCase()).append("','").append(columnDescription.getLabel()).append("','").append(columnDescription.getId()).append("');\n");
         }

         for (TableRow tableRow : dataTable.getRows()) {
            rowBuilder = new StringBuilder("[");
            for (TableCell tableCell : tableRow.getCells()) {
               if (rowBuilder.length() > 1) {
                  rowBuilder.append(',');
               }
               rowBuilder.append(asJavascriptValue(tableCell.getValue()));
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

   private String asJavascriptValue (Value value) {

      switch (value.getType()) {
         case TEXT:
            return '\'' + ((TextValue)value).getValue() + '\'';
         case NUMBER:
            return value.toString();
         case BOOLEAN:
            return value.toString();
         case DATE:
            if (value.isNull()) {

               return "null";
            }

            StringBuilder dateBuilder = new StringBuilder("new Date(");

            dateBuilder.append(((DateValue)value).getYear()).append(',').append(((DateValue)value).getMonth()).append(',').append(((DateValue)value).getDayOfMonth()).append(",0,0,0,0)");

            return dateBuilder.toString();
         case DATETIME:
            if (value.isNull()) {

               return "null";
            }

            StringBuilder dateTimeBuilder = new StringBuilder("new Date(");

            dateTimeBuilder.append(((DateTimeValue)value).getYear()).append(',').append(((DateTimeValue)value).getMonth()).append(',').append(((DateTimeValue)value).getDayOfMonth()).append(',').append(((DateTimeValue)value).getHourOfDay()).append(',').append(((DateTimeValue)value).getMinute()).append(',').append(((DateTimeValue)value).getSecond()).append(',').append(((DateTimeValue)value).getMillisecond()).append(')');

            return dateTimeBuilder.toString();
         case TIMEOFDAY:
            if (value.isNull()) {

               return "null";
            }

            StringBuilder timeOfDayBuilder = new StringBuilder("[");

            timeOfDayBuilder.append(((TimeOfDayValue)value).getHours()).append(',').append(((TimeOfDayValue)value).getMinutes()).append(',').append(((TimeOfDayValue)value).getSeconds()).append(',').append(((TimeOfDayValue)value).getMilliseconds()).append(']');

            return timeOfDayBuilder.toString();
         default:
            throw new UnknownSwitchCaseException(value.getType().name());
      }
   }
}
