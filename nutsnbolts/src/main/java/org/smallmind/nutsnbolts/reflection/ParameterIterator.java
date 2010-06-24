package org.smallmind.nutsnbolts.reflection;

import java.util.Iterator;
import java.util.NoSuchElementException;

/*
* boolean Z
* byte B
* char C
* short S
* int I
* long J
* float F
* double D
* array [
* object L;
* type T;
*/
public class ParameterIterator implements Iterator<String>, Iterable<String> {

  private String encrypted;
  private int index = 0;

  public ParameterIterator (String encrypted) {

    this.encrypted = encrypted;
  }

  public Iterator<String> iterator () {

    return this;
  }

  public boolean hasNext () {

    return index < encrypted.length();
  }

  public String next () {

    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    int arrayDepth = 0;

    do {
      switch (encrypted.charAt(index++)) {
        case 'Z':
          return assembleType("Z", arrayDepth);
        case 'B':
          return assembleType("B", arrayDepth);
        case 'C':
          return assembleType("C", arrayDepth);
        case 'S':
          return assembleType("S", arrayDepth);
        case 'I':
          return assembleType("I", arrayDepth);
        case 'J':
          return assembleType("J", arrayDepth);
        case 'F':
          return assembleType("F", arrayDepth);
        case 'D':
          return assembleType("D", arrayDepth);
        case 'L':

          StringBuilder objectBuilder = new StringBuilder("L");
          char objectChar;

          do {
            switch (objectChar = encrypted.charAt(index++)) {
              case ';':
                return assembleType(objectBuilder.append(';').toString(), arrayDepth);
              default:
                objectBuilder.append(objectChar);
            }
          } while (index < encrypted.length());
          break;
        case '[':
          arrayDepth++;
          break;
        default:
          throw new ByteCodeManipulationException("Unknown format for parameter encrypted(%s)", encrypted);
      }
    } while (index < encrypted.length());

    throw new ByteCodeManipulationException("Unknown format for parameter encrypted(%s)", encrypted);
  }

  private String assembleType (String baseType, int arrayDepth) {

    if (arrayDepth == 0) {

      return baseType;
    }

    StringBuilder arrayBuilder = new StringBuilder();

    for (int count = 0; count < arrayDepth; count++) {
      arrayBuilder.append('[');
    }
    arrayBuilder.append(baseType);

    return arrayBuilder.toString();
  }

  public void remove () {

    throw new UnsupportedOperationException();
  }
}
