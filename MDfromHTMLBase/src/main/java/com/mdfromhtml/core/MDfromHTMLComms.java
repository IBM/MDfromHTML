/**
 * (c) Copyright 2020 IBM Corporation
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

package com.mdfromhtml.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.Iterator;

import com.api.json.JSON;
import com.api.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Communications utilities
 */
public class MDfromHTMLComms {

   static boolean s_debug = false; // true;

   static Gson s_gson = new GsonBuilder().create();

   static public JSONObject sendRequest(String method, JSONObject service,
      JSONObject params) throws Exception {
      JsonObject serviceObj = s_gson.fromJson(service.toString(),
         JsonObject.class);
      JsonObject parameters = s_gson.fromJson(params.toString(),
         JsonObject.class);
      return (JSONObject) JSON
         .parse(sendRequest(method, serviceObj, parameters).toString());
   }

   static public ObjectNode sendRequest(String method, ObjectNode serviceObj,
      ObjectNode params) throws Exception {
      JsonObject parameters = s_gson.fromJson(params.toString(),
         JsonObject.class);
      return sendRequest(method, serviceObj, parameters);
   }

   static public JsonObject sendRequest(String method, JsonObject serviceObj,
      JsonObject params) throws Exception {
      if (method == null) {
         throw new Exception("The method is null.");
      }
      if (serviceObj == null) {
         throw new Exception("The serviceObj is null");
      }
      if (params == null) {
         throw new Exception("The params object is null");
      }
      ObjectNode service = (ObjectNode) new ObjectMapper()
         .readTree(serviceObj.toString());
      ObjectNode response = sendRequest(method, service, params);
      return s_gson.fromJson(response.toString(), JsonObject.class);
   }

   static public ObjectNode sendRequest(String method, ObjectNode serviceObj,
      JsonObject params) throws Exception {
      if (method == null) {
         throw new Exception("The method is null.");
      }
      if (serviceObj == null) {
         throw new Exception("The serviceObj is null");
      }
      if (params == null) {
         throw new Exception("The params object is null");
      }
      String protocol = getStringFromObject(serviceObj, "protocol").trim();
      String domain = getStringFromObject(serviceObj, "domain").trim();
      // allow number or string of number
      JsonNode portnumberElt = serviceObj.get("portnumber");
      if (portnumberElt == null) {
         throw new Exception("The portnumber in the service is missing.");
      }
      String portnumber = "";
      if (portnumberElt.isValueNode()) {
         portnumber = portnumberElt.asText().trim();
      } else {
         throw new Exception(
            "The portnumber is not a String of a number, nor a Number.");
      }
      String endpoint = getStringFromObject(serviceObj, "endpoint", "").trim();
      String username = getStringFromObject(serviceObj, "username").trim();
      String password = getStringFromObject(serviceObj, "password").trim();
      String apitimeout = getStringFromObject(serviceObj, "apitimeout").trim();
      return sendRequest(method, protocol, domain, portnumber, endpoint,
         username, password, apitimeout, params);
   }

