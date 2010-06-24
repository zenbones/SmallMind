package org.smallmind.nutsnbolts.maven.sax;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import org.smallmind.nutsnbolts.xml.XMLEntityResolver;
import org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SettingsSAXParser {

   public static HashMap<String, String> parse (String profile)
      throws IOException, SAXException, ParserConfigurationException {

      SettingsDocumentExtender settingsDocumentExtender;
      StringBuilder settingsPathBuilder = new StringBuilder();

      settingsPathBuilder.append(System.getProperty("user.home"));
      settingsPathBuilder.append(System.getProperty("file.separator"));
      settingsPathBuilder.append(".m2");
      settingsPathBuilder.append(System.getProperty("file.separator"));
      settingsPathBuilder.append("settings.xml");

      ExtensibleSAXParser.parse(settingsDocumentExtender = new SettingsDocumentExtender(profile), new InputSource(new FileInputStream(settingsPathBuilder.toString())), XMLEntityResolver.getInstance(), false);

      return settingsDocumentExtender.getPropertyMap();
   }
}
