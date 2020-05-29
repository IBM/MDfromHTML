/**
 * (c) Copyright 2019-2020 IBM Corporation
 * 1 New Orchard Road, 
 * Armonk, New York, 10504-1722
 * United States
 * +1 914 499 1900
 * support: Nathaniel Mills wnm3@us.ibm.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
 * Copyright 2011 OverZealous Creations, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.overzealous.remark.convert;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import com.overzealous.remark.util.BlockWriter;

/**
 * Handles img tags.
 * 
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class Image extends AbstractNodeHandler {

   /**
    * Creates a link reference to an image, and renders the correct output.
    *
    * @param parent
    *           The previous node walker, in case we just want to remove an
    *           element.
    * @param node
    *           Node to handle
    * @param converter
    *           Parent converter for this object.
    * @param pw
    *           Provenance Writer to receive provenance annotations mapping
    *           generated markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           the domain from the baseUri used to find domain specific
    *           filtering rules
    * @param level
    * @param searchLevel
    * @return the Node where level matches searchLevel, otherwise, returns null
    */
   public Node handleNode(NodeHandler parent, Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel) {
      Node result = null;
      String url = node.attr("src");
      if (url.startsWith("//")) {
         url = getProtocol(baseUri)+url;
      } else if (url.startsWith("#") || url.startsWith("/")) {
         url = baseUri+url;
      }
      url = converter.cleaner.cleanUrl(url);

      // added to use data-src in lieu of src if present
      if (node.attr("data-src").length() != 0) {
         url = node.attr("data-src");
         if (url.startsWith("//")) {
            url = getProtocol(baseUri)+url;
         } else if (url.startsWith("#") || url.startsWith("/")) {
            url = baseUri+url;
         }
         url = converter.cleaner.cleanUrl(url);
      }
      String alt = node.attr("alt");
      if (alt == null || alt.trim().length() == 0) {
         alt = node.attr("title");
         if (alt == null) {
            alt = "";
         }
      }
      alt = converter.cleaner.clean(alt.trim());
      if (converter.options.inlineLinks) {
         if (alt.length() == 0) {
            alt = "Image";
         }
         String md = String.format("![%s](%s)", alt, url);
         converter.output.print(md);
         saveAnnotation(pw, level, node, md);
      } else {
         String linkId = converter.addLink(url, alt, true);
         // give a usable description based on filename whenever possible
         if (alt.length() == 0) {
            alt = linkId;
         }
         BlockWriter out = converter.output;
         if (alt.equals(linkId)) {
            String md = String.format("![%s][]", linkId);
            out.print(md);
            saveAnnotation(pw, level, node, md);
         } else {
            String md = String.format("![%s][%s]", alt, linkId);
            out.print(md);
            saveAnnotation(pw, level, node, md);
         }
      }
      return result;
   }
   
   String getProtocol(String url) {
      int index = url.indexOf(":");
      if (index > 0) {
         return url.substring(0,index) + ":";
      }
      return "http:";
   }
}
