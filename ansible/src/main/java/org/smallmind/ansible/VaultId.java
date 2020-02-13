package org.smallmind.ansible;

import org.smallmind.nutsnbolts.command.CommandLineException;

public class VaultId {

  private String id;
  private String fileOrPrompt;

  public VaultId (String spec)
    throws CommandLineException {

    int atPos;

    if ((atPos = spec.indexOf('@')) < 0) {
      throw new CommandLineException("Unable to parse vault id(%s)", spec);
    } else {
      if ((id = spec.substring(0, atPos)).isEmpty()) {
        throw new CommandLineException("Missing identifier in vault id(%s)", spec);
      }
      if ((fileOrPrompt = spec.substring(atPos + 1)).isEmpty()) {
        throw new CommandLineException("Missing file or 'prompt' in vault id(%s)", spec);
      }
    }
  }

  public String getId () {

    return id;
  }

  public String getFileOrPrompt () {

    return fileOrPrompt;
  }
}
