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

package com.mdfromhtml.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.api.json.JSONArray;
import com.api.json.JSONObject;
import com.mdfromhtml.core.MDfromHTMLBASE64Codec;
import com.mdfromhtml.core.MDfromHTMLUtils;

public class MDfromHTMLServiceUtil implements Serializable {

   static public String HTTP_RESPONSE = "Response";
   static public String HTTP_RETURN_CODE = "ReturnCode";

   // HTTP Authentication
   public static final String AUTHORIZATION = "Authorization";
   public static final String AUTHORIZATION_ADMIN_USER = "MDfromHTMLadmin";
   public static final String AUTHORIZATION_CREDENTIALS = "YnNuYWRtaW46YnNuaXNmdW4=";
   // ildadmin:ildisfun
   public static final byte[] AUTHORIZATION_CREDENTIALS_DECODED = "MDfromHTMLadmin:MDfromHTMLisfun"
      .getBytes();
   public static final String AUTHORIZATION_PREFIX = "Basic ";
   public static final String AUTHORIZATION_LOGIN = AUTHORIZATION_PREFIX
      + AUTHORIZATION_CREDENTIALS;
   public static final String AUTHORIZATION_SESSION_PREFIX = AUTHORIZATION_ADMIN_USER
      + ":";
   public static final int AUTHORIZATION_SESSION_ID_OFFSET = (AUTHORIZATION_SESSION_PREFIX)
      .length();
   static boolean debug = false; // true; // to turn on

   // HTTP header extensions
   // session cookie variables
   public static final String HTTP_HEADER_SESSION_ID = "X-Session-ID";

   // user id request header
   public static final String HTTP_HEADER_USER_ID = "X-User-ID";
   private static final long serialVersionUID = -3804636662225945449L;
   public static final String SESSION_COOKIE_APPL_NAME = "MDfromHTML";
   public static final String SESSION_COOKIE_DOMAIN = null;
   public static final int SESSION_COOKIE_MAX_AGE = 86400; // 24 hours
   public static final String SESSION_COOKIE_NAME = "session";
   public static final String SESSION_COOKIE_PATH = null;
   public static final boolean SESSION_COOKIE_SECURE = false;
   public static final int SESSION_COOKIE_VERSION = 1;
   public static final String USER_COOKIE_APPL_NAME = "MDfromHTMLUser";
   public static final String USER_COOKIE_DOMAIN = null;
   public static final int USER_COOKIE_MAX_AGE = 86400; // 24 hours
   public static final String USER_COOKIE_NAME = "user";
   public static final String USER_COOKIE_PATH = null;
   public static final boolean USER_COOKIE_SECURE = false;
   public static final int USER_COOKIE_VERSION = 1;

   /**
    * Creates a response to contain an error message by the passed
    * ildResponseCodes object, and the passed errorMessage (if one exists (e.g.,
    * is not null)).
    * 
    * @param errorMessage
    * @param MDfromHTMLResponseCodes
    * @return response
    */
   public static Response getErrorResponse(String errorMessage,
      MDfromHTMLResponseCodes MDfromHTMLResponseCodes) {
      if (MDfromHTMLResponseCodes == null) {
         MDfromHTMLResponseCodes = com.mdfromhtml.services.MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR;
      }
      JSONObject errorContentsObj = new JSONObject();
      errorContentsObj.put("code", MDfromHTMLResponseCodes.label());
      errorContentsObj.put("description", MDfromHTMLResponseCodes.description());
      errorContentsObj.put("code", MDfromHTMLResponseCodes.code());
      errorContentsObj.put("src", "MDfromHTML");
      errorContentsObj.put("type", MDfromHTMLResponseCodes.label());
      if (!MDfromHTMLUtils.isUndefined(errorMessage)) {
         errorContentsObj.put("detail", errorMessage);
      }
      JSONObject errorObj = new JSONObject();
      errorObj.put("error", errorContentsObj);
      JSONObject ildErrorObj = errorObj;
      Response resp = Response.status(MDfromHTMLResponseCodes.respCode())
         .header("Access-Control-Allow-Credentials", "true")
         .header("Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization")
         .header("Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD")
         .header("Access-Control-Allow-Origin", "*")
         .header("Access_Control_Max_Age", 43200).entity(ildErrorObj.toString())
         .type(MediaType.APPLICATION_JSON).build();
      return resp;

   }

