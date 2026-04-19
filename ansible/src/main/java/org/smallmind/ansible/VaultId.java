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
package org.smallmind.ansible;

import org.smallmind.nutsnbolts.command.CommandLineException;

/**
 * Parsed representation of an Ansible {@code --vault-id} command-line argument.
 *
 * <p>Ansible accepts vault ids in the form {@code <label>@<source>}, where {@code <label>} is a
 * short identifier embedded in the vault 1.2 header and {@code <source>} is either a path to a
 * file that contains the password or the literal string {@code prompt}, which instructs tooling to
 * ask the user interactively.  This class parses that specification and exposes the two parts as
 * typed accessors.
 *
 * <p>Example valid specs: {@code production@~/.vault_pass}, {@code dev@prompt}.
 */
public class VaultId {

  private final String id;
  private final String fileOrPrompt;

  /**
   * Parses a vault id specification into its label and password-source components.
   *
   * @param spec the raw argument in the form {@code label@source}; must contain exactly one
   *             {@code @} separator with a non-empty label before it and a non-empty source after it
   * @throws CommandLineException if {@code spec} contains no {@code @} character, if the label
   *                              segment is empty, or if the source segment is empty
   */
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

  /**
   * Returns the vault label (the segment before the {@code @}).
   *
   * <p>This value is written into the vault 1.2 header so that {@code ansible-vault} can select
   * the correct password when a playbook references multiple vault ids.
   *
   * @return non-empty vault label; never {@code null}
   */
  public String getId () {

    return id;
  }

  /**
   * Returns the password source (the segment after the {@code @}).
   *
   * <p>The value is either a file-system path whose contents supply the vault password, or the
   * literal string {@code prompt}, which signals that the password must be obtained interactively.
   *
   * @return non-empty file path or {@code "prompt"}; never {@code null}
   */
  public String getFileOrPrompt () {

    return fileOrPrompt;
  }
}
