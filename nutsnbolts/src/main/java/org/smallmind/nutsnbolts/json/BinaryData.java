/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.security.Key;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name = "binary")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class BinaryData implements Serializable {

  private Encoding encoding;
  private Encryption encryption;
  private String contentType;
  private String data;

  public BinaryData () {

  }

  public BinaryData (Encoding encoding, byte[] bytes)
    throws Exception {

    this.encoding = encoding;

    data = encoding.encode(bytes);
  }

  public BinaryData (String contentType, Encoding encoding, byte[] bytes)
    throws Exception {

    this(encoding, bytes);

    this.contentType = contentType;
  }

  public BinaryData (Encoding encoding, Encryption encryption, Key key, String original)
    throws Exception {

    this.encoding = encoding;
    this.encryption = encryption;

    data = encoding.encode(encryption.encrypt(key, original.getBytes()));
  }

  public BinaryData (String contentType, Encoding encoding, Encryption encryption, Key key, String original)
    throws Exception {

    this(encoding, encryption, key, original);

    this.contentType = contentType;
  }

  public byte[] decode ()
    throws Exception {

    return encoding.decode(data);
  }

  public byte[] decrypt (Key key)
    throws Exception {

    if (encryption == null) {
      throw new IllegalStateException("This binary data has not been encrypted");
    }

    return encryption.decrypt(key, decode());
  }

  @XmlTransient
  public boolean isEncrypted () {

    return encryption != null;
  }

  @XmlElement(name = "encoding", required = true)
  public Encoding getEncoding () {

    return encoding;
  }

  public void setEncoding (Encoding encoding) {

    this.encoding = encoding;
  }

  @XmlElement(name = "encryption")
  public Encryption getEncryption () {

    return encryption;
  }

  public void setEncryption (Encryption encryption) {

    this.encryption = encryption;
  }

  @XmlElement(name = "content_type")
  public String getContentType () {

    return contentType;
  }

  public void setContentType (String contentType) {

    this.contentType = contentType;
  }

  @XmlElement(name = "data", required = true)
  public String getData () {

    return data;
  }

  public void setData (String data) {

    this.data = data;
  }
}