package org.smallmind.web.jersey.util;

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

  @XmlElement(name = "encoding", required = true, nillable = false)
  public Encoding getEncoding () {

    return encoding;
  }

  public void setEncoding (Encoding encoding) {

    this.encoding = encoding;
  }

  @XmlElement(name = "encryption", required = false, nillable = false)
  public Encryption getEncryption () {

    return encryption;
  }

  public void setEncryption (Encryption encryption) {

    this.encryption = encryption;
  }

  @XmlElement(name = "contentType", required = false, nillable = false)
  public String getContentType () {

    return contentType;
  }

  public void setContentType (String contentType) {

    this.contentType = contentType;
  }

  @XmlElement(name = "data", required = true, nillable = false)
  public String getData () {

    return data;
  }

  public void setData (String data) {

    this.data = data;
  }
}