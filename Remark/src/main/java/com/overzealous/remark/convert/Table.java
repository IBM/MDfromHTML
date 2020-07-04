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
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import com.overzealous.remark.Options;
import com.overzealous.remark.util.MarkdownTable;
import com.overzealous.remark.util.MarkdownTableCell;
import com.overzealous.remark.util.MarkdownTableHeaderCell;

/**
 * @author Phil DeJarnett
 * @author Nathaniel Mills modifications for provenance and level tracking
 */
public class Table extends AbstractNodeHandler {

   private static final Pattern STYLE_ALIGNMENT_PATTERN = Pattern
      .compile("text-align:\\s*([a-z]+)", Pattern.CASE_INSENSITIVE);

   public Node handleNode(NodeHandler parent, Element node,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel) {
      Node result = null;
      MarkdownTable table = new MarkdownTable();
      int depthLevel = 0;
      String nextLevel = level+".";
      // loop over every direct child of the table node.
      for (final Element child : node.children()) {
         depthLevel++;
         if (searchLevel != null && searchLevel.equals(nextLevel+depthLevel)) {
            return child;
         }
         if (child.tagName().equals("thead")) {
            // handle explicitly declared header sections
            int nextDepthLevel = 0;
            String nextNextLevel = nextLevel+depthLevel+".";
            for (final Element headerRow : child.children()) {
               nextDepthLevel++;
               if (searchLevel != null && searchLevel.equals(nextNextLevel+nextDepthLevel)) {
                  return headerRow;
               }
               result = processHeaderRow(table.addHeaderRow(), headerRow, converter, pw,
                  baseUri, domain, nextNextLevel+nextDepthLevel, searchLevel);
               if (result != null) {
                  return result;
               }
            }

         } else if (child.tagName().equals("tbody")
            || child.tagName().equals("tfoot")) {
            // handle body or foot sections - note: there's no special handling
            // for tfoot
            int nextDepthLevel = 0;
            String nextNextLevel = nextLevel+depthLevel+".";
            for (final Element bodyRow : child.children()) {
               nextDepthLevel++;
               if (searchLevel != null && searchLevel.equals(nextNextLevel+nextDepthLevel)) {
                  return bodyRow;
               }
               result = processRow(table.addBodyRow(), bodyRow, converter, pw, baseUri, domain,
                  nextNextLevel+nextDepthLevel, searchLevel);
               if (result != null) {
                  return result;
               }
            }

         } else if (child.tagName().equals("tr")) {
            // Hrm, a row was added outside a valid table body or header...
            if (!child.children().isEmpty()) {
               if (child.children().get(0).tagName().equals("th")) {
                  // handle manual TH cells
                  if (searchLevel != null && searchLevel.equals(nextLevel+depthLevel+"."+1)) {
                     return child;
                  }
                  result = processHeaderRow(table.addHeaderRow(), child, converter, pw, baseUri, domain,
                     nextLevel+depthLevel+"."+1, searchLevel);
                  if (result != null) {
                     return result;
                  }

               } else {
                  // OK, must be a table row.
                  result = processRow(table.addBodyRow(), child, converter, pw, baseUri, domain,
                     nextLevel+depthLevel, searchLevel);
                  if (result != null) {
                     return result;
                  }
               }
            }
         }
      }

      // OK, now render this sucker
      Options.Tables opts = converter.options.getTables();
      converter.output.startBlock();
      table.renderTable(converter.output, opts.isColspanEnabled(),
         opts.isRenderedAsCode());
      converter.output.endBlock();
      return result;
   }

   private Node processHeaderRow(List<MarkdownTableHeaderCell> row, Element tableRow,
         DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
         String level, String searchLevel) {
         Node result = null;
         int depthLevel = 0;
         String nextLevel = level+"~";
         for (final Element cell : tableRow.children()) {
            depthLevel++;
            if (searchLevel != null && searchLevel.equals(nextLevel+depthLevel)) {
               return cell;
            }
            Set<Node>nodeSet = new HashSet<Node>();
            String contents = converter.getInlineContent(this, cell, true, pw,
               baseUri, domain, level, searchLevel, nodeSet);
            if (nodeSet.size() != 0) {
               return nodeSet.iterator().next();
            }
            row.add(new MarkdownTableHeaderCell(contents, getAlignment(cell),
               getColspan(cell)));
            saveAnnotation(pw, nextLevel+depthLevel, cell, contents.replaceAll("\n"," "));
         }
         return result;
      }

   private Node processRow(List<MarkdownTableCell> row, Element tableRow,
      DocumentConverter converter, ProvenanceWriter pw, String baseUri, String domain,
      String level, String searchLevel) {
      Node result = null;
      int depthLevel = 0;
      String nextLevel = level+"~";
      for (final Element cell : tableRow.children()) {
         depthLevel++;
         if (searchLevel != null && searchLevel.equals(nextLevel+depthLevel)) {
            return cell;
         }
         Set<Node>nodeSet = new HashSet<Node>();
         String contents = converter.getInlineContent(this, cell, true, pw,
            baseUri, domain, level, searchLevel, nodeSet);
         if (nodeSet.size() != 0) {
            return nodeSet.iterator().next();
         }
         row.add(new MarkdownTableCell(contents, getAlignment(cell),
            getColspan(cell)));
         saveAnnotation(pw, nextLevel+depthLevel, cell, contents.replaceAll("\n"," "));
      }
      return result;
   }

   private MarkdownTable.Alignment getAlignment(Element cell) {
      MarkdownTable.Alignment alignment = MarkdownTable.Alignment.LEFT;
      String alignmentString = null;
      if (cell.hasAttr("align")) {
         alignmentString = cell.attr("align").toLowerCase();
      } else if (cell.hasAttr("style")) {
         Matcher m = STYLE_ALIGNMENT_PATTERN.matcher(cell.attr("style"));
         if (m.find()) {
            alignmentString = m.group(1).toLowerCase();
         }
      }
      if (alignmentString != null) {
         if (alignmentString.equals("center")) {
            alignment = MarkdownTable.Alignment.CENTER;
         } else if (alignmentString.equals("right")) {
            alignment = MarkdownTable.Alignment.RIGHT;
         }
      }
      return alignment;
   }

   private int getColspan(Element cell) {
      int colspan = 1;
      if (cell.hasAttr("colspan")) {
         try {
            colspan = Integer.parseInt(cell.attr("colspan"));
         } catch (NumberFormatException ex) {
            // ignore invalid numbers
         }
      }
      return colspan;
   }
}
