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
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.spec.OpenSSHPrivateKeySpec;
import org.bouncycastle.jcajce.spec.OpenSSHPublicKeySpec;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.smallmind.nutsnbolts.http.Base64Codec;

/**
 * Supported encodings for asymmetric keys along with helpers to serialize and construct {@link java.security.spec.KeySpec}s.
 */
public enum AsymmetricKeySpec {

  OPENSSH {
    /**
     * Encodes a public/private key in the OpenSSH authorized_keys format.
     *
     * @param key the key to encode
     * @return an OpenSSH formatted key string
     * @throws IOException if the key bytes cannot be encoded
     * @throws InappropriateKeySpecException if the key type is unsupported
     */
    @Override
    public String fromKey (Key key)
      throws IOException, InappropriateKeySpecException {

      if (key instanceof PublicKey) {

        String keyAlgorithm;
        String sshCode = "EdDSA".equals(keyAlgorithm = key.getAlgorithm()) ? "ed25519" : keyAlgorithm.toLowerCase();

        return "ssh-" + sshCode + " " + Base64.getEncoder().encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(PublicKeyFactory.createKey(key.getEncoded())));
      } else {

        return "-----BEGIN OPENSSH PRIVATE KEY-----\n" + EncryptionUtility.convertToBlock(Base64.getEncoder().encodeToString(OpenSSHPrivateKeyUtil.encodePrivateKey(PrivateKeyFactory.createKey(key.getEncoded()))), 64) + "\n-----END OPENSSH PRIVATE KEY-----";
      }
    }

    /**
     * Builds an {@link OpenSSHPublicKeySpec} from an SSH or Base64 encoded public/private key string.
     *
     * @param type must be {@link AsymmetricKeyType#PUBLIC}
     * @param raw the raw key text, optionally including SSH prologue/epilogue
     * @return a key spec suitable for generating a public key
     * @throws IOException if the raw text is malformed
     * @throws InappropriateKeySpecException if the key type does not match
     */
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

        Matcher prologMatcher;
        Matcher epilogMatcher;
        int start = 0;
        int end;

        raw = raw.strip();
        end = raw.length();

        if ((prologMatcher = OPENSSH_PROLOG_PATTERN.matcher(raw)).find()) {
          start = prologMatcher.end();
        }
        if ((epilogMatcher = OPENSSH_EPILOG_PATTERN.matcher(raw)).find(start)) {
          end = epilogMatcher.start();
        }

        return new OpenSSHPrivateKeySpec(Base64Codec.decode(raw.substring(start, end).replaceAll("\\s", "")));
      }
    }
  },
  PKCS8 {
    /**
     * Encodes a private key to a PKCS8 PEM block.
     *
     * @param key the private key
     * @return the PEM formatted key
     * @throws IOException if encoding fails
     * @throws InappropriateKeySpecException if the key is not a private key
     */
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

    /**
     * Parses a PKCS8 PEM string and builds a {@link PKCS8EncodedKeySpec}.
     *
     * @param type must be {@link AsymmetricKeyType#PRIVATE}
     * @param raw PEM encoded private key, with or without headers
     * @return the constructed key spec
     * @throws IOException if the raw text is malformed
     * @throws InappropriateKeySpecException if the key type does not match
     */
    @Override
    public KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {
        throw new InappropriateKeySpecException(type.name());
      } else {

        Matcher prologMatcher;
        Matcher epilogMatcher;
        int start = 0;
        int end;

        raw = raw.strip();
        end = raw.length();

        if ((prologMatcher = PKCS8_PROLOG_PATTERN.matcher(raw)).find()) {
          start = prologMatcher.end();
        }
        if ((epilogMatcher = PKCS8_EPILOG_PATTERN.matcher(raw)).find(start)) {
          end = epilogMatcher.start();
        }

        return new PKCS8EncodedKeySpec(new PemReader(new StringReader("-----BEGIN PRIVATE KEY-----\n" + raw.substring(start, end).replaceAll("\\s", "\n") + "\n-----END PRIVATE KEY-----")).readPemObject().getContent());
      }
    }
  },
  X509 {
    /**
     * X509 public key encoding is not supported for serialization by this enum.
     *
     * @param key the key to encode
     * @return never returns normally
     * @throws InappropriateKeySpecException always thrown for unsupported key type
     */
    @Override
    public String fromKey (Key key)
      throws InappropriateKeySpecException {

      if (key instanceof PublicKey) {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      } else {
        throw new InappropriateKeySpecException(key.getAlgorithm());
      }
    }

    /**
     * Parses an X509 public key from PEM or Base64 and builds an {@link X509EncodedKeySpec}.
     *
     * @param type must be {@link AsymmetricKeyType#PUBLIC}
     * @param raw the encoded public key text
     * @return the constructed key spec
     * @throws IOException if the raw text is malformed
     * @throws InappropriateKeySpecException if the key type does not match
     */
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
  private static final Pattern OPENSSH_PROLOG_PATTERN = Pattern.compile("^-----BEGIN OPENSSH PRIVATE KEY-----\\s+");
  private static final Pattern OPENSSH_EPILOG_PATTERN = Pattern.compile("\\s+-----END OPENSSH PRIVATE KEY-----$");

  /**
   * Builds a {@link KeySpec} for the provided encoding and key type from raw text.
   *
   * @param type whether a public or private key is expected
   * @param raw  the encoded key text
   * @return a {@link KeySpec} suitable for {@link java.security.KeyFactory}
   * @throws IOException                   if the raw text cannot be parsed
   * @throws InappropriateKeySpecException if the requested spec is not compatible with the key type
   */
  public abstract KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
    throws IOException, InappropriateKeySpecException;

  /**
   * Serializes a key into the current encoding format.
   *
   * @param key the key to serialize
   * @return the encoded key text
   * @throws IOException                   if the key cannot be encoded
   * @throws InappropriateKeySpecException if the key type is unsupported
   */
  public abstract String fromKey (Key key)
    throws IOException, InappropriateKeySpecException;
}
