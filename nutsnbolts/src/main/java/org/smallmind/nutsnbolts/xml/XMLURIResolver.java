package org.smallmind.nutsnbolts.xml;

import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.smallmind.nutsnbolts.resource.Resource;

public class XMLURIResolver implements URIResolver {

   private static XMLURIResolver URI_RESOLVER;

   private ProtocolResolver protocolResolver;

   public synchronized static XMLURIResolver getInstance () {

      if (URI_RESOLVER == null) {
         URI_RESOLVER = new XMLURIResolver(SmallMindProtocolResolver.getInstance());
      }

      return URI_RESOLVER;
   }

   public XMLURIResolver (ProtocolResolver protocolResolver) {

      this.protocolResolver = protocolResolver;
   }

   public Source resolve (String href, String baseHref)
      throws TransformerException {

      Resource uriResource;
      InputStream uriStream;

      try {
         if ((uriResource = protocolResolver.resolve(href)) != null) {
            if ((uriStream = uriResource.getInputStream()) != null) {
               return new StreamSource(uriStream);
            }
         }
      }
      catch (Exception exception) {
         throw new TransformerException(exception);
      }

      return null;
   }

}
