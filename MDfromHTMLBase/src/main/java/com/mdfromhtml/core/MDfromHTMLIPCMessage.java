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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.api.json.JSON;
import com.api.json.JSONObject;

/**
 * Implementation of the MDfromHTML IPC Message to be sent (or received) across the MDfromHTML
 * Interprocess Communications Infrastructure. Messages may carry JSONObject or
 * byte[] content. Messages are identified by a topic (used for subscription), a
 * type (used for identifying how a message should be parsed and interpreted), a
 * create time, a sequence number where 0 implies the only message, 1 or greater
 * implies a chunk of a larger message (where {@link Integer#MAX_VALUE} acks as
 * a semaphore to signal the last of a sequence), and a flag describing if the
 * message content is a JSONObject or byte[].
 */
public class MDfromHTMLIPCMessage implements IMDfromHTMLIPCMessage, Serializable {

	/**
	 * Below overridden by a MDfromHTMLIPC.properties value
	 */
	static public int IPC_MAX_MESSAGE_SIZE = getIPCMaxMessageSize();

	static public int getIPCMaxMessageSize() {
		int maxSize = 327680;
		try {
			Properties props = MDfromHTMLUtils.getMDfromHTMLIPCProps();
			maxSize = new Integer(props.getProperty(MDfromHTMLConstants.IPC_MAX_MESSAGE_SIZE, "327680"));
		} catch (Exception e) {
			; // retains default 327680
		}
		return maxSize;
	}

	static private final long serialVersionUID = -1298716658259442515L;
	static {
		IPC_MAX_MESSAGE_SIZE = getIPCMaxMessageSize();
	}

	/**
	 * Converts the message format sent across the wire into a MDfromHTMLIPCMessage
	 * object
	 * 
	 * @param bytes
	 *            byte array carrying the message
	 * @return transformation of the passed byte array into a message
	 * @throws Exception
	 *             (Exception) if the bytes are null or not a
	 *             valid message format (ExceptionParse) if the message
	 *             content can not be transformed into a JSONObject
	 */
	static public MDfromHTMLIPCMessage fromByteArray(byte[] bytes) throws Exception {
		if (bytes == null || bytes.length < 8) {
			throw new Exception("Byte array is null or too short to be valid.");
		}
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		int headerLen = bb.getInt();
		if (headerLen < 0) {
			throw new Exception("Invalid header length in byte array.");
		}
		byte[] headerBytes = new byte[headerLen];
		bb.get(headerBytes);
		int bodyLen = bb.getInt();
		if (bodyLen < 0) {
			throw new Exception("Invalid body length in byte array.");
		}
		byte[] body = new byte[bodyLen];
		bb.get(body);
		String header = MDfromHTMLUtils.fromUTF8Bytes(headerBytes);
		String[] parts = header.split(MDfromHTMLConstants.MDfromHTML_DELIMITER, -1);
		// note Java split results in 10 as final delimiter has no entry unless
		// 2nd parameter -1 is used
		if (parts.length != 12) {
			throw new Exception("Malformed header in byte array.");
		}
		String publisherName = parts[1];
		MDfromHTMLID sessionID = MDfromHTMLID.getExistingID(parts[2]);
		MDfromHTMLIPCApplications appl = MDfromHTMLIPCApplications.fromValue(new Integer(parts[3]));
		String topic = parts[4];
		String type = parts[5];
		MDfromHTMLIPCVerbs verb = MDfromHTMLIPCVerbs.fromValue(new Integer(parts[6]));
		MDfromHTMLID userID = MDfromHTMLID.getExistingID(parts[7]);
		long time = new Long(parts[8]);
		int seq = new Integer(parts[9]);
		boolean isJSON = parts[10].compareToIgnoreCase("1") == 0;
		MDfromHTMLIPCMessage msg = null;
		if (isJSON && seq == 0) { // only message
			JSONObject jsonObj;
			String strObj = MDfromHTMLUtils.fromUTF8Bytes(body);
			try {
				// handle special case where original was not serializable
				if (MDfromHTMLConstants.ERRANT_JSON_STRING.compareToIgnoreCase(strObj) == 0) {
					strObj = "{}";
				}
				jsonObj = (JSONObject) JSON.parse(strObj);
			} catch (NullPointerException | IOException e) {
				throw new Exception("Error transforming data to JSONObject: \"" + strObj + "\"", e);
			}
			msg = new MDfromHTMLIPCMessage(publisherName, sessionID, appl, topic, type, verb, userID, jsonObj);
		} else {
			// treat as bytes for now to be reassembled later
			msg = new MDfromHTMLIPCMessage(publisherName, sessionID, appl, topic, type, verb, userID, body);
		}
		msg.setTime(time);
		msg.setSequence(seq);
		return msg;
	}

