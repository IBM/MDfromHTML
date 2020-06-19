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
 * Handles ol and ul lists.
 * 
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class List extends AbstractNodeHandler {

	public Node handleNode(NodeHandler parent, Element node, DocumentConverter converter, ProvenanceWriter pw,
			String baseUri, String domain, String level, String searchLevel) {
		Node result = null;
		// the first node doesn't get a linebreak
		boolean first = true;
		// if this is an ol, it's numbered.
		boolean numericList = node.tagName().equals("ol");
		// keep track of where we are in the list.
		int listCounter = 1;

		// we need to store this, because we're going to replace it for each li
		// below (for padding).
		BlockWriter parentWriter = converter.output;
		parentWriter.startBlock();
		int depthLevel = 0;
		String nextLevel = level + ".";
		for (final Element child : node.children()) {
			depthLevel++;
			if (searchLevel != null && searchLevel.equals(nextLevel + depthLevel)) {
				return child;
			}
			String md = "";
			// handle linebreaks between li's
			if (first) {
				first = false;
			} else {
				parentWriter.println();
				// md += "\n";
			}
			if (child.tag() != null && child.tagName().equals("br")) {
				if (converter.options.hardwraps) {
					// do nothing (below breaks other tests so would need additional tests)
					// parentWriter.print("\n");
				}
				
			} else {
				// handle starting character
				if (numericList) {
					md += listCounter + ". ";
					if (listCounter < 10) {
						md += " ";
					}
					parentWriter.print(md);
					saveAnnotation(pw, level, node, md);
				} else {
					parentWriter.print(" *  ");
					saveAnnotation(pw, level, node, " *  ");
				}
			}
			// now, recurse downward, padding the beginning of each line so it
			// looks nice.
			converter.output = new BlockWriter(parentWriter);
			converter.output.setPrependNewlineString("    ", true);
			result = converter.walkNodes(this, child, converter.blockNodes, pw, baseUri, domain, nextLevel + depthLevel,
					searchLevel);
			if (result != null) {
				return result;
			}
			if (child.tag() != null && child.tagName().equals("br")) {
				; // do nothing
			} else {
				listCounter++;
			}
		}
		// cleanup
		parentWriter.endBlock();
		converter.output = parentWriter;
		return result;
	}
}
