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
   * Creates an empty instance for use by JAXB and other frameworks requiring a no-arg constructor.
   */
  public BinaryData () {

  }

  /**
   * Creates an instance by encoding the supplied bytes with the given {@link Encoding}.
   *
   * @param encoding the strategy used to convert raw bytes to a string representation
   * @param bytes    the raw bytes to encode
   * @throws Exception if the encoding operation fails
   */
  public BinaryData (Encoding encoding, byte[] bytes)
    throws Exception {

    this.encoding = encoding;

    data = encoding.encode(bytes);
  }

  /**
   * Creates an instance by encoding the supplied bytes and recording the associated MIME content type.
   *
   * @param contentType the MIME-style content type describing the payload
   * @param encoding    the strategy used to convert raw bytes to a string representation
   * @param bytes       the raw bytes to encode
   * @throws Exception if the encoding operation fails
   */
  public BinaryData (String contentType, Encoding encoding, byte[] bytes)
    throws Exception {

    this(encoding, bytes);

    this.contentType = contentType;
  }

  /**
   * Creates an instance by encrypting a string payload with the given key and then encoding the ciphertext.
   *
   * @param encoding   the strategy used to convert encrypted bytes to a string
   * @param encryption the cipher applied to the UTF-8 bytes of the original string
   * @param key        the key used for encryption
   * @param original   the clear-text string to encrypt and store
   * @throws Exception if encryption or encoding fails
   */
  public BinaryData (Encoding encoding, Encryption encryption, Key key, String original)
    throws Exception {

    this.encoding = encoding;
    this.encryption = encryption;

    data = encoding.encode(encryption.encrypt(key, original.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Decodes the stored string back into its original bytes using the configured {@link Encoding}.
   *
   * @return the decoded bytes
   * @throws Exception if decoding fails
   */
  public byte[] decode ()
    throws Exception {

    return encoding.decode(data);
  }

  /**
   * Decodes and then decrypts the stored payload using the configured {@link Encryption} and the supplied key.
   *
   * @param key the decryption key corresponding to the encryption used when this instance was constructed
   * @return the decrypted bytes
   * @throws IllegalStateException if no encryption was configured on this instance
   * @throws Exception             if decryption or decoding fails
   */
  public byte[] decrypt (Key key)
    throws Exception {

    if (encryption == null) {
      throw new IllegalStateException("This binary data has not been encrypted");
    }

    return encryption.decrypt(key, decode());
  }

  /**
   * Returns whether this payload was encrypted before storage.
   *
   * @return {@code true} when an {@link Encryption} cipher has been applied; {@code false} otherwise
   */
  @XmlTransient
  public boolean isEncrypted () {

    return encryption != null;
  }

  /**
   * Returns the encoding strategy used to represent the binary payload as a string.
   *
   * @return the encoding
   */
  @XmlElement(name = "encoding", required = true)
  public Encoding getEncoding () {

    return encoding;
  }

  /**
   * Sets the encoding strategy used to represent the binary payload as a string.
   *
   * @param encoding the encoding strategy
   */
  public void setEncoding (Encoding encoding) {

    this.encoding = encoding;
  }

  /**
   * Returns the encryption algorithm applied to this payload, or {@code null} if unencrypted.
   *
   * @return the encryption algorithm, or {@code null}
   */
  @XmlElement(name = "encryption")
  public Encryption getEncryption () {

    return encryption;
  }

  /**
   * Sets the encryption algorithm associated with this payload.
   *
   * @param encryption the encryption algorithm that was used to secure the payload
   */
  public void setEncryption (Encryption encryption) {

    this.encryption = encryption;
  }

  /**
   * Returns the logical name associated with this payload, or {@code null} if not set.
   *
   * @return the payload name
   */
  @XmlElement(name = "name")
  public String getName () {

    return name;
  }

  /**
   * Sets a logical name for this payload, returning this instance to allow method chaining.
   *
   * @param name a human-readable identifier for the payload
   * @return this instance
   */
  public BinaryData setName (String name) {

    this.name = name;

    return this;
  }

  /**
   * Returns the MIME-style content type of this payload, or {@code null} if not set.
   *
   * @return the content type
   */
  @XmlElement(name = "contentType")
  public String getContentType () {

    return contentType;
  }

  /**
   * Sets the MIME-style content type of this payload, returning this instance to allow method chaining.
   *
   * @param contentType the content type describing the payload
   * @return this instance
   */
  public BinaryData setContentType (String contentType) {

    this.contentType = contentType;

    return this;
  }

  /**
   * Returns the encoded (and possibly encrypted) payload as a string.
   *
   * @return the encoded payload string
   */
  @XmlElement(name = "data", required = true)
  public String getData () {

    return data;
  }

  /**
   * Sets the encoded payload string.
   *
   * @param data the encoded string representation of the binary payload
   */
  public void setData (String data) {

    this.data = data;
  }
}
