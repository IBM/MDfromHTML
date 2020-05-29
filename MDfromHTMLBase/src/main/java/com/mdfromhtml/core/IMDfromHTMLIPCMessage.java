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

import com.api.json.JSONObject;

/**
 * Interface for the MDfromHTML IPC Message to be sent (or received) across the MDfromHTML
 * Interprocess Communications Infrastructure. Messages may carry JSONObject or
 * byte[] content. Messages are identified by a topic (used for subscription), a
 * type (used for identifying how a message should be parsed and interpreted), a
 * create time, a sequence number where 0 implies the only message, 1 or greater
 * implies a chunk of a larger message (where {@link Integer#MAX_VALUE} acks as
 * a semaphore to signal the last of a sequence), and a flag describing if the
 * message content is a JSONObject or byte[].
 */

public interface IMDfromHTMLIPCMessage {

   /**
    * @return the enumerated value associated with this message
    * @see MDfromHTMLIPCVerbs
    */
   public MDfromHTMLIPCApplications getApplication();

   /**
    * @return the numeric identity of the application
    */
   public int getApplicationValue();

   /**
    * @return the message content as a JSONObject
    * @throws Exception
    */
   public JSONObject getMessage() throws Exception;

   /**
    * @return the message content as a byte[]
    */
   public byte[] getMessageBytes();

   /**
    * @return the publisher (creator) of this message
    */
   public String getPublisherName();

   /**
    * @return the sequence number of this message. Note: zero implies the only
    *         message, non-zero implies a part of a chunked message, with 65768
    *         signifying the last of the sequence of messages.
    */
   public int getSequence();

   /**
    * @return the session id related to the message content. If the content is
    *         not associated with a particular session, the ID returned is
    *         {@link MDfromHTMLID#UNDEFINED_ID} and corresponds with the String value in
    *         {@link MDfromHTMLConstants#UNDEFINED_ID}
    */
   public MDfromHTMLID getSessionID();

   /**
    * @return the original message creation time (not necessarily the same as
    *         the time it was published)
    */
   public long getTime();

   /**
    * @return the message topic
    */
   public String getTopic();

   /**
    * @return the message type. If the original was undefined, the type will
    *         return the {@link MDfromHTMLConstants#UNDEFINED_String}
    */
   public String getType();

   /**
    * Retrieve the numeric identity of the verb
    * 
    * @return the numeric identity of the verb
    */
   public int getVerbValue();

   /**
    * Retrieve the enumerated value associated with this message
    * 
    * @see MDfromHTMLIPCVerbs
    * @return the enumerated value associated with this message
    */
   public MDfromHTMLIPCVerbs getVerb();

   /**
    * Retrieve the user identification associated with this message
    * 
    * @return the user identification associated with this message
    */
   public MDfromHTMLID getUserID();

   /**
    * @return true if the message sequence is {@link Integer#MAX_VALUE}
    *         signaling the end of a chunked sequence of messages
    */
   public boolean isLastMessage();

   /**
    * @return true if the message sequence is zero signaling the message was not
    *         chunked
    */
   public boolean isOnlyMessage();

   /**
    * @return true if the message sequence is not zero (for an "chunked"
    *         message).
    */
   public boolean isSequence();

   /**
    * @return true if the original message was a String (as opposed to a byte[])
    */
   public boolean isJSON();

   /**
    * @return an abbreviated formatted version of the message (contains only the
    *         first 100 bytes of the payload)
    */
   public String toString();

   /**
    * @return formatted complete version of this message
    * @see #toString that is more useful for debugging
    */
   public String toStringFull();

   /**
    * @param formatJSON
    *           true if the message is to be formatted (if the message contains
    *           JSON), false if not.
    * @return formatted complete version of this message
    * @see #toString that is more useful for debugging
    */
   public String toStringFull(boolean formatJSON);
}
