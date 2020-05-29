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

import java.util.HashSet;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import com.overzealous.remark.util.BlockWriter;
import com.overzealous.remark.util.StringUtils;

/**
 * Handles header nodes (h1 through h6)
 * 
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class Heading extends AbstractNodeHandler {

   /**
    * Renders a header node (h1..h6). If enabled, also handles the headerID
    * attribute.
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
      int depth = Integer.parseInt(node.tagName().substring(1, 2));
      Set<Node> nodeSet = new HashSet<Node>();
      // test to skip empty headers
      String inlineContent = converter
         .getInlineContent(this, node, pw, baseUri, domain, level, searchLevel, nodeSet)
         .replace("\n", " ");
      if (nodeSet.size() != 0) {
         result = nodeSet.iterator().next();
      }
      if (inlineContent.trim().length() == 0) {
         return result;
      }
      BlockWriter out = converter.output;
      out.startBlock();
      String md = StringUtils.multiply('#', depth) + " " + inlineContent + " "
         + StringUtils.multiply('#', depth);
      if (converter.options.headerIds && node.hasAttr("id")) {
         md += String.format("    {#%s}", node.attr("id"));
         out.print(md);
         saveAnnotation(pw, level, node, md);
      } else {
         out.print(md);
         saveAnnotation(pw, level, node, md);
      }
      out.endBlock();
      return result;
   }
}
