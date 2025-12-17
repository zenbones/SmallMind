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
package org.smallmind.quorum.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.quorum.namespace.backingStore.NameTranslator;

/**
 * Implementation of {@link Name} that works with a {@link NameTranslator} to convert
 * between internal and external name forms.
 */
public class JavaName implements Name {

  private static final AlphaNumericComparator<String> alphaSort = new AlphaNumericComparator<String>();

  private final NameTranslator nameTranslator;
  private final ArrayList<String> nameList;

  /**
   * Copy constructor.
   *
   * @param name source name to duplicate
   */
  public JavaName (JavaName name) {

    this(name.getNameTranslator(), name.getNameList());
  }

  /**
   * Constructs an empty name using the provided translator.
   *
   * @param nameTranslator translator used to render names
   */
  public JavaName (NameTranslator nameTranslator) {

    this(nameTranslator, new ArrayList<String>());
  }

  private JavaName (NameTranslator nameTranslator, List<String> externalList) {

    this.nameTranslator = nameTranslator;
    this.nameList = new ArrayList<String>(externalList);
  }

  /**
   * Returns the translator used for external/internal conversions.
   *
   * @return name translator
   */
  protected NameTranslator getNameTranslator () {

    return nameTranslator;
  }

  /**
   * Returns the mutable list of name components.
   *
   * @return underlying component list
   */
  protected ArrayList<String> getNameList () {

    return nameList;
  }

  /**
   * Compares this name to another {@link JavaName} using size then alphanumeric components.
   *
   * @param obj object to compare
   * @return negative, zero, or positive per {@link Comparable} contract
   * @throws ClassCastException if {@code obj} is not a {@link JavaName}
   */
  public int compareTo (Object obj) {

    int comparison;
    int count;

    if (!(obj instanceof JavaName)) {
      throw new ClassCastException("Must be an instance of (" + this.getClass().getName() + ")");
    }
    if (nameList.size() < ((JavaName)obj).size()) {
      return -1;
    } else if (nameList.size() > ((JavaName)obj).size()) {
      return 1;
    }
    for (count = 0; count < size(); count++) {
      comparison = alphaSort.compare(nameList.get(count), ((JavaName)obj).get(count));
      if (comparison != 0) {
        return comparison;
      }
    }
    return 0;
  }

  /**
   * Returns the number of components in the name.
   *
   * @return size of the name
   */
  public int size () {

    return nameList.size();
  }

  /**
   * Indicates whether the name has no components.
   *
   * @return {@code true} if empty
   */
  public boolean isEmpty () {

    return nameList.isEmpty();
  }

  /**
   * Returns an enumeration of the name components.
   *
   * @return enumeration over components
   */
  public Enumeration<String> getAll () {

    return Collections.enumeration(nameList);
  }

  /**
   * Returns the component at the given position.
   *
   * @param posn zero-based position
   * @return component value
   */
  public String get (int posn) {

    return nameList.get(posn);
  }

  /**
   * Returns a prefix of the name.
   *
   * @param posn exclusive end index
   * @return prefix as a new {@link Name}
   */
  public Name getPrefix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(0, posn));
  }

  /**
   * Returns a suffix of the name starting at the given position.
   *
   * @param posn start index
   * @return suffix as a new {@link Name}
   */
  public Name getSuffix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(posn, nameList.size()));
  }

  /**
   * Tests whether this name starts with the components of the supplied name.
   *
   * @param n name to compare
   * @return {@code true} if this name begins with {@code n}
   */
  public boolean startsWith (Name n) {

    int count;

    if (nameList.size() < n.size()) {
      return false;
    }
    for (count = 0; count < n.size(); count++) {
      if (!n.get(count).equals(nameList.get(count))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Tests whether this name ends with the components of the supplied name.
   *
   * @param n name to compare
   * @return {@code true} if this name ends with {@code n}
   */
  public boolean endsWith (Name n) {

    int count;

    if (nameList.size() < n.size()) {
      return false;
    }
    for (count = 0; count < n.size(); count++) {
      if (!n.get(n.size() - (count + 1)).equals(nameList.get(nameList.size() - (count + 1)))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Appends all components of the supplied suffix to this name.
   *
   * @param suffix name whose components to add
   * @return this name for chaining
   * @throws InvalidNameException if the suffix is invalid
   */
  public Name addAll (Name suffix)
    throws InvalidNameException {

    int count;

    for (count = 0; count < suffix.size(); count++) {
      nameList.add(suffix.get(count));
    }
    return this;
  }

  /**
   * Inserts the given name at the specified position.
   *
   * @param posn position to start insertion
   * @param n    name whose components to insert
   * @return this name for chaining
   * @throws InvalidNameException if insertion fails
   */
  public Name addAll (int posn, Name n)
    throws InvalidNameException {

    int count;

    for (count = 0; count < n.size(); count++) {
      nameList.add(posn, n.get(count));
    }
    return this;
  }

  /**
   * Appends a single component to the end of this name.
   *
   * @param comp component to add
   * @return this name for chaining
   * @throws InvalidNameException if the component is invalid
   */
  public Name add (String comp)
    throws InvalidNameException {

    nameList.add(comp);
    return this;
  }

  /**
   * Inserts a component at the specified position.
   *
   * @param posn index at which to insert
   * @param comp component to insert
   * @return this name for chaining
   * @throws InvalidNameException if the component is invalid
   */
  public Name add (int posn, String comp)
    throws InvalidNameException {

    nameList.add(posn, comp);
    return this;
  }

  /**
   * Removes the component at the given position.
   *
   * @param posn position of component to remove
   * @return removed component
   * @throws InvalidNameException if the position is invalid
   */
  public Object remove (int posn)
    throws InvalidNameException {

    return nameList.remove(posn);
  }

  /**
   * Creates a deep copy of this name.
   *
   * @return cloned name
   */
  public Object clone () {

    return new JavaName(this);
  }

  /**
   * Converts the name to its external string representation.
   *
   * @return external string form
   */
  public String toString () {

    return nameTranslator.fromExternalNameToExternalString(this);
  }
}
