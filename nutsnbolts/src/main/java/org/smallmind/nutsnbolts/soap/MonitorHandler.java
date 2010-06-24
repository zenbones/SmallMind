package org.smallmind.nutsnbolts.soap;

import java.io.PrintStream;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class MonitorHandler implements SOAPHandler<SOAPMessageContext> {

   private PrintStream printStream;
   private MessageDirection messageDirection;

   public MonitorHandler () {

      this(System.out, MessageDirection.BOTH);
   }

   public MonitorHandler (PrintStream printStream) {

      this(printStream, MessageDirection.BOTH);
   }

   public MonitorHandler (MessageDirection messageDirection) {

      this(System.out, messageDirection);
   }

   public MonitorHandler (PrintStream printStream, MessageDirection messageDirection) {

      this.printStream = printStream;
      this.messageDirection = messageDirection;
   }

   public Set<QName> getHeaders () {

      return null;
   }

   public boolean handleMessage (SOAPMessageContext context) {

      try {
         switch (messageDirection) {
            case IN:
               if (!(Boolean)context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
                  context.getMessage().writeTo(printStream);
                  printStream.println();
               }
               break;
            case OUT:
               if ((Boolean)context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
                  context.getMessage().writeTo(printStream);
                  printStream.println();
               }
               break;
            case BOTH:
               context.getMessage().writeTo(printStream);
               printStream.println();
               break;
            default:
               throw new UnknownSwitchCaseException(messageDirection.name());
         }
      }
      catch (Exception exception) {
         throw new RuntimeException(exception);
      }

      return true;
   }

   public boolean handleFault (SOAPMessageContext context) {

      try {
         context.getMessage().writeTo(printStream);
         printStream.println();
      }
      catch (Exception exception) {
         throw new RuntimeException(exception);
      }

      return true;
   }

   public void close (MessageContext context) {
   }

   protected void finalize () {

      printStream.close();
   }
}