   /**
    * Creates a response to contain an error identified by the passed
    * responseCodes object, and the exception (if one exists (e.g., is not
    * null)).
    * 
    * @param exception
    * @param responseCodes
    * @return response
    */
   public static Response getErrorResponse(Throwable exception,
      MDfromHTMLResponseCodes responseCodes) {
      if (responseCodes == null) {
         responseCodes = MDfromHTMLResponseCodes.MDfromHTML_UNEXPECTED_ERROR;
      }
      JSONObject errorContentsObj = new JSONObject();
      errorContentsObj.put("code", responseCodes.label());
      errorContentsObj.put("description", responseCodes.description());
      errorContentsObj.put("code", responseCodes.code());
      errorContentsObj.put("src", "MDfromHTML");
      errorContentsObj.put("type", responseCodes.label());
      if (exception != null) {
         errorContentsObj.put("detail", exception.getLocalizedMessage());
      }

      // expand
      List<String> errorStack = new ArrayList<String>();
      if (exception != null) {
         Stack<Throwable> chainStack = new Stack<Throwable>();
         Throwable tTrigger = new Throwable(exception.toString());
         tTrigger.setStackTrace(exception.getStackTrace());
         errorStack.add("Caused by: " + tTrigger.getMessage());
         // add the stack trace content
         StackTraceElement[] stElements = tTrigger.getStackTrace();
         for (StackTraceElement stElt : stElements) {
            errorStack.add("  " + stElt.toString());
         }
         chainStack.push(tTrigger);
         // process the underlying chained exceptions
         Throwable tCause = exception.getCause();
         while (tCause != null) {
            Throwable tNew = new Throwable(tCause.getMessage());
            tNew.setStackTrace(tCause.getStackTrace());
            chainStack.push(tNew);
            errorStack.add("Caused by: " + tCause.getMessage());
            // add the stack trace content
            stElements = tCause.getStackTrace();
            for (StackTraceElement stElt : stElements) {
               errorStack.add("  " + stElt.toString());
            }
            tCause = tCause.getCause();
         }
         // unwind the stack
         // will at least have our child exception
         Throwable tChild = (Throwable) chainStack.pop();
         try {
            Throwable tParent = (Throwable) chainStack.pop();
            while (tParent != null) {
               tParent.initCause(tChild);
               tChild = tParent;
               tParent = (Throwable) chainStack.pop();
            }
         } catch (EmptyStackException e) {
            // expect to iterate until stack is empty
            // leaving the tChild pointing at the revised
         }
      }
      JSONArray errorStackArray = new JSONArray();
      for (String errorStackMsg : errorStack) {
         errorStackArray.add(errorStackMsg);
      }
      errorContentsObj.put("stackTrace", errorStackArray);
      JSONObject errorObj = new JSONObject();
      errorObj.put("error", errorContentsObj);
      JSONObject ildErrorObj = errorObj;
      if (debug) {
         try {
            System.out.println(ildErrorObj.serialize(true));
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      Response resp = Response.status(responseCodes.respCode())
         .header("Access-Control-Allow-Credentials", "true")
         .header("Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization")
         .header("Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD")
         .header("Access-Control-Allow-Origin", "*")
         .header("Access_Control_Max_Age", 43200).entity(ildErrorObj.toString())
         .type(MediaType.APPLICATION_JSON).build();
      return resp;

   }

   /**
    * Create a response to the REST Request
    * 
    * @param jsonMessage
    * @return response
    */
   public static Response getResponse(JSONObject jsonMessage) {
      JSONObject respObj = jsonMessage;
      Response resp = Response.status(MDfromHTMLResponseCodes.MDfromHTML_OKAY.respCode())
         .header("Access-Control-Allow-Credentials", "true")
         .header("Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization")
         .header("Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD")
         .header("Access-Control-Allow-Origin", "*")
         .header("Access_Control_Max_Age", 43200).entity(respObj.toString())
         .type(MediaType.APPLICATION_JSON).build();
      return resp;
   }

   /**
    * Create a a response to the REST Request
    * 
    * @param jsonMessage
    * @return response
    */
   public static Response getSessionResponse(JSONObject jsonMessage) {
      JSONObject respObj = jsonMessage;
      String session = "session";
      Response resp = Response.status(MDfromHTMLResponseCodes.MDfromHTML_OKAY.respCode())
         .header("Access-Control-Allow-Credentials", "true")
         .header("Access-Control-Allow-Headers",
            "origin, content-type, accept, authorization")
         .header("Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS, HEAD")
         .header("Access-Control-Allow-Origin", "*")
         .header("Access_Control_Max_Age", 43200)
         .header(AUTHORIZATION,
            AUTHORIZATION_PREFIX + MDfromHTMLBASE64Codec
               .encode((AUTHORIZATION_SESSION_PREFIX + session).getBytes()))
         .entity(respObj.toString()).type(MediaType.APPLICATION_JSON)
         .cookie(NewCookie.valueOf(SESSION_COOKIE_NAME + "=" + session
            + ";Max-Age=" + SESSION_COOKIE_MAX_AGE + ";Secure"))
         .build();
      return resp;

   }

   /**
    * Helper function to read either the inputStream or errorStream depending on
    * the response code.
    * 
    * @param con
    * @return String - the Response data
    * @throws IOException
    */
   static String readResponse(HttpURLConnection con) throws IOException {
      int responseCode = con.getResponseCode();
      BufferedReader in = null;
      StringBuffer response = new StringBuffer();

      if (responseCode < 200 || responseCode > 299) {
         in = new BufferedReader(new InputStreamReader(con.getErrorStream())); // error
      } else {
         in = new BufferedReader(new InputStreamReader(con.getInputStream())); // success
      }

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
         response.append(inputLine);
      }
      in.close();
      return response.toString();
   }

   /**
    * Sends an REST Delete request to the supplied URL with the serialized json
    * supplied
    * 
    * @param url
    * @param json
    * @return string response
    * @throws Exception
    */
   static public String sendRESTDelete(URL url, JSONObject json)
      throws Exception {
      try {
         return sendRESTDelete(url, json.serialize(), "Mozilla/5.0");
      } catch (Exception e) {
         throw new IOException("Can't serialize json object", e);
      }
   }

   /**
    * Sends an REST Delete request to the supplied URL with the supplied user
    * agent and deleteContent
    * 
    * @param url
    * @param deleteContent
    * @param userAgent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTDelete(URL url, String deleteContent,
      String userAgent) throws Exception {
      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("DELETE");
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setRequestProperty("Content-Type",
            "application/json; charset=utf-8");
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);

         // Send delete request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(deleteContent);
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

   /**
    * Sends an REST Get request to the specified URL
    * 
    * @param url
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTGet(URL url) throws Exception {
      return sendRESTGet(url, "Mozilla/5.0");
   }

   /**
    * Sends an REST Get request to the specified URL
    * 
    * @param url
    * @param userAgent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTGet(URL url, String userAgent)
      throws Exception {
      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         // optional default is GET
         con.setRequestMethod("GET");

         // add request header
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setInstanceFollowRedirects(true);

         String response = "";
         try {
            int responseCode = con.getResponseCode();
            response = readResponse(con); // read the data from the connection
                                          // input/error stream
            if (responseCode < 200 || responseCode > 299) {
               throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
                  + HTTP_RESPONSE + "=" + response);
            }
         } catch (Exception e) {
            throw new Exception(e);
         }
         return (response);

      } catch (Exception e) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e);
      }
   }

   /**
    * Sends an REST Get request to the specified URL appending the encoded json
    * object as a query string using Context-Type "application/json"
    * 
    * @param urlString
    * @param queryJSON
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTGetJSON(String urlString, JSONObject queryJSON)
      throws Exception {

      String queryString = "";
      if (queryJSON != null) {
         try {
            queryString = queryJSON.serialize();
         } catch (IOException e) {
            throw new Exception(e);
         }
      }
      if (queryString.length() > 0) {
         if (urlString.endsWith("/") == false) {
            urlString = urlString + "/";
         }
         try {
            urlString = urlString + URLEncoder.encode(queryString, "UTF-8");
         } catch (UnsupportedEncodingException e) {
            throw new Exception(e);
         }
      }
      URL url;
      try {
         url = new URL(urlString);
      } catch (MalformedURLException e) {
         throw new Exception("\"" + urlString + "\" creates a malformed URL",
            e);
      }
      return sendRESTGetJSON(url, "Mozilla/5.0");
   }

   /**
    * Sends an REST Get request to the specified URL using Context-Type
    * "application/json"
    * 
    * @param url
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTGetJSON(URL url) throws Exception {
      return sendRESTGetJSON(url, "Mozilla/5.0");
   }

   /**
    * Sends an REST Get request to the specified URL using Context-Type
    * "application/json"
    * 
    * @param url
    * @param userAgent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTGetJSON(URL url, String userAgent)
      throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         // optional default is GET
         con.setRequestMethod("GET");

         // add request header
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setRequestProperty("Content_Type",
            "application/json; charset=utf-8");
         con.setInstanceFollowRedirects(true);

         String response = "";
         try {
            int responseCode = con.getResponseCode();
            response = readResponse(con); // read the data from the connection
                                          // input/error stream
            if (responseCode < 200 || responseCode > 299) {
               throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
                  + HTTP_RESPONSE + "=" + response); // error
            }
         } catch (Exception e) {
            throw new Exception(e);
         }
         return (response);

      } catch (Exception e) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e);
      }
   }

   /**
    * Sends an REST Patch request to the supplied URL with the serialized json
    * supplied
    * 
    * @param url
    * @param json
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPatch(URL url, JSONObject json)
      throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("PATCH");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Content_Type",
            "application/json; charset=utf-8");
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);

         // Send patch request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(json.toString());
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

   /**
    * Sends an REST Patch request to the supplied URL with the supplied
    * patchContent
    *
    * @param url
    * @param patchContent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPatch(URL url, String patchContent)
      throws Exception {
      return sendRESTPatch(url, patchContent, "Mozilla/5.0");
   }

   /**
    * Sends an REST Patch request to the supplied URL with the supplied user
    * agent and patchContent
    * 
    * @param url
    * @param patchContent
    * @param userAgent
    * @return string response
    * @throws Exception
    */
   static public String sendRESTPatch(URL url, String patchContent,
      String userAgent) throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("PATCH");
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);

