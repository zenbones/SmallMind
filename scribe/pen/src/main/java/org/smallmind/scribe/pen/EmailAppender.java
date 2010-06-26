package org.smallmind.scribe.pen;

import org.smallmind.nutsnbolts.email.Mail;
import org.smallmind.nutsnbolts.email.SendMail;

public class EmailAppender extends AbstractAppender {

   private SendMail sendMail;
   private String from;
   private String to;
   private String subject;

   public EmailAppender (String smtpServer, int smtpPort) {

      super();

      sendMail = new SendMail(smtpServer, smtpPort);
   }

   public EmailAppender (String smtpServer, int smtpPort, String from, String to, String subject) {

      this(smtpServer, smtpPort);

      this.from = from;
      this.to = to;

      setSubject(subject);
   }

   public void setFrom (String from) {

      this.from = from;
   }

   public void setTo (String to) {

      this.to = to;
   }

   public void setSubject (String subject) {

      this.subject = subject;
   }

   public void handleOutput (String output)
      throws Exception {

      sendMail.smtp(new Mail(from, to, subject, output));
   }

   public boolean requiresFormatter () {

      return true;
   }
}