/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.email;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import org.smallmind.nutsnbolts.http.Base64;

public class SendMail {

   private String smtpServer;
   private int smtpPort;

   public SendMail (String smtpServer, int smtpPort) {

      this.smtpServer = smtpServer;
      this.smtpPort = smtpPort;
   }

   public void smtp (Mail outMail)
      throws IOException, SMTPMailException {

      smtp(null, outMail, false);
   }

   public void smtp (Authentication authentication, Mail outMail)
      throws IOException, SMTPMailException {

      smtp(authentication, outMail, false);
   }

   public void smtp (Mail outMail, boolean asHtml)
      throws IOException, SMTPMailException {

      smtp(null, outMail, asHtml);
   }

   public void smtp (Authentication authentication, Mail outMail, boolean asHtml)
      throws IOException, SMTPMailException {

      Socket smtpSocket;
      InetAddress smtpServerInet = InetAddress.getByName(smtpServer);
      BufferedInputStream smtpIn;
      DataOutputStream smtpOut;
      String hostMachine = InetAddress.getLocalHost().getCanonicalHostName();

      smtpSocket = new Socket(smtpServerInet, smtpPort);
      smtpIn = new BufferedInputStream(smtpSocket.getInputStream());
      smtpOut = new DataOutputStream(new BufferedOutputStream(smtpSocket.getOutputStream()));
      checkReply(smtpIn, 220, "Server Handshake");
      checkConnection(hostMachine, smtpIn, smtpOut);

      if (authentication != null) {
         authenticate(authentication, smtpIn, smtpOut);
      }

      send(smtpIn, smtpOut, outMail, asHtml);
      smtpSocket.close();
   }

   private void checkConnection (String hostMachine, BufferedInputStream smtpIn, DataOutputStream smtpOut)
      throws IOException, SMTPMailException {

      writeSocket(smtpOut, "HELO " + hostMachine);
      checkReply(smtpIn, 250, "Hello");
   }

   private void authenticate (Authentication authentication, BufferedInputStream smtpIn, DataOutputStream smtpOut)
      throws IOException, SMTPMailException {

      if (!authentication.getAuthType().equals(Authentication.AuthType.LOGIN)) {
         throw new SMTPMailException("Unknown authentication type requested (%s)", authentication.getAuthType());
      }

      writeSocket(smtpOut, "AUTH LOGIN");
      checkReply(smtpIn, 334, "Auth");
      writeSocket(smtpOut, Base64.encode(authentication.getUser()));
      checkReply(smtpIn, 334, "Username");
      writeSocket(smtpOut, Base64.encode(authentication.getPassword()));
      checkReply(smtpIn, 235, "Password");
   }

   private void send (BufferedInputStream smtpIn, DataOutputStream smtpOut, Mail outMail, boolean asHtml)
      throws IOException, SMTPMailException {

      StringBuilder everyone;
      StringTokenizer everyoneTokenizer;
      String everyoneToken;
      String from = outMail.getFrom();
      String header;
      String text;

      everyone = new StringBuilder(outMail.getTo());
      if ((header = outMail.getCc()) != null) {
         everyone.append(",");
         everyone.append(header);
      }
      if ((header = outMail.getBcc()) != null) {
         everyone.append(",");
         everyone.append(header);
      }
      writeSocket(smtpOut, "MAIL FROM: " + from);
      checkReply(smtpIn, 250, "Mail From");

      everyoneTokenizer = new StringTokenizer(everyone.toString(), ",");
      while (everyoneTokenizer.hasMoreTokens()) {
         everyoneToken = everyoneTokenizer.nextToken();
         writeSocket(smtpOut, "RCPT TO: " + everyoneToken);
         checkReply(smtpIn, 250, "Rcpt To");
      }

      writeSocket(smtpOut, "DATA");
      checkReply(smtpIn, 354, "Data");
      writeSocket(smtpOut, "From: " + from);

      if ((header = outMail.getReplyTo()) != null) {
         writeSocket(smtpOut, "Reply-To: " + header);
      }
      writeSocket(smtpOut, "To: " + outMail.getTo());
      if ((header = outMail.getCc()) != null) {
         writeSocket(smtpOut, "cc: " + header);
      }
      if ((header = outMail.getBcc()) != null) {
         writeSocket(smtpOut, "bcc: " + header);
      }
      if ((header = outMail.getSubject()) != null) {
         writeSocket(smtpOut, "Subject: " + header);
      }
      if ((header = outMail.getDate()) != null) {

         writeSocket(smtpOut, "Date: " + header);
      }
      if (asHtml) {
         writeSocket(smtpOut, "Content-Type: text/html; charset=\"us-ascii\"");
      }

      writeSocket(smtpOut, "");
      if ((text = outMail.getText()) != null) {
         writeSocket(smtpOut, text);
      }
      writeSocket(smtpOut, ".");
      checkReply(smtpIn, 250, "Text End and Send");
   }

   private void checkReply (BufferedInputStream smtpIn, int safeCode, String action)
      throws IOException, SMTPMailException {

      String smtpReturn;

      smtpReturn = readSocket(smtpIn);
      if (!smtpReturn.startsWith(String.valueOf(safeCode) + " ")) {
         throw new SMTPMailException("Code %d not returned on %s\nSMTP reply was: %s", safeCode, action, smtpReturn);
      }
   }

   private String readSocket (BufferedInputStream smtpIn)
      throws IOException {

      byte[] buf;
      int bytesRead;

      buf = new byte[1024];
      bytesRead = smtpIn.read(buf);
      return new String(buf, 0, bytesRead);
   }

   private void writeSocket (DataOutputStream smtpOut, String singleLine)
      throws IOException {

      smtpOut.writeBytes(singleLine + "\r\n");
      smtpOut.flush();
   }

}

