package org.smallmind.cometd.oumuamua.message;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class ExtMapLike extends MapLike {

  private final MapLike parent;
  private JsonNode extNode;

  public ExtMapLike (MapLike mapLike) {

    super(null, mapLike.getNode());

    parent = mapLike;
  }

  @Override
  public Object get (Object key) {

    if ("ext".equals(key)) {
      if (extNode != null) {

        return out(this, extNode);
      } else {

        JsonNode parentExtNode;

        if ((parentExtNode = getNode().get("ext")) == null) {

          return null;
        } else if (JsonNodeType.OBJECT.equals(parentExtNode.getNodeType())) {

          return out(this, extNode = (ObjectNode)JsonCodec.clone(parentExtNode));
        } else {

          return out(parent, parentExtNode);
        }
      }
    } else {

      return parent.get(key);
    }
  }

  @Override
  public Map<String, Object> getAsMapLike (String key) {

    if ("ext".equals(key)) {
      if (extNode != null) {

        return JsonNodeType.OBJECT.equals(extNode.getNodeType()) ? new MapLike(this, (ObjectNode)extNode) : null;
      } else {

        JsonNode parentExtNode;

        if ((parentExtNode = getNode().get("ext")) == null) {

          return null;
        } else if (JsonNodeType.OBJECT.equals(parentExtNode.getNodeType())) {

          return new MapLike(this, (ObjectNode)(extNode = JsonCodec.clone(parentExtNode)));
        } else {

          return null;
        }
      }
    } else {

      return parent.getAsMapLike(key);
    }
  }

  @Override
  public Map<String, Object> createIfAbsentMapLike (String key) {

    if ("ext".equals(key)) {
      if (extNode != null) {

        return JsonNodeType.OBJECT.equals(extNode.getNodeType()) ? new MapLike(this, (ObjectNode)extNode) : null;
      } else {

        JsonNode parentExtNode;

        if (((parentExtNode = getNode().get("ext")) == null) || (!JsonNodeType.OBJECT.equals(parentExtNode.getNodeType()))) {

          return new MapLike(this, (ObjectNode)(extNode = JsonNodeFactory.instance.objectNode()));
        } else {

          return new MapLike(this, (ObjectNode)(extNode = JsonCodec.clone(parentExtNode)));
        }
      }
    } else {

      return parent.createIfAbsentMapLike(key);
    }
  }

  @Override
  public Object put (String key, Object value) {

    if ("ext".equals(key)) {

      Object previousValue = out(null, extNode);

      extNode = in(value);

      return previousValue;
    } else {

      return parent.put(key, value);
    }
  }
}
