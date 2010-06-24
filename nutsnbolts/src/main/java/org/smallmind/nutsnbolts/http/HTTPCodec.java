package org.smallmind.nutsnbolts.http;

import java.text.StringCharacterIterator;
import org.smallmind.nutsnbolts.util.Tuple;

public class HTTPCodec {

  static final String validHex = "1234567890ABCDEFabcdef";

  public static String urlEncode (Tuple<String, String> tuple, String... ignoredKeys) {

    StringBuilder dataBuilder = new StringBuilder();
    String key;

    for (int count = 0; count < tuple.size(); count++) {
      if (dataBuilder.length() > 0) {
        dataBuilder.append('&');
      }

      key = tuple.getKey(count);

      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? key : hexEncode(key));
      dataBuilder.append('=');
      dataBuilder.append(isIgnoredKey(key, ignoredKeys) ? tuple.getValue(count) : hexEncode(tuple.getValue(count)));
    }
    return dataBuilder.toString();
  }

  private static boolean isIgnoredKey (String key, String[] ignoredKeys) {

    if ((ignoredKeys != null) && (ignoredKeys.length > 0)) {
      for (String ignoredKey : ignoredKeys) {
        if (ignoredKey.equals(key)) {

          return true;
        }
      }
    }

    return false;
  }

  public static String hexDecode (String value)
    throws NumberFormatException {

    StringCharacterIterator valueIter;
    StringBuilder modBuilder = new StringBuilder();
    String hexNum;
    int hexInt;

    valueIter = new StringCharacterIterator(value);
    while (valueIter.current() != StringCharacterIterator.DONE) {
      if (valueIter.current() == '+') {
        modBuilder.append(' ');
      }
      else if (valueIter.current() != '%') {
        modBuilder.append(valueIter.current());
      }
      else {
        hexNum = "";
        valueIter.next();
        if (validHex.indexOf(valueIter.current()) >= 0) {
          hexNum += valueIter.current();
          valueIter.next();
          if (validHex.indexOf(valueIter.current()) >= 0) {
            hexNum += valueIter.current();
            hexInt = Integer.valueOf(hexNum, 16);
            modBuilder.append((char)hexInt);
          }
          else {
            modBuilder.append('%');
            modBuilder.append(hexNum);
            modBuilder.append(valueIter.current());
          }
        }
        else {
          modBuilder.append('%');
          modBuilder.append(valueIter.current());
        }
      }
      valueIter.next();
    }
    return modBuilder.toString();
  }

  public static String hexEncode (String value) {

    StringCharacterIterator valueIter;
    StringBuilder modBuilder = new StringBuilder();

    valueIter = new StringCharacterIterator(value);
    while (valueIter.current() != StringCharacterIterator.DONE) {
      if (Character.isSpaceChar(valueIter.current())) {
        modBuilder.append('+');
      }
      else if (Character.isLetterOrDigit(valueIter.current())) {
        modBuilder.append(valueIter.current());
      }
      else {
        modBuilder.append('%');
        modBuilder.append(Integer.toHexString((int)valueIter.current()));
      }
      valueIter.next();
    }
    return modBuilder.toString();
  }
}

