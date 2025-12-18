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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.IterableIterator;

/**
 * Centralized Jackson configuration and convenience helpers for reading/writing JSON and converting
 * between POJOs and tree representations.
 */
public class JsonCodec {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>();
  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
                                                      .addModule(new AfterburnerModule())
                                                      .addModule(new JakartaXmlBindAnnotationModule().setNonNillableInclusion(JsonInclude.Include.NON_NULL))
                                                      .addModule(new PolymorphicModule())
                                                      .enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME).build();

  /**
   * Reads a JSON byte array into a {@link JsonNode}.
   *
   * @param bytes JSON payload
   * @return parsed node
   * @throws IOException if parsing fails
   */
  public static JsonNode readAsJsonNode (byte[] bytes)
    throws IOException {

    return OBJECT_MAPPER.readTree(bytes);
  }

  /**
   * Reads a JSON string into a {@link JsonNode}.
   *
   * @param aString JSON payload
   * @return parsed node
   * @throws JsonProcessingException if parsing fails
   */
  public static JsonNode readAsJsonNode (String aString)
    throws JsonProcessingException {

    return OBJECT_MAPPER.readTree(aString);
  }

  /**
   * Reads JSON from an input stream into a {@link JsonNode}.
   *
   * @param inputStream stream containing JSON
   * @return parsed node
   * @throws IOException if reading or parsing fails
   */
  public static JsonNode readAsJsonNode (InputStream inputStream)
    throws IOException {

    return OBJECT_MAPPER.readTree(inputStream);
  }

  /**
   * Deserializes JSON bytes into the requested type.
   *
   * @param bytes JSON payload
   * @param clazz target class
   * @param <T>   target type
   * @return deserialized object
   * @throws IOException if parsing fails
   */
  public static <T> T read (byte[] bytes, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(bytes, clazz);
  }

  /**
   * Deserializes a subset of JSON bytes into the requested type.
   *
   * @param bytes  JSON payload
   * @param offset offset to start reading
   * @param len    length to read
   * @param clazz  target class
   * @param <T>    target type
   * @return deserialized object
   * @throws IOException if parsing fails
   */
  public static <T> T read (byte[] bytes, int offset, int len, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(bytes, offset, len, clazz);
  }

  /**
   * Deserializes a JSON string into the requested type.
   *
   * @param aString JSON payload
   * @param clazz   target class
   * @param <T>     target type
   * @return deserialized object
   * @throws JsonProcessingException if parsing fails
   */
  public static <T> T read (String aString, Class<T> clazz)
    throws JsonProcessingException {

    return OBJECT_MAPPER.readValue(aString, clazz);
  }

  /**
   * Deserializes JSON from an input stream into the requested type.
   *
   * @param inputStream JSON stream
   * @param clazz       target class
   * @param <T>         target type
   * @return deserialized object
   * @throws IOException if parsing fails
   */
  public static <T> T read (InputStream inputStream, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(inputStream, clazz);
  }

  /**
   * Deserializes JSON using an existing {@link JsonParser}.
   *
   * @param parser parser positioned at the value
   * @param clazz  target class
   * @param <T>    target type
   * @return deserialized object
   * @throws IOException if parsing fails
   */
  public static <T> T read (JsonParser parser, Class<T> clazz)
    throws IOException {

    return OBJECT_MAPPER.readValue(parser, clazz);
  }

  /**
   * Converts a JSON node into a POJO of the requested type.
   *
   * @param node  source node
   * @param clazz target class
   * @param <T>   target type
   * @return converted value
   * @throws JsonProcessingException if conversion fails
   */
  public static <T> T read (JsonNode node, Class<T> clazz)
    throws JsonProcessingException {

    return OBJECT_MAPPER.treeToValue(node, clazz);
  }

  /**
   * Converts a POJO into a Jackson tree node.
   *
   * @param obj source object
   * @return JSON node representation
   */
  public static JsonNode writeAsJsonNode (Object obj) {

    return OBJECT_MAPPER.valueToTree(obj);
  }

  /**
   * Serializes a POJO to JSON bytes.
   *
   * @param obj object to serialize
   * @return byte representation
   * @throws JsonProcessingException if serialization fails
   */
  public static byte[] writeAsBytes (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.writeValueAsBytes(obj);
  }

  /**
   * Serializes a POJO to a compact JSON string.
   *
   * @param obj object to serialize
   * @return JSON string
   * @throws JsonProcessingException if serialization fails
   */
  public static String writeAsString (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.writeValueAsString(obj);
  }

  /**
   * Serializes a POJO to a pretty-printed JSON string with sorted object fields.
   *
   * @param obj object to serialize
   * @return formatted JSON string
   * @throws JsonProcessingException if serialization fails
   */
  public static String writeAsPrettyPrintedString (Object obj)
    throws JsonProcessingException {

    return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(sort(OBJECT_MAPPER.valueToTree(obj)));
  }

  /**
   * Writes a POJO as JSON directly to an output stream.
   *
   * @param outputStream destination stream
   * @param obj          object to serialize
   * @throws IOException if writing or serialization fails
   */
  public static void writeToStream (OutputStream outputStream, Object obj)
    throws IOException {

    OBJECT_MAPPER.writeValue(outputStream, obj);
  }

  /**
   * Converts an object to another type using Jackson's data binding.
   *
   * @param obj   source object
   * @param clazz target class
   * @param <T>   target type
   * @return converted value
   */
  public static <T> T convert (Object obj, Class<T> clazz) {

    return OBJECT_MAPPER.convertValue(obj, clazz);
  }

  /**
   * Deep-copies a JSON node, cloning object and array structures.
   *
   * @param node node to copy
   * @return copied node (or {@code null} if input is null)
   */
  public static JsonNode copy (JsonNode node) {

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

  /**
   * Recursively sorts object field names using an alphanumeric comparator.
   *
   * @param node node to sort
   * @return sorted node (new tree for objects, original for other types)
   */
  private static JsonNode sort (JsonNode node) {

    if (node == null) {

      return null;
    } else if (node.isObject()) {

      ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
      LinkedList<String> sortedFieldNameList = new LinkedList<>();

      for (String fieldName : new IterableIterator<>(node.fieldNames())) {
        sortedFieldNameList.add(fieldName);
      }

      sortedFieldNameList.sort(ALPHA_NUMERIC_COMPARATOR);

      for (String sortedFieldName : sortedFieldNameList) {
        objectNode.set(sortedFieldName, sort(node.get(sortedFieldName)));
      }

      return objectNode;
    } else {

      return node;
    }
  }
}
