package org.smallmind.bayeux.oumuamua.server.spi.json.jackson;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface JsonSerDes {

  ObjectNode to (byte[] buffer)
    throws IOException;

  byte[] from (ObjectNode objectNode)
    throws JsonProcessingException;

  ObjectNode copy (ObjectNode objectNode);
}
