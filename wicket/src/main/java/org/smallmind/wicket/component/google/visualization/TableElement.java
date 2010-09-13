package org.smallmind.wicket.component.google.visualization;

import java.io.IOException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.smallmind.wicket.FormattedWicketRuntimeException;

public class TableElement {

   private static final JsonFactory JSON_FACTORY = new JsonFactory();

   private JsonNode jsonNode;

   public synchronized void setProperties (String jsonValue) {

      try {
         jsonNode = JSON_FACTORY.createJsonParser(jsonValue).readValueAsTree();
      }
      catch (IOException ioException) {
         throw new FormattedWicketRuntimeException(ioException);
      }
   }

   public boolean hasProperties () {

      return jsonNode != null;
   }

   public synchronized String getPropertiesAsJson () {

      return (jsonNode == null) ? "{}" : jsonNode.toString();
   }
}
