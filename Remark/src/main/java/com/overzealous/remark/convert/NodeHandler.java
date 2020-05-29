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
import org.jsoup.nodes.TextNode;

/**
 * Interface for classes that handle processing HTML Elements.
 * 
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public interface NodeHandler {

   /**
    * Handles an HTML Element node. This is where most of the work is done.
    *
    * Which NodeHandler is used is based on the tagName of the element.
    *
    * @param parent
    *           The previous node walker, in case we just want to remove an
    *           element.
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
    * @return the node where we matched level with searchLevel, otherwise null
    */
   public Node handleNode(NodeHandler parent, Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel);

   /**
    * Handle a child text node.
    *
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
    */
   public void handleTextNode(TextNode node, DocumentConverter converter,
      ProvenanceWriter pw, String baseUri, String domain, String level);

   /**
    * Handle an ignored HTMLElement.
    * 
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
    */
   public void handleIgnoredHTMLElement(Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level);

}
