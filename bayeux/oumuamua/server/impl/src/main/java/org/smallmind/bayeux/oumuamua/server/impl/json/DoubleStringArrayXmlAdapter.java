package org.smallmind.bayeux.oumuamua.server.impl.json;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class DoubleStringArrayXmlAdapter extends XmlAdapter<String, String[][]> {

  @Override
  public String[][] unmarshal (String s) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String marshal (String[][] doubleArray) {

    if (doubleArray == null) {

      return null;
    } else {

      StringBuilder builder = new StringBuilder("[");
      boolean first = true;

      for (String[] array : doubleArray) {
        if (!first) {
          builder.append(",");
        }

        builder.append("/").append(String.join("/", array));
        first = false;
      }

      return builder.append("]").toString();
    }
  }
}
