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
package org.smallmind.nutsnbolts.security;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.PublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.spec.OpenSSHPublicKeySpec;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.smallmind.nutsnbolts.http.Base64Codec;

public enum AsymmetricKeySpec {

  OPENSSH {
    @Override
    public String fromKey (Key key)
      throws IOException, InappropriateKeySpecException {

      if (key instanceof PublicKey) {

        String keyAlgorithm;
        String sshCode = "EdDSA".equals(keyAlgorithm = key.getAlgorithm()) ? "ed25519" : keyAlgorithm.toLowerCase();

        return "ssh-" + sshCode + " " + Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(PublicKeyFactory.createKey(key.getEncoded())));
      } else {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      }
    }

    @Override
    public KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {

        int lastSpacePos;

        if (raw.startsWith("ssh-")) {

          int firstSpacePos;

          if ((firstSpacePos = raw.indexOf(' ')) <= 0) {
            throw new IOException("The raw key requires a space separator after the ssh prologue");
          } else {
            raw = raw.substring(firstSpacePos + 1);
          }
        }

        // remove the email address comment at the tail of the ssh key
        if ((lastSpacePos = raw.lastIndexOf(' ')) >= 0) {
          raw = raw.substring(0, lastSpacePos);
        }

        return new OpenSSHPublicKeySpec(Base64Codec.decode(raw));
      } else {
        throw new InappropriateKeySpecException(type.name());
      }
    }
  },
  PKCS8 {
    @Override
    public String fromKey (Key key)
      throws IOException, InappropriateKeySpecException {

      if (key instanceof PublicKey) {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      } else {

        StringWriter stringWriter;
        PemWriter pemWriter = new PemWriter(stringWriter = new StringWriter());

        pemWriter.writeObject(new PemObject("PRIVATE KEY", key.getEncoded()));
        pemWriter.flush();

        return stringWriter.getBuffer().toString();
      }
    }

    @Override
    public KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {
        throw new InappropriateKeySpecException(type.name());
      } else {

        Matcher prologMatcher;
        Matcher epilogMatcher;
        int start = 0;
        int end = raw.length();

        raw = raw.strip();

        if ((prologMatcher = PKCS8_PROLOG_PATTERN.matcher(raw)).find()) {
          start = prologMatcher.end();
        }
        if ((epilogMatcher = PKCS8_EPILOG_PATTERN.matcher(raw)).find()) {
          end = epilogMatcher.start();
        }

        return new PKCS8EncodedKeySpec(new PemReader(new StringReader("-----BEGIN PRIVATE KEY-----\n" + raw.substring(start, end).replaceAll("\\s", "\n") + "\n-----END PRIVATE KEY-----")).readPemObject().getContent());
      }
    }
  },
  X509 {
    @Override
    public String fromKey (Key key)
      throws InappropriateKeySpecException {

      if (key instanceof PublicKey) {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      } else {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      }
    }

    @Override
    public KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {
        raw = raw.strip();

        if (raw.startsWith("-----")) {

          int lineReturnPos;

          if ((lineReturnPos = raw.indexOf('\n')) < 0) {
            throw new IOException("The raw key requires a line separator after the ssh prologue");
          } else {
            raw = raw.substring(lineReturnPos + 1);
          }
        }
        if (raw.endsWith("-----")) {

          int lastLineReturnPos;

          if ((lastLineReturnPos = raw.lastIndexOf('\n')) < 0) {
            throw new IOException("The raw key requires a line separator before the ssh epilogue");
          } else {
            raw = raw.substring(0, lastLineReturnPos);
          }
        }

        return new X509EncodedKeySpec(Base64Codec.decode(raw.replaceAll("\\s", "")));
      } else {
        throw new InappropriateKeySpecException(type.name());
      }
    }
  };

  private static final Pattern PKCS8_PROLOG_PATTERN = Pattern.compile("^-----BEGIN PRIVATE KEY-----\\s+");
  private static final Pattern PKCS8_EPILOG_PATTERN = Pattern.compile("\\s+-----END PRIVATE KEY-----$");

  public abstract KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
    throws IOException, InappropriateKeySpecException;

  public abstract String fromKey (Key key)
    throws IOException, InappropriateKeySpecException;
}