   static public ObjectNode sendRequest(String method, String protocol,
      String domain, String portnumber, String endpoint, String username,
      String password, String apitimeout, JsonObject params) throws Exception {
      if (method == null) {
         throw new Exception("The method is null.");
      }
      method = method.trim();
      if (method.length() == 0) {
         throw new Exception("The method is empty.");
      }
      if (params == null) {
         throw new Exception("The params object is null");
      }
      int timeout = 10000;
      try {
         timeout = new Integer(apitimeout);
         if (timeout < 0) {
            throw new Exception(
               "The apitimeout is less than zero milliseconds.");
         }
      } catch (NumberFormatException nfe) {
         throw new Exception(
            "The apitimeout is not a positive integer of milliseconds.");
      }
      ObjectNode responseObj = null;
      String url = protocol + "://" + domain;
      if (portnumber != null && portnumber.length() > 0) {
         url += ":" + portnumber;
      }
      boolean needsQuestionMark = (endpoint.indexOf("?") == -1);
      // Note: assume endpoint entered with appropriate encoding
      url += endpoint;
      if ("GET".equals(method)) {
         // move parameters to URL query string
         String key = null;
         JsonElement value;
         boolean first = true;
         StringBuffer sb = new StringBuffer();
         if (params.size() > 0) {
            for (Iterator<String> it = params.keySet().iterator(); it
               .hasNext();) {
               if (first) {
                  if (needsQuestionMark) {
                     sb.append("?");
                  } else {
                     // assume appending additional parameters
                     sb.append("&");
                  }
                  first = false;
               } else {
                  sb.append("&");
               }
               key = it.next();
               sb.append(key);
               sb.append("=");
               value = params.get(key);
               sb.append(s_gson.toJson(value));
            }
            url += URLEncoder.encode(sb.toString(), "UTF-8");
         }
      }
      if (s_debug) {
         System.out.println("URL: " + url);
      }
      URL obj = new URL(url);
      HttpURLConnection.setFollowRedirects(true);
      HttpURLConnection serviceConnection = (HttpURLConnection) obj
         .openConnection();
      serviceConnection.setRequestMethod(method);
      if (username != null && password != null) {
         // set up authentication header
         String auth = username + ":" + password;
         // Note: do not use the Base64.getUrlEncoder for authentication
         byte[] authEncBytes = Base64.getEncoder().encode(auth.getBytes());
         String authStringEnc = new String(authEncBytes);
         authStringEnc = "Basic " + authStringEnc;
         serviceConnection.setRequestProperty("Authorization", authStringEnc);
      }
      serviceConnection.setRequestProperty("Content-Type", "application/json");
      serviceConnection.setDoOutput(true);

      OutputStream os = serviceConnection.getOutputStream();
      serviceConnection.setConnectTimeout(timeout);
      if ("GET".equals(method)) {
         os.write(new byte[0]);
      } else {
         os.write(params.toString().getBytes());
      }
      os.flush();
      os.close();
      int responseCode = serviceConnection.getResponseCode();
      String responseMsg = serviceConnection.getResponseMessage();
      if (s_debug) {
         System.out
            .println("Returned code " + responseCode + " " + responseMsg);
      }
      if (responseCode >= 200 && responseCode < 299) { // success
         BufferedReader in = new BufferedReader(
            new InputStreamReader(serviceConnection.getInputStream()));
         String inputLine;
         StringBuffer response = new StringBuffer();
         while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
         }
         in.close();
         ObjectMapper mapper = new ObjectMapper();
         // convert request to JsonNode
         JsonNode responseNode = mapper.readTree(response.toString());
         if (responseNode.isObject()) {
            responseObj = (ObjectNode) responseNode;
         }
      } else {
         responseObj = JsonNodeFactory.instance.objectNode();
         responseObj.put("errorCode", responseCode);
         responseObj.put("errorMsg", responseMsg);
      }
      return responseObj;
   };

   static public ArrayNode getArrayFromObject(ObjectNode obj, String key)
      throws Exception {
      JsonNode test = obj.get(key);
      if (test == null) {
         throw new Exception("The " + key + " is missing.");
      }
      if (test.isArray()) {
         return (ArrayNode) test;
      }
      throw new Exception("The value for " + key + " is not an Array.");
   };

   static public boolean getBooleanFromObject(ObjectNode obj, String key)
      throws Exception {
      JsonNode test = obj.get(key);
      if (test == null) {
         throw new Exception("The " + key + " is missing.");
      }
      if (test.isBoolean()) {
         return test.asBoolean();
      }
      throw new Exception("The value for " + key + " is not a Boolean.");
   }

   static public ObjectNode getNonNullObjectFromNode(JsonNode node)
      throws Exception {
      if (node == null) {
         throw new Exception("Node is null.");
      }
      if (node.isObject()) {
         return (ObjectNode) node;
      }
      throw new Exception("Node is not an object.");
   }

   static public ObjectNode getObjectFromObject(ObjectNode obj, String key)
      throws Exception {
      JsonNode test = obj.get(key);
      if (test == null) {
         throw new Exception("The " + key + " is missing.");
      }
      if (test.isObject()) {
         return (ObjectNode) test;
      }
      throw new Exception("The value for " + key + " is not an Object.");
   }

   static public String getStringFromObject(ObjectNode obj, String key)
      throws Exception {
      JsonNode test = obj.get(key);
      if (test == null) {
         throw new Exception("The " + key + " is missing.");
      }
      if (test.isTextual()) {
         return test.asText();
      }
      throw new Exception("The value for " + key + " is not a String.");
   }

   static public String getStringFromObject(ObjectNode obj, String key,
      String defaultValue) throws Exception {
      try {
         return getStringFromObject(obj, key);
      } catch (Exception e) {
         /**
          * TODO: define exceptions as format templates to avoid error due to
          * rewording
          */
         if (e.getLocalizedMessage().endsWith("is missing.")) {
            return defaultValue;
         } else {
            throw e;
         }
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      String username = MDfromHTMLUtils.prompt("Enter username:");
      String password = MDfromHTMLUtils.prompt("Enter password:");
      // Note: do not use the Base64.getUrlEncoder for authentication
      byte[] authEncBytes = Base64.getEncoder().encode((username+":"+password).getBytes());
      String authStringEnc = new String(authEncBytes);
      System.out.println("Authorization: Basic " + authStringEnc);
   }

}
