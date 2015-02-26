/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.namespace.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.quorum.namespace.java.backingStore.NameTranslator;

public class JavaName implements Name {

  private static final AlphaNumericComparator<String> alphaSort = new AlphaNumericComparator<String>();

  private NameTranslator nameTranslator;
  private ArrayList<String> nameList;

  public JavaName (JavaName name) {

    this(name.getNameTranslator(), name.getNameList());
  }

  public JavaName (NameTranslator nameTranslator) {

    this(nameTranslator, new ArrayList<String>());
  }

  private JavaName (NameTranslator nameTranslator, List<String> externalList) {

    this.nameTranslator = nameTranslator;
    this.nameList = new ArrayList<String>(externalList);
  }

  protected NameTranslator getNameTranslator () {

    return nameTranslator;
  }

  protected ArrayList<String> getNameList () {

    return nameList;
  }

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

  public int size () {

    return nameList.size();
  }

  public boolean isEmpty () {

    return nameList.isEmpty();
  }

  public Enumeration<String> getAll () {

    return Collections.enumeration(nameList);
  }

  public String get (int posn) {

    return nameList.get(posn);
  }

  public Name getPrefix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(0, posn));
  }

  public Name getSuffix (int posn) {

    return new JavaName(nameTranslator, nameList.subList(posn, nameList.size()));
  }

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

  public Name addAll (Name suffix)
    throws InvalidNameException {

    int count;

    for (count = 0; count < suffix.size(); count++) {
      nameList.add(suffix.get(count));
    }
    return this;
  }

  public Name addAll (int posn, Name n)
    throws InvalidNameException {

    int count;

    for (count = 0; count < n.size(); count++) {
      nameList.add(posn, n.get(count));
    }
    return this;
  }

  public Name add (String comp)
    throws InvalidNameException {

    nameList.add(comp);
    return this;
  }

  public Name add (int posn, String comp)
    throws InvalidNameException {

    nameList.add(posn, comp);
    return this;
  }

  public Object remove (int posn)
    throws InvalidNameException {

    return nameList.remove(posn);
  }

  public Object clone () {

    return new JavaName(this);
  }

  public String toString () {

    return nameTranslator.fromExternalNameToExternalString(this);
  }
}
