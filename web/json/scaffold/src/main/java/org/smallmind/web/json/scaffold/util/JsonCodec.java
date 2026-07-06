/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.json.scaffold.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Centralized Jackson-backed facade for reading, writing, converting, and copying JSON trees and
 * POJOs. The codec is exposed as a mutable singleton: retrieve the active instance through
 * {@link #instance()} and invoke the reading and writing methods on it. The backing
 * {@link ObjectMapper} can be replaced at startup through {@link #redefine(ObjectMapper)}, and
 * {@link #standardMapperBuilder()} hands back a fresh, pre-configured builder so callers can layer
 * additional modules on top of the standard configuration.
 */
public class JsonCodec {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>();
  private static JsonCodec INSTANCE = new JsonCodec();
  private final ObjectMapper objectMapper;

  private JsonCodec () {

    this(standardMapperBuilder().build());
  }

  private JsonCodec (ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;
  }

  /**
   * Returns a fresh, pre-configured {@link JsonMapper.Builder} matching the standard codec
   * configuration. A new builder is constructed on each call, so additional modules or features can
   * be layered onto it — typically to build a customized {@link ObjectMapper} for installation
   * through {@link #redefine(ObjectMapper)} — without disturbing the shared instance or other callers.
   *
   * @return a fresh, pre-configured mapper builder
   */
  public static JsonMapper.Builder standardMapperBuilder () {

    return JsonMapper.builder()
             // TODO: Bring back when fixed
             // AfterBurner fails with Jackson 3.x parsing implemented methods of an interface (multiple definitions of method <methode> found)
             // .addModule(new AfterburnerModule())
             .addModule(new JakartaXmlBindAnnotationModule().setNonNillableInclusion(JsonInclude.Include.NON_NULL))
             .addModule(new PolymorphicModule())
             .enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);
  }

  /**
   * Returns the codec currently installed as the shared singleton.
   *
   * @return the active {@code JsonCodec} instance
   */
  public static JsonCodec instance () {

    return INSTANCE;
  }

  /**
   * Installs a new shared codec backed by the supplied {@link ObjectMapper}, replacing the previous
   * singleton so that subsequent calls to {@link #instance()} return the new codec. Doing this can
   * come as a surprise to all callers, and the intention is that this is engaged *once*, very early in
   * your service life cycle.
   *
   * @param objectMapper the mapper to back the new codec
   * @return the newly installed {@code JsonCodec} instance
   */
  public static JsonCodec redefine (ObjectMapper objectMapper) {

    INSTANCE = new JsonCodec(objectMapper);

    return INSTANCE;
  }

  /**
   * Recursively sorts the field names of object nodes using an alphanumeric comparator.
   *
   * @param node node to sort
   * @return a new object node with sorted fields, or the original node for non-object types
   */
  private JsonNode sort (JsonNode node) {

    if (node == null) {

      return null;
    } else if (node.isObject()) {

      ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
      LinkedList<String> sortedPropertyNameList = new LinkedList<>(node.propertyNames());

      sortedPropertyNameList.sort(ALPHA_NUMERIC_COMPARATOR);

      for (String sortedPropertyName : sortedPropertyNameList) {
        objectNode.set(sortedPropertyName, sort(node.get(sortedPropertyName)));
      }

      return objectNode;
    } else {

      return node;
    }
  }

  /**
   * Parses a JSON byte array into a {@link JsonNode}.
   *
   * @param bytes JSON payload
   * @return parsed tree node
   */
  public JsonNode readAsJsonNode (byte[] bytes) {

    return objectMapper.readTree(bytes);
  }

  /**
   * Parses a JSON string into a {@link JsonNode}.
   *
   * @param aString JSON payload
   * @return parsed tree node
   */
  public JsonNode readAsJsonNode (String aString) {

    return objectMapper.readTree(aString);
  }

  /**
   * Parses JSON from an input stream into a {@link JsonNode}.
   *
   * @param inputStream stream containing JSON data
   * @return parsed tree node
   */
  public JsonNode readAsJsonNode (InputStream inputStream) {

    return objectMapper.readTree(inputStream);
  }

  /**
   * Deserializes JSON bytes into the requested type.
   *
   * @param bytes JSON payload
   * @param clazz target class
   * @param <T>   target type
   * @return deserialized object
   */
  public <T> T read (byte[] bytes, Class<T> clazz) {

    return objectMapper.readValue(bytes, clazz);
  }

  /**
   * Deserializes a slice of JSON bytes into the requested type.
   *
   * @param bytes  JSON payload buffer
   * @param offset start offset within the buffer
   * @param len    number of bytes to read
   * @param clazz  target class
   * @param <T>    target type
   * @return deserialized object
   */
  public <T> T read (byte[] bytes, int offset, int len, Class<T> clazz) {

    return objectMapper.readValue(bytes, offset, len, clazz);
  }

  /**
   * Deserializes a JSON string into the requested type.
   *
   * @param aString JSON payload
   * @param clazz   target class
   * @param <T>     target type
   * @return deserialized object
   */
  public <T> T read (String aString, Class<T> clazz) {

    return objectMapper.readValue(aString, clazz);
  }

  /**
   * Deserializes JSON from an input stream into the requested type.
   *
   * @param inputStream JSON data stream
   * @param clazz       target class
   * @param <T>         target type
   * @return deserialized object
   */
  public <T> T read (InputStream inputStream, Class<T> clazz) {

    return objectMapper.readValue(inputStream, clazz);
  }

  /**
   * Deserializes the current token from an existing {@link JsonParser} into the requested type.
   *
   * @param parser parser positioned at the value to read
   * @param clazz  target class
   * @param <T>    target type
   * @return deserialized object
   */
  public <T> T read (JsonParser parser, Class<T> clazz) {

    return objectMapper.readValue(parser, clazz);
  }

  /**
   * Converts a JSON tree node into a POJO of the requested type.
   *
   * @param node  source tree node
   * @param clazz target class
   * @param <T>   target type
   * @return converted POJO
   */
  public <T> T read (JsonNode node, Class<T> clazz) {

    return objectMapper.treeToValue(node, clazz);
  }

  /**
   * Serializes a POJO into a Jackson tree node.
   *
   * @param obj source object
   * @return JSON tree representation
   */
  public JsonNode writeAsJsonNode (Object obj) {

    return objectMapper.valueToTree(obj);
  }

  /**
   * Serializes a POJO to a JSON byte array.
   *
   * @param obj object to serialize
   * @return JSON bytes
   */
  public byte[] writeAsBytes (Object obj) {

    return objectMapper.writeValueAsBytes(obj);
  }

  /**
   * Serializes a POJO to a compact JSON string.
   *
   * @param obj object to serialize
   * @return compact JSON string
   */
  public String writeAsString (Object obj) {

    return objectMapper.writeValueAsString(obj);
  }

  /**
   * Serializes a POJO to a pretty-printed JSON string with object fields sorted alphanumerically.
   *
   * @param obj object to serialize
   * @return formatted, sorted JSON string
   */
  public String writeAsPrettyPrintedString (Object obj) {

    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sort(objectMapper.valueToTree(obj)));
  }

  /**
   * Writes a POJO as JSON directly to an output stream.
   *
   * @param outputStream destination stream
   * @param obj          object to serialize
   */
  public void writeToStream (OutputStream outputStream, Object obj) {

    objectMapper.writeValue(outputStream, obj);
  }

  /**
   * Converts an object to another type using Jackson's data-binding conversion.
   *
   * @param obj   source object
   * @param clazz target class
   * @param <T>   target type
   * @return converted value
   */
  public <T> T convert (Object obj, Class<T> clazz) {

    return objectMapper.convertValue(obj, clazz);
  }

  /**
   * Deep-copies a JSON node, recursively cloning object and array structures.
   *
   * @param node node to copy
   * @return independent copy of the node, or {@code null} if the input is {@code null}
   */
  public JsonNode copy (JsonNode node) {

    if (node == null) {

      return null;
    } else {
      switch (node.getNodeType()) {
        case OBJECT:

          ObjectNode objectNode = JsonNodeFactory.instance.objectNode();

          for (Map.Entry<String, JsonNode> nodeEntry : node.properties()) {
            objectNode.set(nodeEntry.getKey(), copy(nodeEntry.getValue()));
          }

          return objectNode;
        case ARRAY:

          ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode(node.size());

          for (JsonNode item : node) {
            arrayNode.add(copy(item));
          }

          return arrayNode;
        default:

          return node;
      }
    }
  }
}
