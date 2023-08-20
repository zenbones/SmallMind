package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class DefaultSerDes implements JsonSerDes {

  @Override
  public ObjectNode to (byte[] buffer)
    throws IOException {

    return (ObjectNode)JsonCodec.readAsJsonNode(buffer);
  }

  @Override
  public byte[] from (ObjectNode objectNode)
    throws JsonProcessingException {

    return JsonCodec.writeAsBytes(objectNode);
  }

  @Override
  public ObjectNode copy (ObjectNode objectNode) {

    return (ObjectNode)JsonCodec.copy(objectNode);
  }
}
