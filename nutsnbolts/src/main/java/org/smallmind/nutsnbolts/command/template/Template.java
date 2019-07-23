/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class Template {

  private static final XMLEntityResolver SMALL_MIND_ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());

  private LinkedList<Option> optionList;
  private HashSet<Option> optionSet = new HashSet<>();
  private String shortName;

  public Template (Class entryClass) {

    shortName = entryClass.getSimpleName();
  }

  public static Template createTemplate (Class entryClass)
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

  public String getShortName () {

    return shortName;
  }

  public synchronized List<Option> getRootOptionList () {

    return Collections.unmodifiableList(optionList);
  }

  public synchronized Set<Option> getOptionSet () {

    return Collections.unmodifiableSet(optionSet);
  }

  public synchronized void setOptionList (LinkedList<Option> optionList)
    throws CommandLineException {

    this.optionList = optionList;

    addOptions(optionList);
  }

  public synchronized void addOptions (LinkedList<Option> optionList)
    throws CommandLineException {

    for (Option option : optionList) {

      if (((option.getName() == null) || option.getName().isEmpty()) && (option.getFlag() == null)) {
        throw new CommandLineException("All options must have either their 'name' or 'flag' set");
      }

      for (Option heldOption : optionSet) {
        if ((option.getName() != null) && (!option.getName().isEmpty())) {
          if (option.getName().equals(heldOption.getName())) {
            throw new CommandLineException("All options must have a unique 'name', '%s' has been used", option.getName());
          }
        }
        if (option.getFlag() != null) {
          if (option.getFlag().equals(heldOption.getFlag())) {
            throw new CommandLineException("All options must have a unique 'flag', '%s' has been used", option.getFlag().toString());
          }
        }
      }

      optionSet.add(option);
    }
  }

  public synchronized String toString () {

    StringBuilder lineBuilder = new StringBuilder();

    stringifyOptions(lineBuilder, optionList);

    return lineBuilder.toString();
  }

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
          lineBuilder.append(", ");
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
        default:
          throw new UnknownSwitchCaseException(option.getArgument().getType().name());
      }

      if ((option.getOptionList() != null) && (!option.getOptionList().isEmpty())) {
        lineBuilder.append(' ');
        stringifyOptions(lineBuilder, option.getOptionList());
      }

      lineBuilder.append(option.isRequired() ? ']' : '>');

      first = false;
    }
    lineBuilder.append('}');
  }
}
