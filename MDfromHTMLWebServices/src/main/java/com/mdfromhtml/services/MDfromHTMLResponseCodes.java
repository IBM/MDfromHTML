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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public enum MDfromHTMLResponseCodes {
      MDfromHTML_CLASS_NOT_FOUND(1007, "CLASS_NOT_FOUND", "Class not found",
         Response.Status.BAD_REQUEST), MDfromHTML_CONFIGURATION_ERROR(1014,
            "TEMPLATE_ERROR", "Request Template error",
            Response.Status.SERVICE_UNAVAILABLE), //
      MDfromHTML_MDfromHTML_OBJECT_NOT_FOUND(1009, "MDfromHTML_OBJECT_NOT_FOUND",
         "MDfromHTML JSONObject not found", Response.Status.NOT_FOUND), //
      MDfromHTML_INPUT_NOT_FOUND(1013, "MISSING_REQUEST_INPUT",
         "No request provided", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_JSON_DELETE_REQUEST(1004,
         "MDfromHTML_INVALID_JSON_DELETE_REQUEST",
         "Invalid JSON Syntax for Delete Request", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_JSON_GET_REQUEST(1001, "MDfromHTML_INVALID_JSON_GET_REQUEST",
         "Invalid JSON Syntax for Get Request", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_JSON_PATCH_REQUEST(1015, "MDfromHTML_INVALID_JSON_PATCH_REQUEST",
         "Invalid JSON Syntax for Patch Request", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_JSON_POST_REQUEST(1002, "MDfromHTML_INVALID_JSON_POST_REQUEST",
         "Invalid JSON Syntax for Post Request", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_JSON_PUT_REQUEST(1003, "MDfromHTML_INVALID_JSON_PUT_REQUEST",
         "Invalid JSON Syntax for Put Request", Response.Status.BAD_REQUEST), //
      MDfromHTML_INVALID_SESSION_ID(1011, "INVALID_SESSION_ID", "Invalid session id",
         Response.Status.BAD_REQUEST), //
      MDfromHTML_JSONOBJECT_NOT_FOUND(1010, "JSONOBJECT_NOT_FOUND",
         "JSONObject not found",
         Response.Status.NOT_FOUND), MDfromHTML_METHOD_NOT_FOUND(1008,
            "METHOD_NOT_FOUND", "Method not found",
            Response.Status.BAD_REQUEST), MDfromHTML_OKAY(1000, "MDfromHTML_OKAY", "Okay",
               Response.Status.OK), MDfromHTML_SERVICE_NOT_FOUND(1012,
                  "INVALID_SERVICE_NAME", "Service name not recognized",
                  Response.Status.BAD_REQUEST), MDfromHTML_SESSION_ID_NOT_FOUND(1011,
                     "SESSION_ID_NOT_FOUND", "Session id not found",
                     Response.Status.NOT_FOUND), MDfromHTML_UNEXPECTED_ERROR(1006,
                        "UNEXPECTED_ERROR", "Unexpected error",
                        Response.Status.BAD_REQUEST), MDfromHTML_UNSUPPORTED_ENCODING_TYPE(
                           1005, "UNSUPPORTED_ENCODING_TYPE",
                           "Unsupported Encoding Type",
                           Response.Status.BAD_REQUEST);

   private final Integer code;

   private final String description;

   private final String label;

   private final Status respCode;

   MDfromHTMLResponseCodes(final Integer code, final String label,
      final String description, Status respCode) {
      this.code = code;
      this.label = label;
      this.description = description;
      this.respCode = respCode;
   }

   public Integer code() {
      return code;
   }

   public String description() {
      return description;
   }

   public String label() {
      return label;
   }

   public Status respCode() {
      return respCode;
   }
}
