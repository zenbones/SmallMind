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
package org.smallmind.nutsnbolts.json;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * JAXB-ready representation of binary content that can be encoded and optionally encrypted for transport.
 * Supports round-tripping byte arrays via the configured {@link Encoding} (and {@link Encryption}, if present).
 */
@XmlRootElement(name = "binary", namespace = "http://org.smallmind/nutsnbolts/json")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class BinaryData implements Serializable {

  private Encoding encoding;
  private Encryption encryption;
  private String name;
  private String contentType;
  private String data;

  /**
   * Creates an empty instance for frameworks that require a no-arg constructor.
   */
  public BinaryData () {

  }

  /**
   * Creates encoded binary content.
   *
   * @param encoding the codec to convert raw bytes into a string
   * @param bytes    the raw bytes to encode
   * @throws Exception if the encoding operation fails
   */
  public BinaryData (Encoding encoding, byte[] bytes)
    throws Exception {

    this.encoding = encoding;

    data = encoding.encode(bytes);
  }

  /**
   * Creates encoded binary content and records the associated content type.
   *
   * @param contentType MIME-style content type describing the payload
   * @param encoding    the codec to convert raw bytes into a string
   * @param bytes       the raw bytes to encode
   * @throws Exception if the encoding operation fails
   */
  public BinaryData (String contentType, Encoding encoding, byte[] bytes)
    throws Exception {

    this(encoding, bytes);

    this.contentType = contentType;
  }

  /**
   * Creates encoded content that is first encrypted with the supplied key.
   *
   * @param encoding   the codec to convert encrypted bytes to a string
   * @param encryption the cipher to apply to the payload
   * @param key        the key used for encryption/decryption
   * @param original   the clear-text payload
   * @throws Exception if encryption or encoding fails
   */
  public BinaryData (Encoding encoding, Encryption encryption, Key key, String original)
    throws Exception {

    this.encoding = encoding;
    this.encryption = encryption;

    data = encoding.encode(encryption.encrypt(key, original.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Decodes the stored string into its original bytes using the configured {@link Encoding}.
   *
   * @return the decoded bytes
   * @throws Exception if decoding fails
   */
  public byte[] decode ()
    throws Exception {

    return encoding.decode(data);
  }

  /**
   * Decrypts the stored payload using the configured {@link Encryption}.
   *
   * @param key the decryption key corresponding to the encryption used
   * @return the decrypted bytes
   * @throws Exception if decryption fails or no encryption was configured
   */
  public byte[] decrypt (Key key)
    throws Exception {

    if (encryption == null) {
      throw new IllegalStateException("This binary data has not been encrypted");
    }

    return encryption.decrypt(key, decode());
  }

  /**
   * Indicates whether this payload has been encrypted.
   *
   * @return {@code true} when an {@link Encryption} has been applied
   */
  @XmlTransient
  public boolean isEncrypted () {

    return encryption != null;
  }

  /**
   * Returns the encoding used to serialize the binary payload.
   *
   * @return the encoding
   */
  @XmlElement(name = "encoding", required = true)
  public Encoding getEncoding () {

    return encoding;
  }

  /**
   * Sets the encoding used to serialize the binary payload.
   *
   * @param encoding the encoding strategy
   */
  public void setEncoding (Encoding encoding) {

    this.encoding = encoding;
  }

  /**
   * Returns the encryption algorithm applied to the payload, if any.
   *
   * @return the encryption algorithm or {@code null} if unencrypted
   */
  @XmlElement(name = "encryption")
  public Encryption getEncryption () {

    return encryption;
  }

  /**
   * Sets the encryption metadata for this instance.
   *
   * @param encryption the encryption algorithm that secured the payload
   */
  public void setEncryption (Encryption encryption) {

    this.encryption = encryption;
  }

  /**
   * Returns the logical name associated with the payload.
   *
   * @return payload name, possibly {@code null}
   */
  @XmlElement(name = "name")
  public String getName () {

    return name;
  }

  /**
   * Assigns a logical name to the payload.
   *
   * @param name a human readable identifier
   * @return this instance for chaining
   */
  public BinaryData setName (String name) {

    this.name = name;

    return this;
  }

  /**
   * Returns the MIME-style content type of the payload.
   *
   * @return content type, possibly {@code null}
   */
  @XmlElement(name = "contentType")
  public String getContentType () {

    return contentType;
  }

  /**
   * Sets the MIME-style content type of the payload.
   *
   * @param contentType payload content type
   * @return this instance for chaining
   */
  public BinaryData setContentType (String contentType) {

    this.contentType = contentType;

    return this;
  }

  /**
   * Returns the encoded payload string.
   *
   * @return encoded payload
   */
  @XmlElement(name = "data", required = true)
  public String getData () {

    return data;
  }

  /**
   * Sets the encoded payload string.
   *
   * @param data encoded payload
   */
  public void setData (String data) {

    this.data = data;
  }
}
