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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.OptionSet;
import org.smallmind.nutsnbolts.command.template.NoneArgument;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.command.template.SingleArgument;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class AnsibleVault {

  private static final VaultHelp DECRYPT_HELP;
  private static final VaultHelp ENCRYPT_HELP;
  private static final VaultHelp REKEY_HELP;
  private static final VaultHelp VIEW_HELP;
  private static final String ACTIONS = "decrypt|encrypt|encrypt_string|rekey|view";

  static {

    try {
      DECRYPT_HELP = new VaultHelp(new Template("ansible-vault", new Option("help", null, false, NoneArgument.instance()), new Option("vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("vault-password-file", null, false, new SingleArgument("file"))),
        (template, stream) -> {
          stream.print("decrypt ");
          stream.print(template);
          stream.println(" [files...]");
        });
      ENCRYPT_HELP = new VaultHelp(new Template("ansible-vault", new Option("help", null, false, NoneArgument.instance()), new Option("vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("vault-password-file", null, false, new SingleArgument("file"))),
        (template, stream) -> {
          stream.print("encrypt ");
          stream.print(template);
          stream.println(" [files...]");
        });
      REKEY_HELP = new VaultHelp(new Template("ansible-vault", new Option("help", null, false, NoneArgument.instance()), new Option("vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("new-vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("vault-password-file", null, false, new SingleArgument("file")), new Option("new-vault-password-file", null, false, new SingleArgument("file"))),
        (template, stream) -> {
          stream.print("rekey ");
          stream.print(template);
          stream.println(" [files...]");
        });
      VIEW_HELP = new VaultHelp(new Template("ansible-vault", new Option("help", null, false, NoneArgument.instance()), new Option("vault-id", null, false, new SingleArgument("[id]@[file|'prompt']")), new Option("vault-password-file", null, false, new SingleArgument("file"))),
        (template, stream) -> {
          stream.print("view ");
          stream.print(template);
          stream.println(" [file]");
        });
    } catch (CommandLineException commandLineException) {
      throw new StaticInitializationError(commandLineException);
    }
  }

  public static void main (String... args) {

    try {
      if ((args == null) || (args.length == 0)) {
        throw new CommandLineException("Missing 'action', requires one of [(%s)]", ACTIONS);
      } else {

        String[] remainingArgs = new String[args.length - 1];

        System.arraycopy(args, 1, remainingArgs, 0, args.length - 1);

        switch (args[0]) {
          case "--help":
            provideHelp();
            break;
          case "decrypt":

            OptionSet decryptOptionSet = CommandLineParser.parseCommands(DECRYPT_HELP.getTemplate(), remainingArgs, true);

            if (decryptOptionSet.containsOption("help")) {
              System.out.print("ansible-vault ");
              DECRYPT_HELP.out(System.out);
            } else {

              PasswordAndId passwordAndId = getPasswordAndId(decryptOptionSet, "vault-id", "vault-password-file", false);
              String[] remaining;

              if ((remaining = decryptOptionSet.getRemaining()).length == 0) {
                throw new CommandLineException("Missing a list of files to decrypt");
              } else {
                for (String file : remaining) {

                  Path path = Paths.get(file);

                  Files.write(path, VaultCodec.decrypt(Files.newInputStream(path), passwordAndId.getPassword()), StandardOpenOption.TRUNCATE_EXISTING);
                }
              }
            }
            break;
          case "encrypt":

            OptionSet encryptOptionSet = CommandLineParser.parseCommands(ENCRYPT_HELP.getTemplate(), remainingArgs, true);

            if (encryptOptionSet.containsOption("help")) {
              System.out.print("ansible-vault ");
              ENCRYPT_HELP.out(System.out);
            } else {

              PasswordAndId passwordAndId = getPasswordAndId(encryptOptionSet, "vault-id", "vault-password-file", true);
              String[] remaining;

              if ((remaining = encryptOptionSet.getRemaining()).length == 0) {
                throw new CommandLineException("Missing a list of files to encrypt");
              } else {
                for (String file : remaining) {

                  Path path = Paths.get(file);

                  Files.write(path, VaultCodec.encrypt(Files.newInputStream(path), passwordAndId.getPassword(), passwordAndId.getId()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                }
              }
            }
            break;
          case "rekey":

            OptionSet rekeyOptionSet = CommandLineParser.parseCommands(REKEY_HELP.getTemplate(), remainingArgs, true);

            if (rekeyOptionSet.containsOption("help")) {
              System.out.print("ansible-vault ");
              REKEY_HELP.out(System.out);
            } else {

              PasswordAndId passwordAndId = getPasswordAndId(rekeyOptionSet, "vault-id", "vault-password-file", false);
              PasswordAndId newPasswordAndId = getPasswordAndId(rekeyOptionSet, "new-vault-id", "new-vault-password-file", true);
              String[] remaining;

              if ((remaining = rekeyOptionSet.getRemaining()).length == 0) {
                throw new CommandLineException("Missing a list of files to rekey");
              } else {
                for (String file : remaining) {

                  Path path = Paths.get(file);
                  byte[] contents = VaultCodec.decrypt(Files.newInputStream(path), passwordAndId.getPassword());

                  Files.write(path, VaultCodec.encrypt(new ByteArrayInputStream(contents), newPasswordAndId.getPassword()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                }
              }
            }
            break;
          case "view":

            OptionSet viewOptionSet = CommandLineParser.parseCommands(VIEW_HELP.getTemplate(), remainingArgs, true);

            if (viewOptionSet.containsOption("help")) {
              System.out.print("ansible-vault ");
              VIEW_HELP.out(System.out);
            } else {

              PasswordAndId passwordAndId = getPasswordAndId(viewOptionSet, "vault-id", "vault-password-file", false);
              String[] remaining;

              if ((remaining = viewOptionSet.getRemaining()).length != 1) {
                throw new CommandLineException("Must request one file to view");
              } else {
                System.out.println(new String(VaultCodec.decrypt(Files.newInputStream(Paths.get(remaining[0])), passwordAndId.getPassword())));
                waitHere();
              }
            }
            break;
          default:
            throw new CommandLineException("Unknown 'action', requires one of [(%s)]", ACTIONS);
        }
      }
    } catch (CommandLineException commandLineException) {
      System.out.println(commandLineException.getMessage());
      provideHelp();
    } catch (Exception exception) {
      System.out.println("Failure...");
      System.out.println(exception.getMessage());
    }
  }

  private static void provideHelp () {

    System.out.print("ansible-vault [(");
    System.out.print(ACTIONS);
    System.out.println("]");
    System.out.print("\t");
    ENCRYPT_HELP.out(System.out);
    System.out.print("\t");
    DECRYPT_HELP.out(System.out);
    System.out.print("\t");
    REKEY_HELP.out(System.out);
    System.out.print("\t");
    VIEW_HELP.out(System.out);
  }

  private static void waitHere ()
    throws CommandLineException {

    Console console;

    if ((console = System.console()) == null) {
      throw new CommandLineException("No available console");
    }
    console.readPassword(":");
  }

  private static PasswordAndId getPasswordAndId (OptionSet optionSet, String idOption, String fileOption, boolean confirm)
    throws IOException, CommandLineException {

    if (optionSet.containsOption(idOption)) {

      VaultId vaultId = new VaultId(optionSet.getArgument(idOption));

      if ("prompt".equals(vaultId.getFileOrPrompt())) {

        return new PasswordAndId(getPasswordFomPrompt(confirm), vaultId.getId());
      } else {

        return new PasswordAndId(getPasswordFromFile(vaultId.getFileOrPrompt()), vaultId.getId());
      }
    } else if (optionSet.containsOption(fileOption)) {

      return new PasswordAndId(getPasswordFromFile(optionSet.getArgument(fileOption)), null);
    } else {

      return new PasswordAndId(getPasswordFomPrompt(confirm), null);
    }
  }

  private static String getPasswordFromFile (String file)
    throws IOException {

    try (FileReader fileReader = new FileReader(file)) {

      ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
      int singleByte;

      while ((singleByte = fileReader.read()) >= 0) {
        if (singleByte != '\n') {
          byteOutputStream.write(singleByte);
        } else {

          return byteOutputStream.toString().trim();
        }
      }

      return byteOutputStream.toString().trim();
    }
  }

  private static String getPasswordFomPrompt (boolean confirm)
    throws CommandLineException {

    Console console;

    if ((console = System.console()) == null) {
      throw new CommandLineException("No available console");
    } else if (confirm) {
      while (true) {

        char[] password = console.readPassword("New Vault password: ");
        char[] confirmation = console.readPassword("Confirm New Vault password: ");

        if (!Arrays.equals(password, confirmation)) {
          System.out.println("Passwords do not match...");
        } else {

          return new String(password);
        }
      }
    } else {

      return new String(console.readPassword("Vault password: "));
    }
  }

  private static class PasswordAndId {

    private String password;
    private String id;

    public PasswordAndId (String password, String id) {

      this.password = password;
      this.id = id;
    }

    public String getPassword () {

      return password;
    }

    public String getId () {

      return id;
    }
  }
}
