package org.smallmind.cometd.oumuamua.v1.json.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import org.smallmind.cometd.oumuamua.v1.json.Value;
import org.smallmind.cometd.oumuamua.v1.json.ValueType;

public class JacksonValue implements Value {

  private JsonNode node;

  @Override
  public ValueType getType () {

    switch (node.getNodeType()) {
      case OBJECT:
        return ValueType.OBJECT;
      case ARRAY:
        return ValueType.ARRAY;
      case STRING:
        return ValueType.STRING;
      case NUMBER:
        return ValueType.NUMBER;
      case BOOLEAN:
        return ValueType.BOOLEAN;
      case NULL:
        return ValueType.NULL;
      default:
        throw new InvalidJsonNodeType(node.getNodeType().name());
    }
  }
}