         // Send patch request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(patchContent);
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

   /**
    * Sends an REST Post request to the supplied URL with the serialized json
    * supplied
    * 
    * @param url
    * @param json
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPost(URL url, JSONObject json)
      throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("POST");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Content-Type",
            "application/json; charset=utf-8");
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);

         // Send post request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(json.toString());
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }
   }

   /**
    * Sends an REST Post request to the supplied URL with the supplied
    * postContent
    * 
    * @param url
    * @param postContent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPost(URL url, String postContent)
      throws Exception {
      return sendRESTPost(url, postContent, "Mozilla/5.0");
   }

   /**
    * Sends an REST Post request ot the supplied URL with the supplied user
    * agent and postContent
    * 
    * @param url
    * @param postContent
    * @param userAgent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPost(URL url, String postContent,
      String userAgent) throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("POST");
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);

         // Send post request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(postContent);
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

   /**
    * Sends an REST Put request to the supplied URL with the serialized json
    * supplied
    * 
    * @param url
    * @param json
    * @return string response
    * @throws Exception
    */
   static public String sendRESTPut(URL url, JSONObject json) throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("PUT");
         con.setRequestProperty("User-Agent", "Mozilla/5.0");
         con.setRequestProperty("Content-Type",
            "application/json; charset=utf-8");
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);
         // Send put request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(json.toString());
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {
            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new IOException(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

   /**
    * Sends an REST Put request to the supplied URL with the supplied putContent
    * 
    * @param url
    * @param putContent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPut(URL url, String putContent)
      throws Exception {
      return sendRESTPut(url, putContent, "Mozilla/5.0");
   }

   /**
    * Sends an REST Put request ot the supplied URL with the supplied user agent
    * and putContent
    * 
    * @param url
    * @param putContent
    * @param userAgent
    * @return response to REST request
    * @throws Exception
    */
   static public String sendRESTPut(URL url, String putContent,
      String userAgent) throws Exception {

      HttpURLConnection con;
      try {
         con = (HttpURLConnection) url.openConnection();

         con.setRequestMethod("PUT");
         if (MDfromHTMLUtils.isUndefined(userAgent)) {
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
         } else {
            con.setRequestProperty("User-Agent", userAgent);
         }
         con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
         con.setInstanceFollowRedirects(true);
         // Send put request
         con.setDoOutput(true);
         DataOutputStream wr = new DataOutputStream(con.getOutputStream());
         wr.writeBytes(putContent);
         wr.flush();
         wr.close();

         int responseCode = con.getResponseCode();
         String response = readResponse(con); // read the data from the
                                              // connection input/error stream
         if (responseCode < 200 || responseCode > 299) {

            throw new Exception(HTTP_RETURN_CODE + "=" + responseCode + ","
               + HTTP_RESPONSE + "=" + response);
         }
         return (response);
      } catch (Exception e1) {
         throw new Exception(
            "Can not open connection to " + url.toExternalForm(), e1);
      }

   }

}
