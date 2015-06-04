package org.smallmind.phalanx.wire;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.web.jersey.util.JsonCodec;

public class WireContextXmlAdapter extends XmlAdapter<Object[], WireContext[]> {

  @Override
  public WireContext[] unmarshal (Object[] objects) throws Exception {

    WireContext[] contexts;
    LinkedList<WireContext> contextList = new LinkedList<>();

    if (objects != null) {
      for (Object obj : objects) {

        LinkedHashMap<String, Object> objectMap;

        if ((objectMap = (LinkedHashMap<String, Object>)obj).size() == 1) {

          Map.Entry<String, Object> topEntry = objectMap.entrySet().iterator().next();
          Class<? extends WireContext> contextClass;

          if ((contextClass = WireContextManager.getContextClass(topEntry.getKey())) != null) {
            contextList.add(JsonCodec.convert(topEntry.getValue(), contextClass));
          } else {
            contextList.add(new ProtoWireContext(topEntry.getKey(), topEntry.getValue()));
          }
        }
      }
    }

    contexts = new WireContext[contextList.size()];
    contextList.toArray(contexts);

    return contexts;
  }

  @Override
  public Object[] marshal (WireContext[] wireContexts) throws Exception {

    if (wireContexts == null) {

      return null;
    }

    Object[] objects = new Object[wireContexts.length];
    int index = 0;

    for (WireContext wireContext : wireContexts) {
      if (wireContext instanceof ProtoWireContext) {

        LinkedHashMap<String, Object> objectMap = new LinkedHashMap<>();

        objectMap.put(((ProtoWireContext)wireContext).getSkin(), ((ProtoWireContext)wireContext).getGuts());
        objects[index++] = objectMap;
      } else {

        LinkedHashMap<String, Object> objectMap = new LinkedHashMap<>();
        XmlRootElement xmlRootElementAnnotation = wireContext.getClass().getAnnotation(XmlRootElement.class);

        objectMap.put((xmlRootElementAnnotation == null) ? wireContext.getClass().getSimpleName() : xmlRootElementAnnotation.name(), wireContext);
        objects[index++] = objectMap;
      }
    }

    return objects;
  }
}