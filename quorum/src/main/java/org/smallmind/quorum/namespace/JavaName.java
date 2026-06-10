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
 * {@link Name} implementation used within the {@code java:} namespace that carries a
 * {@link NameTranslator} so it can render itself in the backing store's external format.
 * <p>
 * Components are stored in an {@link ArrayList} in logical order (most general last for LDAP).
 * Prefix and suffix slices, like {@link #clone()}, copy the relevant components into a new backing
 * list, so each returned name is independent: mutating the original after a slice is taken does not
 * affect the slice, and vice versa.
 * <p>
 * Comparison first orders by size (shorter names come first), then component-by-component using
 * an alphanumeric sort.
 */
public class JavaName implements Name {

  private static final AlphaNumericComparator<String> alphaSort = new AlphaNumericComparator<String>();

  private final NameTranslator nameTranslator;
  private final ArrayList<String> nameList;

  /**
   * Copy constructor that duplicates the component list of the given name.
   *
   * @param name the source name to copy; its translator is reused
   */
  public JavaName (JavaName name) {

    this(name.getNameTranslator(), name.getNameList());
  }

  /**
   * Creates an empty name backed by the given translator.
   *
   * @param nameTranslator the translator used to render this name to its external string form
   */
  public JavaName (NameTranslator nameTranslator) {

    this(nameTranslator, new ArrayList<>());
  }

  private JavaName (NameTranslator nameTranslator, List<String> externalList) {

    this.nameTranslator = nameTranslator;
    this.nameList = new ArrayList<>(externalList);
  }

  /**
   * Returns the translator used to convert this name to its external string form.
   *
   * @return the {@link NameTranslator}; never {@code null}
   */
  protected NameTranslator getNameTranslator () {

    return nameTranslator;
  }

  /**
   * Returns the mutable backing list of name components.
   *
   * @return the component list; never {@code null}
   */
  protected ArrayList<String> getNameList () {

    return nameList;
  }

  /**
   * Compares this name to another {@link JavaName} first by size, then alphanumerically
   * component by component.
   *
   * @param obj the object to compare to; must be a {@link JavaName}
   * @return a negative integer, zero, or a positive integer as this name is less than, equal to,
   * or greater than {@code obj}
   * @throws ClassCastException if {@code obj} is not an instance of {@link JavaName}
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
   * Returns the number of components in this name.
   *
   * @return the component count; {@code 0} for an empty name
   */
  public int size () {

    return nameList.size();
  }

  /**
   * Returns {@code true} if this name has no components.
   *
   * @return {@code true} when empty
   */
  public boolean isEmpty () {

    return nameList.isEmpty();
  }

  /**
   * Returns an enumeration over the components of this name in order.
   *
   * @return a non-null {@link Enumeration} of component strings
   */
  public Enumeration<String> getAll () {

    return Collections.enumeration(nameList);
  }

  /**
   * Returns the component at the given zero-based index.
   *
   * @param posn zero-based index of the component to return
   * @return the component string at position {@code posn}
   * @throws IndexOutOfBoundsException if {@code posn} is out of range
   */
  public String get (int posn) {

    return nameList.get(posn);
  }

  /**
   * Returns a {@link JavaName} containing the first {@code posn} components of this name.
   *
   * @param posn the exclusive end index of the prefix; {@code 0} returns an empty name
   * @return a new {@link JavaName} holding an independent copy of the first {@code posn} components
   */
  public Name getPrefix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(0, posn));
  }

  /**
   * Returns a {@link JavaName} containing the components from index {@code posn} to the end.
   *
   * @param posn the inclusive start index of the suffix
   * @return a new {@link JavaName} holding an independent copy of the components from {@code posn} onward
   */
  public Name getSuffix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(posn, nameList.size()));
  }

  /**
   * Tests whether this name begins with the same components as {@code n}.
   *
   * @param n the name whose components to compare against the start of this name
   * @return {@code true} if this name has at least {@code n.size()} components and the first
   * {@code n.size()} components are equal to those of {@code n}
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
   * Tests whether this name ends with the same components as {@code n}.
   *
   * @param n the name whose components to compare against the end of this name
   * @return {@code true} if this name has at least {@code n.size()} components and the last
   * {@code n.size()} components are equal to those of {@code n}
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
   * Appends all components of {@code suffix} to the end of this name.
   *
   * @param suffix the name whose components to append
   * @return this name, for chaining
   * @throws InvalidNameException if {@link #add(String)} rejects any component of {@code suffix}
   */
  public Name addAll (Name suffix) {

    int count;

    for (count = 0; count < suffix.size(); count++) {
      nameList.add(suffix.get(count));
    }
    return this;
  }

  /**
   * Inserts all components of {@code n} into this name starting at position {@code posn}.
   *
   * @param posn the index at which insertion begins; existing components at and after this
   *             position are shifted right
   * @param n    the name whose components to insert
   * @return this name, for chaining
   * @throws InvalidNameException if {@link #add(int, String)} rejects any component
   */
  public Name addAll (int posn, Name n) {

    int count;

    for (count = 0; count < n.size(); count++) {
      nameList.add(posn + count, n.get(count));
    }
    return this;
  }

  /**
   * Appends a single component to the end of this name.
   *
   * @param comp the component string to append; must not be {@code null}
   * @return this name, for chaining
   * @throws InvalidNameException never thrown by this implementation
   */
  public Name add (String comp)
    throws InvalidNameException {

    nameList.add(comp);
    return this;
  }

  /**
   * Inserts a single component at position {@code posn}.
   *
   * @param posn the zero-based index at which to insert; existing components at and after this
   *             position are shifted right
   * @param comp the component string to insert; must not be {@code null}
   * @return this name, for chaining
   * @throws InvalidNameException never thrown by this implementation
   */
  public Name add (int posn, String comp)
    throws InvalidNameException {

    nameList.add(posn, comp);
    return this;
  }

  /**
   * Removes and returns the component at position {@code posn}.
   *
   * @param posn the zero-based index of the component to remove
   * @return the removed component string
   * @throws InvalidNameException never thrown by this implementation
   */
  public Object remove (int posn)
    throws InvalidNameException {

    return nameList.remove(posn);
  }

  /**
   * Returns a deep copy of this name with a new, independent backing list.
   *
   * @return a new {@link JavaName} with the same translator and an independent copy of the
   * component list
   */
  public Object clone () {

    return new JavaName(this);
  }

  /**
   * Returns the external string representation of this name as produced by the associated
   * {@link NameTranslator}.
   *
   * @return the external string form; never {@code null}
   */
  public String toString () {

    return nameTranslator.fromExternalNameToExternalString(this);
  }
}
