package org.smallmind.nagios;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.smallmind.nutsnbolts.command.CommandException;
import org.smallmind.nutsnbolts.command.CommandLineParser;
import org.smallmind.nutsnbolts.command.CommandSet;

public class CheckJMX {

   /**
    * Usage: check_jmx -U url -O object_name -A attribute [-K compound_key] [-I attribute_info] [-J attribute_info_key] -w warn_limit -c crit_limit [-v[vvv]] [-help]
    * , where options are:
    * -help
    * Prints this page
    * -U
    * JMX URL, for example: "service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi"
    * -O
    * Object name to be checked, for example, "java.lang:type=Memory"
    * -A
    * Attribute of the object to be checked, for example, "NonHeapMemoryUsage"
    * -K
    * Attribute key for -A attribute compound data, for example, "used" (optional)
    * -I
    * Attribute of the object containing information for text output (optional)
    * -J
    * Attribute key for -I attribute compound data, for example, "used" (optional)
    * -v[vvv]
    * verbatim level controlled as a number of v (optional)
    * -w
    * warning integer value
    * -c
    * critical integer value
    * Note that if warning level > critical, system checks object attribute value to be LESS THAN OR EQUAL warning, critical
    * If warning level < critical, system checks object attribute value to be MORE THAN OR EQUAL warning, critical
    */

   private JMXConnector connector;
   private MBeanServerConnection connection;
   private VerbatimLevel verbatimLevel;
   private Object infoData;
   private String url;
   private String attribute, infoAttribute;
   private String attributeKey, infoKey;
   private String object;
   private long warning;
   private long critical;
   private long checkData;

   private void connect ()
      throws IOException {

      JMXServiceURL jmxUrl = new JMXServiceURL(url);
      connector = JMXConnectorFactory.connect(jmxUrl);
      connection = connector.getMBeanServerConnection();
   }

   private void disconnect ()
      throws IOException {

      if (connector != null) {
         connector.close();
         connector = null;
      }
   }

   private int report (Exception exception, PrintStream out) {

      if (exception instanceof CommandException) {
         out.print(ReturnCode.UNKNOWN.name());
         out.print(" ");
         reportException(exception, out);
         out.println(" Usage: check_jmx -help ");

         return ReturnCode.UNKNOWN.getCode();
      }
      else {
         out.print(ReturnCode.CRITICAL.name());
         out.print(" ");
         reportException(exception, out);
         out.println();

         return ReturnCode.CRITICAL.getCode();
      }
   }

   private void reportException (Exception exception, PrintStream out) {

      switch (verbatimLevel) {
         case LACONIC:

            Throwable rootCause = exception;
            while (rootCause.getCause() != null) {
               rootCause = rootCause.getCause();
            }

            out.print(rootCause.getMessage());
            break;
         case STANDARD:
            out.print(exception.getMessage() + " connecting to " + object + " by URL " + url);
            break;
         default:
            exception.printStackTrace(out);
      }
   }

   private int report (PrintStream out) {

      ReturnCode returnCode;

      if (compare(critical, warning < critical)) {
         returnCode = ReturnCode.CRITICAL;

      }
      else if (compare(warning, warning < critical)) {
         returnCode = ReturnCode.WARN;
      }
      else {
         returnCode = ReturnCode.OK;
      }

      out.print(returnCode.name() + " ");

      if ((infoData == null) || (verbatimLevel.ordinal() >= VerbatimLevel.STANDARD.ordinal())) {
         if (attributeKey != null) {
            out.print(attribute + '.' + attributeKey + '=' + checkData);
         }
         else {
            out.print(attribute + '=' + checkData);
         }
      }

      if (infoData != null) {
         if (infoData instanceof CompositeDataSupport) {
            report((CompositeDataSupport)infoData, out);
         }
         else {
            out.print(infoData.toString());
         }
      }

      out.println();

      return returnCode.getCode();
   }

   private void report (CompositeDataSupport data, PrintStream out) {

      CompositeType type = data.getCompositeType();

      out.print('{');
      for (Iterator it = type.keySet().iterator(); it.hasNext();) {
         String key = (String)it.next();
         if (data.containsKey(key)) {
            out.print(key + '=' + data.get(key));
         }
         if (it.hasNext()) {
            out.print(';');
         }
      }
      out.print('}');
   }

   private boolean compare (long level, boolean more) {

      if (more) {
         return checkData >= level;
      }
      else {
         return checkData <= level;
      }
   }

   private void execute ()
      throws Exception {

      Object attr = connection.getAttribute(new ObjectName(object), attribute);

      if (attr instanceof CompositeDataSupport) {

         CompositeDataSupport cds = (CompositeDataSupport)attr;

         if (attributeKey == null) {
            throw new CommandException("Attribute key is null for composed data " + object);
         }

         checkData = parseData(cds.get(attributeKey));
      }
      else {
         checkData = parseData(attr);
      }

      if (infoAttribute != null) {

         Object info_attr = infoAttribute.equals(attribute) ? attr : connection.getAttribute(new ObjectName(object), infoAttribute);

         if (infoKey != null && (info_attr instanceof CompositeDataSupport)) {

            CompositeDataSupport cds = (CompositeDataSupport)attr;

            infoData = cds.get(infoKey);
         }
         else {
            infoData = info_attr;
         }
      }
   }

   private long parseData (Object obj) {

      if (obj instanceof Number) {
         return ((Number)obj).longValue();
      }
      else {
         return Long.parseLong(obj.toString());
      }
   }

   private void parse (String[] args)
      throws CommandException {

      CommandSet commandSet;

      commandSet = CommandLineParser.parseCommands(args);

      if (commandSet.containsCommand("help")) {
         url = commandSet.getArgument("Options are like this...");
      }
      else {
         if (commandSet.containsCommand("U")) {
            url = commandSet.getArgument("U");
         }

         if (commandSet.containsCommand("O")) {
            object = commandSet.getArgument("O");
         }

         if (commandSet.containsCommand("A")) {
            attribute = commandSet.getArgument("A");
         }

         if (commandSet.containsCommand("I")) {
            infoAttribute = commandSet.getArgument("I");
         }

         if (commandSet.containsCommand("J")) {
            infoKey = commandSet.getArgument("J");
         }

         if (commandSet.containsCommand("K")) {
            attributeKey = commandSet.getArgument("K");
         }

         if (commandSet.containsCommand("v")) {
            verbatimLevel = VerbatimLevel.valueOf(commandSet.getArgument("v"));
         }

         if (commandSet.containsCommand("w")) {
            warning = Long.parseLong(commandSet.getArgument("w"));
         }

         if (commandSet.containsCommand("c")) {
            critical = Long.parseLong(commandSet.getArgument("c"));
         }

         if (url == null || object == null || attribute == null) {
            throw new CommandException("Required options not specified");
         }
      }
   }

   public static void main (String[] args) {

      CheckJMX checkJMX = new CheckJMX();

      try {
         checkJMX.parse(args);
         checkJMX.connect();
         checkJMX.execute();
         int status = checkJMX.report(System.out);
         System.exit(status);
      }
      catch (Exception ex) {
         int status = checkJMX.report(ex, System.out);
         System.exit(status);
      }
      finally {
         try {
            checkJMX.disconnect();
         }
         catch (IOException e) {
            int status = checkJMX.report(e, System.out);
            System.exit(status);
         }
      }
   }
}

