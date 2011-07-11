package org.smallmind.persistence.orm.type;

import java.util.Locale;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LocaleAdapter extends XmlAdapter<String, Locale> {

  @Override
  public Locale unmarshal (String value) throws Exception {

    if (value == null) {

      return null;
    }
    else if (value.toLowerCase().equals("default")) {

      return Locale.getDefault();
    }
    else {

      String[] localeParts = value.split("_", 3);

      switch (localeParts.length) {
        case 0:
          return new Locale(localeParts[0]);
        case 1:
          return new Locale(localeParts[0], localeParts[1]);
        case 2:
          return new Locale(localeParts[0], localeParts[1], localeParts[2]);
        default:
          throw new TypeFormatException("Not a valid locale(%s)", value);
      }
    }
  }

  @Override
  public String marshal (Locale locale) throws Exception {

    return locale.toString();
  }
}
