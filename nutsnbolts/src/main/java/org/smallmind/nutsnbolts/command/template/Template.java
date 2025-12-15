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
 * Defines the complete set of allowable command line options for an application.
 */
public class Template {

  private static final XMLEntityResolver SMALL_MIND_ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());

  private final LinkedList<Option> optionList = new LinkedList<>();
  private final HashSet<Option> optionSet = new HashSet<>();
  private final HashSet<String> nameAndFlagSet = new HashSet<>();
  private final String shortName;

  /**
   * Creates a template whose default short name is derived from the entry class.
   *
   * @param entryClass class representing the entry point
   */
  public Template (Class<?> entryClass) {

    shortName = entryClass.getSimpleName();
  }

  /**
   * Creates a template with the given short name and optional root options.
   *
   * @param shortName short name of the command (used in help)
   * @param options   root options to register
   * @throws CommandLineException if validation of the options fails
   */
  public Template (String shortName, Option... options)
    throws CommandLineException {

    this.shortName = shortName;

    addOptions(options);
  }

  /**
   * Loads a template from an XML descriptor named after the entry class with suffix ".arguments.xml".
   *
   * @param entryClass class representing the entry point
   * @return populated template
   * @throws CommandLineException if the resource is missing or parsing fails
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
   * @return short name for the command
   */
  public String getShortName () {

    return shortName;
  }

  /**
   * @return immutable view of the root option list
   */
  public synchronized List<Option> getRootOptionList () {

    return Collections.unmodifiableList(optionList);
  }

  /**
   * @return immutable view of all options, including nested children
   */
  public synchronized Set<Option> getOptionSet () {

    return Collections.unmodifiableSet(optionSet);
  }

  /**
   * Adds one or more root options to the template.
   *
   * @param options options to register
   * @throws CommandLineException if validation fails
   */
  public synchronized void addOptions (Option... options)
    throws CommandLineException {

    addOptions(Arrays.asList(options), false);
  }

  /**
   * Adds options to the template, optionally indicating that the list is a nested child list.
   *
   * @param optionList options to register
   * @param sublist    {@code true} when the list represents child options
   * @throws CommandLineException if names or flags are missing or not unique
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
   * Renders the template as a nested textual representation.
   */
  public synchronized String toString () {

    StringBuilder lineBuilder = new StringBuilder();

    stringifyOptions(lineBuilder, optionList);

    return lineBuilder.toString();
  }

  /**
   * Recursive helper to append options to a string builder in readable form.
   *
   * @param lineBuilder builder receiving the formatted options
   * @param optionList  options to render
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
