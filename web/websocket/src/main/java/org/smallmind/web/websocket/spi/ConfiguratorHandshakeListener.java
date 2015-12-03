package org.smallmind.web.websocket.spi;

import java.util.List;
import java.util.Map;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.HandshakeResponse;
import org.smallmind.nutsnbolts.util.Tuple;
import org.smallmind.web.websocket.HandshakeListener;

public class ConfiguratorHandshakeListener implements HandshakeListener {

  private ClientEndpointConfig.Configurator configurator;

  public ConfiguratorHandshakeListener (ClientEndpointConfig.Configurator configurator) {

    this.configurator = configurator;
  }

  @Override
  public void beforeRequest (Tuple<String, String> headerTuple) {

    Map<String, List<String>> headerMap = headerTuple.asMap();

    configurator.beforeRequest(headerMap);

    headerTuple.clear();
    for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {

      List<String> valueList;

      if (((valueList = entry.getValue()) != null) && (!valueList.isEmpty())) {
        for (String value : valueList) {
          headerTuple.addPair(entry.getKey(), value);
        }
      }
    }
  }

  @Override
  public void afterResponse (final Tuple<String, String> headerTuple) {

    configurator.afterResponse(new HandshakeResponse() {

      @Override
      public Map<String, List<String>> getHeaders () {

        return headerTuple.asMap();
      }
    });
  }
}
