package org.smallmind.nutsnbolts.security;

public interface Decryptor {

  byte[] decrypt (byte[] encrypted)
    throws Exception;
}
