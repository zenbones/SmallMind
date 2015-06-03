package org.smallmind.web.jersey.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class BooleanXmlAdapter extends XmlAdapter<String, Boolean> {

  @Override
  public Boolean unmarshal (String value) {

    return Boolean.parseBoolean(value);
  }

  @Override
  public String marshal (Boolean value) {

    return (value == null) ? null : (value) ? "true" : "false";
  }
}