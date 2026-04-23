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
package org.smallmind.nutsnbolts.spring.property;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.yaml.snakeyaml.Yaml;

/**
 * Enumerates the supported property file formats and provides the corresponding {@link PropertyHandler} for each.
 */
public enum PropertyFileType {

  PROPERTIES(new String[] {"properties"}) {
    /**
     * Loads the given stream as a Java {@code .properties} file and returns a handler over its entries.
     *
     * @param inputStream the input stream of the properties file
     * @return a {@link PropertyHandler} over the loaded entries
     * @throws IOException if the stream cannot be read
     */
    @Override
    public PropertyHandler<?> getPropertyHandler (InputStream inputStream)
      throws IOException {

      Properties properties = new Properties();

      properties.load(inputStream);

      return new PropertiesPropertyHandler(properties);
    }
  },
  YAML(new String[] {"yaml", "yml"}) {
    /**
     * Parses the given stream as a YAML file and returns a handler that flattens its structure into property entries.
     *
     * @param inputStream the input stream of the YAML file
     * @return a {@link PropertyHandler} over the flattened entries
     */
    @Override
    public PropertyHandler<?> getPropertyHandler (InputStream inputStream) {

      Yaml yaml = new Yaml();

      return new YamlPropertyHandler(yaml.load(inputStream));
    }
  };
  private final String[] extensions;

  /**
   * Initializes the enum constant with the file extensions it recognizes.
   *
   * @param extensions file extensions (without leading dot) that map to this type
   */
  PropertyFileType (String[] extensions) {

    this.extensions = extensions;
  }

  /**
   * Returns the {@link PropertyFileType} whose recognized extensions include the given value, or {@code null} if none matches.
   *
   * @param extension a file extension without a leading dot
   * @return the matching type, or {@code null} if no type claims this extension
   */
  public static PropertyFileType forExtension (String extension) {

    for (PropertyFileType propertyFileType : PropertyFileType.values()) {
      for (String possibleExtension : propertyFileType.getExtensions()) {
        if (possibleExtension.equals(extension)) {

          return propertyFileType;
        }
      }
    }

    return null;
  }

  /**
   * Creates a {@link PropertyHandler} capable of reading this file format from the given input stream.
   *
   * @param inputStream the source input stream
   * @return a property handler for the entries in the stream
   * @throws IOException if the stream cannot be read
   */
  public abstract PropertyHandler<?> getPropertyHandler (InputStream inputStream)
    throws IOException;

  /**
   * Returns the file extensions recognized by this property file type.
   *
   * @return array of extension strings (without leading dot)
   */
  public String[] getExtensions () {

    return extensions;
  }
}