	/**
	 * Reassembles an array of sequenced messages into a single message
	 * 
	 * @param msgs
	 *            array of message chunks to be reassembled into a single
	 *            message
	 * @return the single message built from the pieces passed
	 * @throws Exception
	 *             if the message array is null or incomplete (e.g., missing a
	 *             sequence number)
	 */
	static public MDfromHTMLIPCMessage getAssembledMessage(MDfromHTMLIPCMessage[] msgs) throws Exception {
		if (msgs == null || msgs.length == 0) {
			throw new Exception("Can not assemble null or empty array of messages.");
		}
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		// create new byte array from array of messages
		String publisherName = null;
		MDfromHTMLID sessionID = MDfromHTMLID.UNDEFINED_ID;
		String topic = null;
		String type = null;
		MDfromHTMLIPCApplications appl = MDfromHTMLIPCApplications.UNDEFINED;
		MDfromHTMLIPCVerbs verb = MDfromHTMLIPCVerbs.UNDEFINED;
		MDfromHTMLID userID = MDfromHTMLID.UNDEFINED_ID;
		long msgTime = 0L;
		for (int i = 0; i < msgs.length; i++) {
			if (i == 0) {
				publisherName = msgs[i].getPublisherName();
				sessionID = msgs[i].getSessionID();
				topic = msgs[i].getTopic();
				type = msgs[i].getType();
				msgTime = msgs[i].getTime();
				appl = msgs[i].getApplication();
				verb = msgs[i].getVerb();
				userID = msgs[i].getUserID();
			}
			try {
				ba.write(msgs[i].getMessageBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// below sets _isJSON
		MDfromHTMLIPCMessage msg = new MDfromHTMLIPCMessage(publisherName, sessionID, appl, topic, type, verb, userID,
				ba.toByteArray());
		msg.setTime(msgTime);
		return msg;
	}

	/**
	 * Test rig for various methods
	 * 
	 * @param args
	 *            No arguments are required, but if a positive integer between 1
	 *            and 99 is passed, it is used to split up the default 100
	 *            character message into chunks.
	 */
	public static void main(String[] args) {
		int split = 9;
		try {
			if (args.length > 0) {
				split = new Integer(args[0]);
			}
			MDfromHTMLID sessionID = new MDfromHTMLID();
			MDfromHTMLID userID = new MDfromHTMLID();
			JSONObject testObj = new JSONObject();
			testObj.put("test",
					"0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
			MDfromHTMLIPCMessage msg = new MDfromHTMLIPCMessage(null, sessionID, MDfromHTMLIPCApplications.MDfromHTML, "topic1", "type1",
					MDfromHTMLIPCVerbs.UNDEFINED, userID, testObj);
			System.out.println("Split value is " + split);
			System.out.println("Start message:" + msg);
			MDfromHTMLIPCMessage[] msgs = msg.getSequencedMessages(split);
			String seqStr;
			for (MDfromHTMLIPCMessage xmsg : msgs) {
				seqStr = Integer.toString(xmsg.getSequence());
				if (xmsg.isLastMessage()) {
					seqStr = "Last";
				}
				System.out.println(seqStr + ":\t" + MDfromHTMLUtils.fromUTF8Bytes(xmsg.getMessageBytes()));
			}
			MDfromHTMLIPCMessage newMsg = MDfromHTMLIPCMessage.getAssembledMessage(msgs);
			System.out.println("Reassembled message is \"" + newMsg.getMessage() + "\"");
			byte[] msgBytes = toByteArray(newMsg);
			System.out.println("As bytes: " + MDfromHTMLUtils.hexEncode(msgBytes));
			try {
				newMsg = fromByteArray(msgBytes);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Final message:" + newMsg);
			System.out.println("Message as JSON:" + newMsg.toJson());
			try {
				System.out.println("Message from JSON:" + new MDfromHTMLIPCMessage(newMsg.toJson()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Transforms a message into the byte array used for transmission on the
	 * wire
	 * 
	 * @param msg
	 *            the message to be transformed into a byte array
	 * @return the byte array representing the content of the passed message
	 * @throws Exception
	 *             if the message is null
	 */
	static public byte[] toByteArray(MDfromHTMLIPCMessage msg) throws Exception {
		if (msg == null) {
			throw new Exception("Message is null.");
		}
		// capture non-message parts as a string
		StringBuffer sb = new StringBuffer();
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getPublisherName());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getSessionID());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getApplicationValue());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getTopic());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getType());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getVerbValue());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getUserID());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getTime());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.getSequence());
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		sb.append(msg.isJSON() ? "1" : "0");
		sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		ByteBuffer bb = null;
		byte[] header = MDfromHTMLUtils.toUTF8Bytes(sb.toString());
		byte[] body = msg.getMessageBytes();
		bb = ByteBuffer.allocate(4 + header.length + 4 + body.length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(header.length);
		bb.put(header);
		bb.putInt(body.length);
		bb.put(body);
		return bb.array();
	}

	/**
	 * Identifies the message as originating from a MDfromHTML application
	 */
	private MDfromHTMLIPCApplications _appl = MDfromHTMLIPCApplications.MDfromHTML;

	/**
	 * Time the original message was created
	 */
	private long _createTime = new MDfromHTMLDate().getTime();

	/**
	 * Whether or not the message contains a String
	 */
	private boolean _isJSON = true;

	/**
	 * The message content (converted to a UTF-8 byte array if the message
	 * contains a String)
	 */
	private byte[] _message = new byte[0];

	/**
	 * Unique name of the publisher (creator) of this message
	 */
	private String _publisherName = MDfromHTMLConstants.UNDEFINED_String;

	/**
	 * Sequence number for chunked messages (if > 0). Special
	 * {@link Integer#MAX_VALUE} is a semaphore to signal the last message. This
	 * works because we will not group consumers of messages. If this changes we
	 * would need to add a message count field to ensure messages that could
	 * then arrive out of sequence are accounted for. A single message will have
	 * a sequence of zero.
	 */
	private int _seqnum = 0;

	/**
	 * The identity of the session related to the message content. If the
	 * content is not associated with a particular session, the MDfromHTMLID is
	 * {@link MDfromHTMLID#UNDEFINED_ID} and corresponds with the String value in
	 * {@link MDfromHTMLConstants#UNDEFINED_ID}
	 */
	private MDfromHTMLID _sessionID = MDfromHTMLID.UNDEFINED_ID;

	/**
	 * The unique identity of the user or issuer within the session used for
	 * routing IPC messages within the WSServer to specific WebSockets connected to a
	 * UX, or for tracking a query and response
	 */
	private MDfromHTMLID _userID = MDfromHTMLID.UNDEFINED_ID;

	/**
	 * Topic used for publish/subscribe routing. Topics are registered with the
	 * MDfromHTML IPC (pub/sub) system before they can be sent.
	 */
	private String _topic = MDfromHTMLConstants.UNDEFINED_String;

	/**
	 * Type is used for processing within a Topic and may infer the message
	 * format. Type is optional and if not specified, it will contain the
	 * {@link MDfromHTMLConstants#UNDEFINED_String}.
	 */
	private String _type = MDfromHTMLConstants.UNDEFINED_String;

	/**
	 * Describes the action associated with this message
	 * 
	 * @see MDfromHTMLIPCVerbs
	 */
	private MDfromHTMLIPCVerbs _verb = MDfromHTMLIPCVerbs.UNDEFINED;

	/**
	 * Constructor.
	 * 
	 * @param publisherName
	 *            if null, the {@link MDfromHTMLID#UNDEFINED_ID} is used to create a
	 *            unique name
	 * @param sessionID
	 *            if null, the {@link MDfromHTMLID#UNDEFINED_ID} is used
	 * @param appl
	 *            the application id, if null, the
	 *            {@link MDfromHTMLIPCApplications#UNDEFINED} is used
	 * @param topic
	 *            if null, the {@link MDfromHTMLConstants#IPC_DEFAULT_TOPIC} is used
	 * @param type
	 *            if null, the {@link MDfromHTMLConstants#UNDEFINED_String} is used
	 * @param verb
	 *            the verb id, if null, the {@link MDfromHTMLIPCVerbs#UNDEFINED} is
	 *            used
	 * @param userID
	 *            the user id within the session, if null, the
	 *            {@link MDfromHTMLID#UNDEFINED_ID} is used
	 * @param message
	 *            if null, an empty byte array is used
	 */
	public MDfromHTMLIPCMessage(String publisherName, MDfromHTMLID sessionID, MDfromHTMLIPCApplications appl, String topic, String type,
			MDfromHTMLIPCVerbs verb, MDfromHTMLID userID, byte[] message) {
		setPublisherName(publisherName);
		setSessionID(sessionID);
		setApplication(appl);
		setTopic(topic);
		setType(type);
		setVerb(verb);
		setUserID(userID);
		// below determines if the message is JSON
		setMessageBytes(message);
	}

	/**
	 * Constructor.
	 * 
	 * @param publisherName
	 *            if null, the {@link MDfromHTMLID#UNDEFINED_ID} is used to create a
	 *            unique name
	 * @param sessionID
	 *            if null, the {@link MDfromHTMLID#UNDEFINED_ID} is used
	 * @param appl
	 *            the application id, if null, the
	 *            {@link MDfromHTMLIPCApplications#UNDEFINED} is used
	 * @param topic
	 *            if null, the {@link MDfromHTMLConstants#IPC_DEFAULT_TOPIC} is used
	 * @param type
	 *            if null, the {@link MDfromHTMLConstants#UNDEFINED_String} is used
	 * @param verb
	 *            the verb id, if null, the {@link MDfromHTMLIPCVerbs#UNDEFINED} is
	 *            used
	 * @param userID
	 *            the user id within the session, if null, the
	 *            {@link MDfromHTMLID#UNDEFINED_ID} is used
	 * @param message
	 *            if null, an empty JSONObject is sent. If for some reason the
	 *            object can not be serialized, rather than throwing an
	 *            exception, we are using the toString() method that sends a
	 *            non-JSON String. This should never happen as we are always
	 *            using a valid JSON object so throwing the exception and
	 *            forcing people to catch it everywhere didn't make any sense
	 */
	public MDfromHTMLIPCMessage(String publisherName, MDfromHTMLID sessionID, MDfromHTMLIPCApplications appl, String topic, String type,
			MDfromHTMLIPCVerbs verb, MDfromHTMLID userID, JSONObject message) {
		setPublisherName(publisherName);
		setSessionID(sessionID);
		setApplication(appl);
		setTopic(topic);
		setType(type);
		setVerb(verb);
		setUserID(userID);
		setMessage(message);
	}

	public MDfromHTMLIPCMessage(JSONObject message) throws Exception {
		try {
			String publisherName = (String) message.get("publisher");
			setPublisherName(publisherName);
			MDfromHTMLID sessionID = MDfromHTMLID.getExistingID((String) message.get("sessionID"));
			setSessionID(sessionID);
			MDfromHTMLIPCApplications appl = MDfromHTMLIPCApplications.fromName((String) message.get("appl"));
			setApplication(appl);
			String topic = (String) message.get("topic");
			setTopic(topic);
			String type = (String) message.get("type");
			setType(type);
			MDfromHTMLIPCVerbs verb = MDfromHTMLIPCVerbs.fromName((String) message.get("verb"));
			setVerb(verb);
			MDfromHTMLID userID = MDfromHTMLID.getExistingID((String) message.get("userID"));
			setUserID(userID);
			Long seq = (Long) message.get("seq");
			setSequence(seq.intValue());
			Boolean isJSON = (Boolean) message.get("isJSON");
			if (isJSON) {
				JSONObject msgObj = (JSONObject) message.get("msg");
				setMessage(msgObj);
			} else {
				String msgBytesStr = (String) message.get("msg");
				// strip "0x"
				msgBytesStr = msgBytesStr.substring(2);
				byte[] msgBytes = MDfromHTMLUtils.hexDecode(msgBytesStr);
				setMessageBytes(msgBytes);
			}
		} catch (Exception e) {
			throw new Exception("Malformed message " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Determines whether the passed array is a JSONObject
	 * 
	 * @param bytes
	 *            array to be tested to see if it represents a JSON object
	 * @return whether the passed array is a JSONObject
	 */
	private boolean checkIsJSON(byte[] bytes) {
		boolean isJSON = false;
		// simple test to determine if this is a JSONObject
		if ((_message.length > 1) && (_message[0] == 123) && (_message[_message.length - 1] == 125)) {
			try {
				JSON.parse(MDfromHTMLUtils.fromUTF8Bytes(_message));
				isJSON = true;
			} catch (NullPointerException | IOException e) {
				// keave isJSON = false
			}
		}
		return isJSON;
	}
	
   /**
    * @return the properties file used to define MDfromHTML interprocess communications
    * @see MDfromHTMLUtils#loadMDfromHTMLProperties(String)
    * @see MDfromHTMLConstants#MDfromHTML_IPC_PropertiesFileName
    */
   static public Properties getMDfromHTMLIPCProps() throws Exception {
      Properties ipcProps = MDfromHTMLUtils
         .loadMDfromHTMLProperties(MDfromHTMLConstants.MDfromHTML_IPC_PropertiesFileName);
      return ipcProps;
   }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getApplication()
	 */
	public MDfromHTMLIPCApplications getApplication() {
		return _appl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getApplicationValue()
	 */
	public int getApplicationValue() {
		return _appl.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getMessage()
	 */
	@Override
	public JSONObject getMessage() throws Exception {
		try {
			return (JSONObject) JSON.parse(getMessageString());
		} catch (NullPointerException | IOException e) {
			throw new Exception("Message contains invalid JSON", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getMessageBytes()
	 */
	@Override
	public byte[] getMessageBytes() {
		return _message;
	}

	/**
	 * Retrieve the length (in bytes) of the message content.
	 * 
	 * @return the length (in bytes) of the message content.
	 */
	protected int getMessageSize() {
		int iSize = 0;
		// based on the following
		// int header length
		iSize += Integer.BYTES; // could be Integer.SIZE/Byte.SIZE pre 1.8
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getPublisherName());
		iSize += _publisherName.length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getSessionID());
		iSize += MDfromHTMLID.ID_LENGTH;
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getApplicationValue());
		iSize += Integer.toString(getApplicationValue()).length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getTopic());
		iSize += _topic.length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getType());
		iSize += _type.length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getVerbValue());
		iSize += Integer.toString(getVerbValue()).length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getUserID());
		iSize += MDfromHTMLID.ID_LENGTH;
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getTime());
		iSize += new Long(_createTime).toString().length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.getSequence());
		iSize += new Integer(_seqnum).toString().length();
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// sb.append(msg.isJSON() ? "1" : "0");
		iSize += 1;
		// sb.append(MDfromHTMLConstants.MDfromHTML_DELIMITER);
		iSize += MDfromHTMLConstants.MDfromHTML_DELIMITER.length();
		// int message length
		iSize += Integer.BYTES; // could be Integer.SIZE/Byte.SIZE pre 1.8
		iSize += _message.length;
		return iSize;
	}

	/**
	 * Retrieve the message as a String
	 * 
	 * @return
	 * @throws Exception
	 *             if the original message was not a String
	 * @see #getMessageBytes()
	 */
	private String getMessageString() throws Exception {
		if (isJSON() == false) {
			throw new Exception("Message contains non-String data. Use getMessageBytes() instead. \n0x"
					+ MDfromHTMLUtils.hexEncode(_message));
		}
		return MDfromHTMLUtils.fromUTF8Bytes(_message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getPublisherName()
	 */
	@Override
	public String getPublisherName() {
		return _publisherName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getSequence()
	 */
	@Override
	public int getSequence() {
		return _seqnum;
	}

	/**
	 * Splits a single message into a sequence of smaller messages based on the
	 * chunkSize. The original message's time is preserved, as is the isJSON
	 * status
	 * 
	 * @param chunkSize
	 * @return array of messages based on the passed chunkSize
	 * @throws Exception
	 */
	public MDfromHTMLIPCMessage[] getSequencedMessages(int chunkSize) throws Exception {
		if (chunkSize < 2 || chunkSize > IPC_MAX_MESSAGE_SIZE) {
			throw new Exception(
					"Invalid chunk size. Must be > 1 or < " + IPC_MAX_MESSAGE_SIZE + ".");
		}

		ArrayList<MDfromHTMLIPCMessage> list = new ArrayList<MDfromHTMLIPCMessage>();
		if (chunkSize >= _message.length) {
			list.add(this);
		} else {
			int chunks = (_message.length / chunkSize);
			int offset = 0;
			MDfromHTMLIPCMessage msg = null;
			for (int seq = 1; seq <= chunks; seq++) {
				offset = (seq - 1) * chunkSize;
				msg = new MDfromHTMLIPCMessage(_publisherName, _sessionID, _appl, _topic, _type, _verb, _userID,
						Arrays.copyOfRange(_message, offset, (seq * chunkSize)));
				// retain this message's time
				msg.setTime(_createTime);
				msg.setSequence(seq);
				list.add(msg);
			}
			if (_message.length % chunkSize != 0) {
				// add final if there is one
				offset = (chunks) * chunkSize;
				msg = new MDfromHTMLIPCMessage(_publisherName, _sessionID, _appl, _topic, _type, _verb, _userID,
						Arrays.copyOfRange(_message, offset, _message.length));
				// retain this message's time
				msg.setTime(_createTime);
				msg.setSequence(Integer.MAX_VALUE);
				list.add(msg);
			} else {
				// flag this is the last in the sequence
				msg.setSequence(Integer.MAX_VALUE);
			}
		}

		return list.toArray(new MDfromHTMLIPCMessage[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getSessionID()
	 */
	@Override
	public MDfromHTMLID getSessionID() {
		return _sessionID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getTime()
	 */
	@Override
	public long getTime() {
		return _createTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getTopic()
	 */
	@Override
	public String getTopic() {
		return _topic;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getType()
	 */
	@Override
	public String getType() {
		return _type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getVerb()
	 */
	@Override
	public MDfromHTMLIPCVerbs getVerb() {
		return _verb;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getUserID()
	 */
	@Override
	public MDfromHTMLID getUserID() {
		return _userID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#getVerbValue()
	 */
	@Override
	public int getVerbValue() {
		return _verb.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#isJSON()
	 */
	@Override
	public boolean isJSON() {
		return _isJSON;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#isLastMessage()
	 */
	@Override
	public boolean isLastMessage() {
		return _seqnum ==  Integer.MAX_VALUE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#isOnlyMessage()
	 */
	@Override
	public boolean isOnlyMessage() {
		return _seqnum == 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#isSequence()
	 */
	@Override
	public boolean isSequence() {
		return _seqnum != 0;
	}


	/**
	 * Set the verb to a specific type. If the parameter is null then the
	 * {@link MDfromHTMLIPCApplications#UNDEFINED} is used.
	 * 
	 * @param appl
	 *            the application associated with this message
	 */
	private void setApplication(MDfromHTMLIPCApplications application) {
		if (application == null) {
			_appl = MDfromHTMLIPCApplications.UNDEFINED;
			return;
		}
		_appl = application;
	}

	/**
	 * Updates the message when disassembling or reassembling a chunked message
	 * 
	 * @param message
	 *            the payload of this message
	 */
	public void setMessage(JSONObject message) {
		if (MDfromHTMLUtils.isUndefined(message)) {
			// convert to explicit undefined value
			message = new JSONObject();
		}
		_message = MDfromHTMLUtils.toUTF8Bytes(message.toString());
		_isJSON = true;
	}

	/**
	 * Updates the message when disassembling or reassembling a chunked message
	 * 
	 * @param bytes
	 *            the payload of this message
	 */
	private void setMessageBytes(byte[] bytes) {
		if (bytes == null) {
			bytes = new byte[0];
		}
		_message = bytes;
		_isJSON = checkIsJSON(_message);
	}

	/**
	 * Updates the publisher name when splitting or reassembling a chunked
	 * message
	 * 
	 * @param publisherName
	 */
	private void setPublisherName(String publisherName) {
		if (MDfromHTMLUtils.isUndefined(publisherName)) {
			publisherName = new MDfromHTMLID().toString();
		}
		_publisherName = publisherName;
	}

	/**
	 * Updates the sequence number when splitting or reassembling a chunked
	 * message
	 * 
	 * @param seqnum
	 */
	private void setSequence(int seqnum) {
		_seqnum = seqnum;
	}

	/**
	 * Updates the session identifier when splitting or reassembling a chunked
	 * message
	 * 
	 * @param sessionID
	 */
	private void setSessionID(MDfromHTMLID sessionID) {
		if (MDfromHTMLID.isUndefined(sessionID)) {
			sessionID = MDfromHTMLID.UNDEFINED_ID;
		}
		_sessionID = sessionID;
	}

	/**
	 * Update the time when splitting or reassembling a chunked message
	 * 
	 * @param time
	 */
	private void setTime(long time) {
		_createTime = time;
	}

	/**
	 * Updates the topic when disassembling or reassembling a chunked message
	 * 
	 * @param topic
	 */
	private void setTopic(String topic) {
		if (MDfromHTMLUtils.isUndefined(topic)) {
			topic = MDfromHTMLConstants.IPC_DEFAULT_TOPIC;
		}
		_topic = topic;
	}

	/**
	 * Updates the type when disassembling or reassembling a chunked message
	 * 
	 * @param type
	 */
	private void setType(String type) {
		if (MDfromHTMLUtils.isUndefined(type)) {
			type = MDfromHTMLConstants.UNDEFINED_String;
		}
		_type = type;
	}

	/**
	 * Set the verb to a specific type. If the parameter is null then the
	 * {@link MDfromHTMLIPCVerbs#UNDEFINED} is used.
	 * 
	 * @param verb
	 *            the verb associated with this message
	 */
	private void setVerb(MDfromHTMLIPCVerbs verb) {
		if (verb == null) {
			_verb = MDfromHTMLIPCVerbs.UNDEFINED;
			return;
		}
		_verb = verb;
	}

	/**
	 * Updates the userIidentifier when splitting or reassembling a chunked
	 * message
	 * 
	 * @param userID
	 */
	private void setUserID(MDfromHTMLID userID) {
		if (MDfromHTMLID.isUndefined(userID)) {
			userID = MDfromHTMLID.UNDEFINED_ID;
		}
		_userID = userID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#toStringFull()
	 */
	@Override
	public String toStringFull() {
		return toStringFull(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mdfromhtml.ipc.IMDfromHTMLIPCMessage#toStringFull(boolean)
	 */
	@Override
	public String toStringFull(boolean formatJSON) {
		StringBuffer sb = new StringBuffer();
		sb.append("SessionID:");
		sb.append(_sessionID);
		sb.append(", Appl:");
		sb.append(_appl.name());
		sb.append(" Topic:");
		sb.append(_topic);
		sb.append(", Type:");
		sb.append(_type);
		sb.append(", Verb:");
		sb.append(_verb.name());
		sb.append(", UserID:");
		sb.append(_userID);
		sb.append(", Publisher:");
		sb.append(_publisherName);
		sb.append(", Time:");
		sb.append(new MDfromHTMLDate(_createTime).toStringDateTime());
		sb.append(", Seq:");
		sb.append(_seqnum);
		sb.append(", isJSON:");
		sb.append(_isJSON);
		sb.append(", isLast:");
		sb.append(isLastMessage());
		sb.append(", isOnly:");
		sb.append(isOnlyMessage());
		sb.append("\nmsg:");
		if (isJSON()) {
			try {
				try {
					sb.append(getMessage().serialize(formatJSON));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			sb.append("0x");
			sb.append(MDfromHTMLUtils.hexEncode(_message));
		}
		return sb.toString();
	}

	public String toHdrString() {
		StringBuffer sb = new StringBuffer();
		sb.append("SessionID:");
		sb.append(_sessionID);
		sb.append(", Appl:");
		sb.append(_appl.name());
		sb.append(" Topic:");
		sb.append(_topic);
		sb.append(", Type:");
		sb.append(_type);
		sb.append(", Verb:");
		sb.append(_verb.name());
		sb.append(", UserID:");
		sb.append(_userID);
		sb.append(", Publisher:");
		sb.append(_publisherName);
		sb.append(", Time:");
		sb.append(new MDfromHTMLDate(_createTime).toStringDateTime());
		sb.append(", Seq:");
		sb.append(_seqnum);
		sb.append(", isJSON:");
		sb.append(_isJSON);
		sb.append(", isLast:");
		sb.append(isLastMessage());
		sb.append(", isOnly:");
		sb.append(isOnlyMessage());
		return sb.toString();
	}

	public String toStringHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append(" Time: ");
		sb.append(new MDfromHTMLDate(_createTime).toStringDateTime());
		sb.append(", Seq:");
		sb.append(_seqnum);
		sb.append(", Publisher:");
		sb.append(_publisherName);
		sb.append(", SessionID:");
		sb.append(_sessionID);
		sb.append(",\n   Topic:");
		sb.append(_topic);
		sb.append(", Type:");
		sb.append(_type);
		sb.append(", Verb:");
		sb.append(_verb.name());
		sb.append(", UserID:");
		sb.append(_userID);
		sb.append(", isJSON:");
		sb.append(_isJSON);
		sb.append(", isLast:");
		sb.append(isLastMessage());
		sb.append(", isOnly:");
		sb.append(isOnlyMessage());
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" Time: ");
		sb.append(new MDfromHTMLDate(_createTime).toStringDateTime());
		sb.append(", Seq:");
		sb.append(_seqnum);
		sb.append(", Publisher:");
		sb.append(_publisherName);
		sb.append(", SessionID:");
		sb.append(_sessionID);
		sb.append(",\n   Topic:");
		sb.append(_topic);
		sb.append(", Type:");
		sb.append(_type);
		sb.append(", Verb:");
		sb.append(_verb.name());
		sb.append(", UserID:");
		sb.append(_userID);
		sb.append(", isJSON:");
		sb.append(_isJSON);
		sb.append(", isLast:");
		sb.append(isLastMessage());
		sb.append(", isOnly:");
		sb.append(isOnlyMessage());
		sb.append("\nmsg:");
		String text = "";
		if (isJSON()) {
			try {
				text = getMessage().toString();
			} catch (Exception e) {
				// Should not happen as only JSONObject or byte[] are allowed
				e.printStackTrace();
			}
		} else {
			text = "0x" + MDfromHTMLUtils.hexEncode(_message);
		}
		sb.append(MDfromHTMLUtils.shortenString(text, 100));
		return sb.toString();
	}

	public JSONObject toJson() {
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("sessionID", _sessionID.toString());
		jsonObj.put("appl", _appl.name());
		jsonObj.put("topic", _topic);
		jsonObj.put("type", _type);
		jsonObj.put("verb", _verb.name());
		jsonObj.put("userID", _userID.toString());
		jsonObj.put("publisher", _publisherName);
		jsonObj.put("time", new MDfromHTMLDate(_createTime).toStringDateTime());
		jsonObj.put("seq", new Long(_seqnum));
		jsonObj.put("isJSON", _isJSON);
		jsonObj.put("isLast", isLastMessage());
		jsonObj.put("isOnly", isOnlyMessage());
		try {
			jsonObj.put("msg", (isJSON() ? getMessage() : "0x" + MDfromHTMLUtils.hexEncode(_message)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			jsonObj.put("msg", "0x" + MDfromHTMLUtils.hexEncode(_message));
		}
		return jsonObj;
	}

}
