/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.email;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class Postman {

  private final HashMap<SHA256Key, Template> templateMap = new HashMap<>();
  private Session session;
  private Configuration freemarkerConf;

  public Postman () {

  }

  public Postman (String host, int port) {

    this(host, port, Authentication.NONE, false);
  }

  public Postman (String host, int port, Authentication authentication) {

    this(host, port, authentication, false);
  }

  public Postman (String host, int port, boolean secure) {

    this(host, port, Authentication.NONE, secure);
  }

  public Postman (String host, int port, Authentication authentication, boolean secure) {

    session = (!secure) ? Protocol.SMTP.getSession(host, port, authentication) : Protocol.SMTPS.getSession(host, port, authentication);
    freemarkerConf = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
  }

  public void send (Mail mail)
    throws MailDeliveryException {

    send(mail, null);
  }

  public void send (Mail mail, HashMap<String, Object> interpolationMap)
    throws MailDeliveryException {

    MimeMessage message = new MimeMessage(session);
    Multipart multipart = new MimeMultipart();

    try {
      message.setFrom(new InternetAddress(mail.getFrom().strip()));

      if (mail.getReplyTo() != null) {
        message.setReplyTo(new Address[] {new InternetAddress(mail.getReplyTo())});
      }
      if (mail.getTo() != null) {
        addRecipients(message, Message.RecipientType.TO, mail.getTo());
      }
      if (mail.getCc() != null) {
        addRecipients(message, Message.RecipientType.CC, mail.getCc());
      }
      if (mail.getBcc() != null) {
        addRecipients(message, Message.RecipientType.BCC, mail.getBcc());
      }

      message.setSentDate(new Date());

      if (mail.getSubject() != null) {
        message.setSubject(mail.getSubject(), StandardCharsets.UTF_8.name());
      }

      if (mail.getBodyReader() != null) {

        CharArrayWriter bodyWriter;
        char[] buffer;
        int charsRead;

        buffer = new char[256];
        bodyWriter = new CharArrayWriter();
        while ((charsRead = mail.getBodyReader().read(buffer)) >= 0) {
          bodyWriter.write(buffer, 0, charsRead);
        }
        mail.getBodyReader().close();

        MimeBodyPart textPart = new MimeBodyPart();

        if (interpolationMap == null) {
          textPart.setText(bodyWriter.toString(), StandardCharsets.UTF_8.name(), mail.isHtml() ? "html" : "plain");
        } else {

          Template template;
          StringWriter templateWriter;
          SHA256Key sha256Key = new SHA256Key(EncryptionUtility.hash(HashAlgorithm.SHA_256, bodyWriter.toString().getBytes(StandardCharsets.UTF_8)));

          synchronized (templateMap) {
            if ((template = templateMap.get(sha256Key)) == null) {
              templateMap.put(sha256Key, template = new Template(new String(sha256Key.getHash(), StandardCharsets.UTF_8), new CharArrayReader(bodyWriter.toCharArray()), freemarkerConf));
            }
          }

          template.process(interpolationMap, templateWriter = new StringWriter());
          textPart.setText(templateWriter.toString(), StandardCharsets.UTF_8.name(), mail.isHtml() ? "html" : "plain");
        }

        multipart.addBodyPart(textPart);
      }

      if ((mail.getAttachments() != null) && (mail.getAttachments().length > 0)) {
        for (DataSource attachment : mail.getAttachments()) {

          MimeBodyPart filePart = new MimeBodyPart();

          filePart.setDataHandler(new DataHandler(attachment));
          filePart.setFileName(attachment.getName());
          multipart.addBodyPart(filePart);
        }
      }

      message.setContent(multipart);
      Transport.send(message);
    } catch (Exception exception) {
      throw new MailDeliveryException(exception);
    }
  }

  private void addRecipients (Message message, Message.RecipientType type, String addresses)
    throws MessagingException {

    for (String address : addresses.split(",")) {
      message.addRecipient(type, new InternetAddress(address.strip()));
    }
  }

  private static class SHA256Key {

    private final byte[] hash;

    public SHA256Key (byte[] hash) {

      this.hash = hash;
    }

    public byte[] getHash () {

      return hash;
    }

    @Override
    public int hashCode () {

      return Arrays.hashCode(hash);
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof SHA256Key) && Arrays.equals(hash, ((SHA256Key)obj).getHash());
    }
  }
}
