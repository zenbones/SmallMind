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
package org.smallmind.nutsnbolts.command.template;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.smallmind.nutsnbolts.command.CommandLineException;
import org.smallmind.nutsnbolts.command.sax.OptionsDocumentExtender;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.xml.SmallMindProtocolResolver;
import org.smallmind.nutsnbolts.xml.XMLEntityResolver;
import org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser;
import org.xml.sax.InputSource;

/**
 * Defines the complete set of allowable command line options for an application and serves as the contract passed to {@link org.smallmind.nutsnbolts.command.CommandLineParser}.
 */
public class Template {

  private static final XMLEntityResolver SMALL_MIND_ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());

  private final LinkedList<Option> optionList = new LinkedList<>();
  private final HashSet<Option> optionSet = new HashSet<>();
  private final HashSet<String> nameAndFlagSet = new HashSet<>();
  private final String shortName;

  /**
   * Creates a template whose short name is the simple class name of the given entry class.
   *
   * @param entryClass class whose simple name becomes the command's short name
   */
  public Template (Class<?> entryClass) {

    shortName = entryClass.getSimpleName();
  }

  /**
   * Creates a template with an explicit short name and registers the supplied root options immediately.
   *
   * @param shortName short name of the command, typically used in help/usage output
   * @param options   root-level options to register at construction time
   * @throws CommandLineException if any option fails validation (missing name and flag, or duplicate name/flag)
   */
  public Template (String shortName, Option... options)
    throws CommandLineException {

    this.shortName = shortName;

    addOptions(options);
  }

  /**
   * Loads and returns a {@link Template} by parsing an XML descriptor resource located alongside the given entry class.
   * The resource is expected on the classpath at {@code <canonical-class-path>.arguments.xml}.
   *
   * @param entryClass class whose canonical name determines the path of the XML descriptor
   * @return fully populated {@link Template} derived from the XML descriptor
   * @throws CommandLineException if the resource cannot be located, or if parsing or option validation fails
   */
  public static Template createTemplate (Class<?> entryClass)
    throws CommandLineException {

    Template template;
    OptionsDocumentExtender optionsDocumentExtender;
    InputStream optionsInputStream;

    template = new Template(entryClass);
    optionsDocumentExtender = new OptionsDocumentExtender(template);

    try {
      if ((optionsInputStream = Template.class.getClassLoader().getResourceAsStream(entryClass.getCanonicalName().replace('.', '/') + ".arguments.xml")) == null) {
        throw new CommandLineException("No arguments file found matching Class(%s)", entryClass.getCanonicalName());
      }
      ExtensibleSAXParser.parse(optionsDocumentExtender, new InputSource(optionsInputStream), SMALL_MIND_ENTITY_RESOLVER);
    } catch (Exception exception) {
      throw new CommandLineException(exception);
    }

    return template;
  }

  /**
   * Returns the short name of the command, typically the entry class simple name or an explicitly supplied label.
   *
   * @return command short name for use in help and usage output
   */
  public String getShortName () {

    return shortName;
  }

  /**
   * Returns an unmodifiable view of the root-level options registered in declaration order.
   *
   * @return immutable ordered list of root {@link Option} objects
   */
  public synchronized List<Option> getRootOptionList () {

    return Collections.unmodifiableList(optionList);
  }

  /**
   * Returns an unmodifiable view of all registered options, including nested child options.
   *
   * @return immutable flat set of all {@link Option} objects known to this template
   */
  public synchronized Set<Option> getOptionSet () {

    return Collections.unmodifiableSet(optionSet);
  }

  /**
   * Registers one or more root options with this template.
   *
   * @param options options to add at the root level
   * @throws CommandLineException if any option lacks both a name and a flag, or if a name or flag is already in use
   */
  public synchronized void addOptions (Option... options)
    throws CommandLineException {

    addOptions(Arrays.asList(options), false);
  }

  /**
   * Registers a list of options, recursively processing any child options, and optionally suppressing their addition to the root option list.
   *
   * @param optionList ordered list of options to register
   * @param sublist    {@code true} when the list represents child options and should not be added to the root list
   * @throws CommandLineException if any option lacks both a name and a flag, or if a name or flag is not unique
   */
  public synchronized void addOptions (List<Option> optionList, boolean sublist)
    throws CommandLineException {

    for (Option option : optionList) {

      if (((option.getName() == null) || option.getName().isEmpty()) && (option.getFlag() == null)) {
        throw new CommandLineException("All options must have either their 'name' or 'flag' set");
      } else {
        if ((option.getName() != null) && (!option.getName().isEmpty())) {
          if (nameAndFlagSet.contains(option.getName())) {
            throw new CommandLineException("All options must have a unique 'name', '%s' has been used", option.getName());
          } else {
            nameAndFlagSet.add(option.getName());
          }
        }
        if (option.getFlag() != null) {
          if (nameAndFlagSet.contains(option.getFlag().toString())) {
            throw new CommandLineException("All options must have a unique 'flag', '%s' has been used", option.getFlag().toString());
          } else {
            nameAndFlagSet.add(option.getFlag().toString());
          }
        }
      }

      optionSet.add(option);

      if (option.getChildren() != null) {
        addOptions(option.getChildren(), true);
      }
    }

    if (!sublist) {
      this.optionList.addAll(optionList);
    }
  }

  /**
   * Returns a human-readable, nested textual representation of all registered options and their argument types.
   *
   * @return formatted string showing the option hierarchy with required/optional brackets
   */
  public synchronized String toString () {

    StringBuilder lineBuilder = new StringBuilder();

    stringifyOptions(lineBuilder, optionList);

    return lineBuilder.toString();
  }

  /**
   * Recursively appends a formatted representation of each option in the list to the builder, descending into child options.
   *
   * @param lineBuilder builder that accumulates the formatted output
   * @param optionList  options to format at the current nesting level
   */
  private void stringifyOptions (StringBuilder lineBuilder, List<Option> optionList) {

    boolean first = true;

    lineBuilder.append('{');
    for (Option option : optionList) {

      boolean named = false;

      if (!first) {
        lineBuilder.append(' ');
      }

      lineBuilder.append(option.isRequired() ? '[' : '<');

      if ((option.getName() != null) && (!option.getName().isEmpty())) {
        lineBuilder.append("--").append(option.getName());
        named = true;
      }
      if (option.getFlag() != null) {
        if (named) {
          lineBuilder.append("|");
        }
        lineBuilder.append('-').append(option.getFlag());
      }

      switch (option.getArgument().getType()) {
        case NONE:
          break;
        case SINGLE:
          lineBuilder.append(' ').append(((SingleArgument)option.getArgument()).getDescription());
          break;
        case LIST:
          lineBuilder.append(' ').append(((ListArgument)option.getArgument()).getDescription()).append("...");
          break;
        case ENUMERATED:
          lineBuilder.append(" (");
          for (int index = 0; index < ((EnumeratedArgument)option.getArgument()).getValues().length; index++) {
            if (index > 0) {
              lineBuilder.append('|');
            }
            lineBuilder.append(((EnumeratedArgument)option.getArgument()).getValues()[index]);
          }
          lineBuilder.append(')');
          break;
        case MULTIPLE:
          lineBuilder.append("... ").append(((MultipleArgument)option.getArgument()).getDescription());
          break;
        default:
          throw new UnknownSwitchCaseException(option.getArgument().getType().name());
      }

      if ((option.getChildren() != null) && (!option.getChildren().isEmpty())) {
        lineBuilder.append(' ');
        stringifyOptions(lineBuilder, option.getChildren());
      }

      lineBuilder.append(option.isRequired() ? ']' : '>');

      first = false;
    }
    lineBuilder.append('}');
  }
}
