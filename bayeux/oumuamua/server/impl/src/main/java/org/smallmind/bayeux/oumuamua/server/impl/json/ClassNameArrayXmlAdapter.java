package org.smallmind.bayeux.oumuamua.server.impl.json;

import java.util.Arrays;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.util.MutationUtility;

public class ClassNameArrayXmlAdapter extends XmlAdapter<String, Object[]> {

  @Override
  public Object[] unmarshal (String s) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String marshal (Object[] objArray) {

    return (objArray == null) ? null : Arrays.toString(MutationUtility.toArray(objArray, String.class, obj -> obj.getClass().getName()));
  }
}
