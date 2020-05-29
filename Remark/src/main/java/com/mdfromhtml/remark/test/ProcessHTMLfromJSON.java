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

package com.mdfromhtml.remark.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.api.json.JSON;
import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLUtils;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.ProvenanceWriter;

/**
 * Test class for calling the Remark APIs
 * 
 * @author Nathaniel Mills
 *
 */
public class ProcessHTMLfromJSON {

   public static void main(String[] args) {
      // hardcoded for now...
      JSONArray htmlList = new JSONArray();
      try {
         File testFile = new File("src/test/resources/Archive0001.json");
         JSONObject filejson = (JSONObject) JSON
            .parse(new FileInputStream(testFile));
         htmlList = (JSONArray) filejson.get("captureArray");
      } catch (IOException e1) {
         System.out
            .println("Can not find file \"url_html_info.json\". Goodbye. "
               + e1.getLocalizedMessage());
         System.exit(-1);
      }
      String url = null;
      String html = null;
      String outputFileName = null;
      Remark remark;
      int htmlCounter = 0;
      ProvenanceWriter annotations = null;
      try {
         remark = new Remark();
         for (Object obj : htmlList) {
            htmlCounter++;
            outputFileName = "Archive0001" + ".md";
            JSONObject htmlObj = (JSONObject) obj;
            url = (String) htmlObj.get("url");
            html = (String) htmlObj.get("html");
            System.out.println("\n" + url + "\n" + outputFileName);
            String htmlOutputFileName = "Archive0001" + "_"
               + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + ".html";
            MDfromHTMLUtils.saveTextFile(htmlOutputFileName, html);
            Document doc = Jsoup.parse(html, url);

            String formattedHTML = doc.toString(); // HTMLFormatter.formatHTML(html);
            String formattedHTMLOutputFileName = "Archive0001" + "_"
               + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + "_formatted.html";
            MDfromHTMLUtils.saveTextFile(formattedHTMLOutputFileName, formattedHTML);

            String markdownOutputFileName = "Archive0001" + "_"
               + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + ".md";

            String provenanceOutputFileName = "Archive0001" + "_"
               + MDfromHTMLUtils.padLeftZero(htmlCounter, 3) + ".annotations";

            File provenanceOutputFile = new File(provenanceOutputFileName);
            if (provenanceOutputFile.exists()) {
               provenanceOutputFile.delete();
            }
            annotations = new ProvenanceWriter(formattedHTMLOutputFileName,
               markdownOutputFileName, remark.getHTMLFilters(), null, null,
               new FileWriter(provenanceOutputFile,true));
            String markdown = remark.convert(doc, annotations);
            MDfromHTMLUtils.saveTextFile(markdownOutputFileName, markdown);
         }
      } catch (Exception e1) {
         e1.printStackTrace();
      } finally {
         if (annotations != null) {
            try {
               annotations.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
