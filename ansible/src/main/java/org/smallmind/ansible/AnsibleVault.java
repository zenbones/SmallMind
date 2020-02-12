/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.OptionSet;
import org.smallmind.nutsnbolts.command.template.NoneArgument;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.command.template.SingleArgument;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class AnsibleVault {

  private static final Template ENCRYPT_TEMPLATE;
  private static final String ACTIONS = "decrypt|edit|encrypt|encrypt_string|view|rekey";

  static {

    try {
      ENCRYPT_TEMPLATE = new Template("ansible-vault", new Option("help", null, false, NoneArgument.instance()), new Option("vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("vault-password-file", null, false, new SingleArgument("file")));
    } catch (CommandLineException commandLineException) {
      throw new StaticInitializationError(commandLineException);
    }
  }

  public static void main (String... args)
    throws IOException, CommandLineException {

    if ((args == null) || (args.length == 0)) {
      throw new CommandLineException("Missing 'action', requires one of [(%s)", ACTIONS);
    } else {

      String[] remainingArgs = new String[args.length - 1];

      System.arraycopy(args, 1, remainingArgs, 0, args.length - 1);

      switch (args[0]) {
        case "--help":
          break;
        case "encrypt":

          OptionSet encryptOptionSet = CommandLineParser.parseCommands(ENCRYPT_TEMPLATE, remainingArgs, true);

          if (encryptOptionSet.containsOption("help")) {
            System.out.print("create ");
            System.out.print(ENCRYPT_TEMPLATE.toString());
            System.out.println(" [file list]");
          } else if (encryptOptionSet.containsOption("vault-id") && encryptOptionSet.containsOption("vault-password-file")) {
            throw new CommandLineException("Options can user either 'vault-id' or 'vault-password-file', but not both");
          } else {

            String[] remaining;

            if ((remaining = encryptOptionSet.getRemaining()).length == 0) {
              throw new CommandLineException("Missing a list of files to encrypt");
            } else {
              for (String file : remaining) {
                  VaultCodec.encrypt(Files.newInputStream(Paths.get(file)), );
              }
            }
          }
          break;
        default:
          throw new CommandLineException("Unknown 'action', requires one of [(%s)]", ACTIONS);
      }
    }
  }
}
