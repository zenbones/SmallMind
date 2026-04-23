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
 * Enumeration of supported asymmetric key encoding formats with helpers for serializing keys and constructing {@link java.security.spec.KeySpec} instances from raw text.
 */
public enum AsymmetricKeySpec {

  OPENSSH {
    /**
     * Serializes a public key to the OpenSSH {@code authorized_keys} single-line format, or a private key to the OpenSSH PEM block format.
     *
     * @param key the public or private key to serialize
     * @return an OpenSSH-formatted key string
     * @throws IOException                   if the key bytes cannot be encoded
     * @throws InappropriateKeySpecException if the key type is not supported by this encoding
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
     * Parses raw OpenSSH key text and returns the appropriate {@link KeySpec} for the requested key type.
     * For {@link AsymmetricKeyType#PUBLIC} keys the text may include the {@code ssh-<algo>} prefix and trailing comment.
     * For private keys the text may include or omit the OpenSSH PEM header and footer.
     *
     * @param type whether to construct a public or private key spec
     * @param raw  the raw OpenSSH key text, with or without header/footer and comment
     * @return a {@link KeySpec} suitable for use with a {@link java.security.KeyFactory}
     * @throws IOException                   if the raw text is malformed or cannot be decoded
     * @throws InappropriateKeySpecException if the requested key type is not supported by this encoding
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
     * Serializes a private key to a PKCS8 PEM block ({@code -----BEGIN PRIVATE KEY-----}).
     * Throws {@link InappropriateKeySpecException} if a public key is supplied.
     *
     * @param key the private key to serialize
     * @return the PEM-encoded private key string
     * @throws IOException                   if PEM encoding fails
     * @throws InappropriateKeySpecException if a public key is passed instead of a private key
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
     * Parses PKCS8 PEM text and returns a {@link PKCS8EncodedKeySpec} for constructing a private key.
     * Throws {@link InappropriateKeySpecException} if {@link AsymmetricKeyType#PUBLIC} is requested.
     *
     * @param type must be {@link AsymmetricKeyType#PRIVATE}; public keys are not supported by PKCS8
     * @param raw  the PEM-encoded private key text, with or without the {@code -----BEGIN/END PRIVATE KEY-----} headers
     * @return a {@link PKCS8EncodedKeySpec} suitable for use with a {@link java.security.KeyFactory}
     * @throws IOException                   if the raw text is malformed or cannot be decoded
     * @throws InappropriateKeySpecException if {@link AsymmetricKeyType#PUBLIC} is requested
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
     * Serialization to X.509 format is not supported; always throws {@link InappropriateKeySpecException}.
     *
     * @param key the key (ignored)
     * @return never returns normally
     * @throws InappropriateKeySpecException always, because X.509 key serialization is not implemented
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
     * Parses X.509 / SubjectPublicKeyInfo encoded text and returns an {@link X509EncodedKeySpec} for constructing a public key.
     * The raw text may optionally include PEM {@code -----BEGIN/END PUBLIC KEY-----} headers.
     * Throws {@link InappropriateKeySpecException} if {@link AsymmetricKeyType#PRIVATE} is requested.
     *
     * @param type must be {@link AsymmetricKeyType#PUBLIC}; private keys are not supported by this encoding
     * @param raw  the Base64 or PEM-encoded public key text
     * @return an {@link X509EncodedKeySpec} suitable for use with a {@link java.security.KeyFactory}
     * @throws IOException                   if the raw text is malformed or cannot be decoded
     * @throws InappropriateKeySpecException if {@link AsymmetricKeyType#PRIVATE} is requested
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
   * Parses raw key text in this encoding format and returns a {@link java.security.spec.KeySpec} for the requested key type.
   *
   * @param type whether a public or private key spec should be constructed
   * @param raw  the encoded key text in this format's representation
   * @return a {@link java.security.spec.KeySpec} suitable for use with {@link java.security.KeyFactory}
   * @throws IOException                   if the raw text cannot be parsed or decoded
   * @throws InappropriateKeySpecException if this encoding does not support the requested key type
   */
  public abstract KeySpec constructKeySpec (AsymmetricKeyType type, String raw)
    throws IOException, InappropriateKeySpecException;

  /**
   * Serializes the given key into this encoding format's textual representation.
   *
   * @param key the public or private key to serialize
   * @return the encoded key text
   * @throws IOException                   if the key cannot be encoded
   * @throws InappropriateKeySpecException if the key type is not supported by this encoding
   */
  public abstract String fromKey (Key key)
    throws IOException, InappropriateKeySpecException;
}
