package org.smallmind.web.json.dto;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class DefaultXmlAdapter extends XmlAdapter<Object, Object> {

  @Override
  public Object unmarshal (Object v) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Object marshal (Object v) {

    throw new UnsupportedOperationException();
  }
}
