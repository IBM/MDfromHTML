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

/**
 * Handles paragraph (p) tags.
 * 
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class Paragraph extends AbstractNodeHandler {

   /**
    * Creates a standard text block, then walks down over inline nodes.
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
    * @param searchLevel
    * @return the Node where level matches searchLevel, otherwise, returns null
    */
   public Node handleNode(NodeHandler parent, Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel) {
      converter.output.startBlock();
      Node result = converter.walkNodes(this, node, converter.inlineNodes, pw,
         baseUri, domain, level, searchLevel);
      converter.output.endBlock();
      return result;
   }
}
