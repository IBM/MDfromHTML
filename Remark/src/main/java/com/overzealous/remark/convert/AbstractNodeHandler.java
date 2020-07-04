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

import java.io.IOException;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import com.overzealous.remark.util.BlockWriter;

/**
 * Contains basic implementations for handling text nodes and ignored HTML
 * elements.
 *
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public abstract class AbstractNodeHandler implements NodeHandler {

   /**
    * Handle a child text node. The default method, implemented here, is to
    * simply write the cleaned text directly.
    *
    * @param node
    *           Node to handle
    * @param converter
    *           Parent converter for this object.
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           the domain from the baseUri used to find domain specific
    *           filtering rules
    * @param level
    */
   public void handleTextNode(TextNode node, DocumentConverter converter,
      ProvenanceWriter pw, String baseUri, String domain, String level) {
      String md = converter.cleaner.clean(node);
      md = md.replace("<", "&lt;");
      md = md.replace(">", "&gt;");
      
      converter.output.write(md);
      saveAnnotation(pw, level, node, md);
   }

   /**
    * Handle an ignored HTMLElement. The default method here is to either write
    * the HTMLElement as a block if it is a block element, or write it directly
    * if it is not.
    *
    * @param node
    *           Node to handle
    * @param converter
    *           Parent converter for this object.
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           the domain from the baseUri used to find domain specific
    *           filtering rules
    * @param level
    */
   public void handleIgnoredHTMLElement(Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level) {
      if (node.isBlock()) {
         converter.output.writeBlock(node.toString());
      } else {
         // Note: because this is an inline element, we want to make sure it
         // stays that way!
         // this means turning off prettyPrinting, so that JSoup doesn't add
         // unecessary spacing around
         // the child nodes.
         Document doc = node.ownerDocument();
         boolean oldPrettyPrint = doc.outputSettings().prettyPrint();
         doc.outputSettings().prettyPrint(false);
         converter.output.writeAsIs(node.toString());
         String md = node.toString();
         saveAnnotation(pw, level, node, md);
         doc.outputSettings().prettyPrint(oldPrettyPrint);
      }
   }

   /**
    * Recursively processes child nodes, and prepends the given string to the
    * output.
    * 
    * @param prepend
    *           String to prepend
    * @param node
    *           Starting Node
    * @param converter
    *           Parent document converter
    * @param nodes
    *           Map of valid nodes
    * @param pw
    *           Annotation Writer to receive annotations mapping generated
    *           markdown to document element(s)
    * @param baseUri
    *           the base URI needed to flesh out partial (local) image or href URL references
    * @param domain
    *           the domain from the baseUri used to find domain specific
    *           filtering rules
    * @param level
    *           The dotted tree notation for the location within the dom of the
    *           node being processed. e.g., The top level node (e.g., <body>)
    *           would be 1. Its first child element would be 1.1 and 2nd would
    *           be 1.2. The first childs first child would be 1.1.1. For inline
    *           expressions, we can introduce a different separator like the
    *           tilde. So, if an element is like: <a class="ocShareButton"
    *           data-bi-bhvr="SOCIALSHARE" data-bi-name="facebook"
    *           data-bi-slot="1"
    *           href="https://web.archive.org/web/20190411221943/https://www.facebook.com/sharer.php?u=https://support.office.com/en-us/article/install-skype-for-business-8a0d4da8-9d58-44f9-9759-5c8f340cb3fb"
    *           id="ocFacebookButton" ms.cmpgrp="Share"
    *           ms.ea_action="Goto"ms.ea_offer="SOC" ms.interactiontype="1"
    *           ms.pgarea="Body" target="_blank">
    *           <img alt="Facebook" class="ocArticleFooterImage" ms.cmpgrp=
    *           "content" ms.pgarea="Body" src=
    *           "/web/20190411221943im_/https://support.office.com/Images/SOC-Facebook.png">
    *           </a> Then the <a> tag would be x.y and its href would be x.y~1
    *           and the <img> would be x.y.1 and its src would be x.y.1~1. For
    *           text between tags we could use a carat as a separator.
    * @param searchLevel
    *           signals to stop dom walking if we have reached the search level,
    *           and return the node we are on. If null, no interuption occurs.
    * @return the Node where level matched searchLevel, otherwise, null
    */
   protected Node prependAndRecurse(String prepend, Element node,
      DocumentConverter converter, Map<String, NodeHandler> nodes,
      ProvenanceWriter pw, String baseUri, String domain, String level, String searchLevel) {
      Node result = null;
      BlockWriter oldOutput = converter.output;
      converter.output = new BlockWriter(oldOutput);
      converter.output.setPrependNewlineString(prepend);
      result = converter.walkNodes(this, node, nodes, pw, baseUri, domain, level,
         searchLevel);
      converter.output = oldOutput;
      return result;
   }

   /**
    * Save the annotation for the level, node, and markdown content printing any
    * exceptions that may occur
    * 
    * @param pw
    * @param level
    * @param node
    * @param markdown
    */
   protected void saveAnnotation(ProvenanceWriter pw, String level, Node node,
      String markdown) {
      if (pw != null) {
         try {
            pw.saveHTML2MD(level, node, markdown);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Save the filter reason annotation for the level, node, and filterReason content printing any
    * exceptions that may occur
    * 
    * @param pw
    * @param level
    * @param node
    * @param filterReason
    */
   protected void saveFilterAnnotation(ProvenanceWriter pw, String level, Node node,
      String filterReason) {
      if (pw != null) {
         try {
            pw.saveFilteredHTML(level, node, filterReason);
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }
}
