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

import java.io.Serializable;

/**
 * A home grown Base 64 encoder/decoder that produces and interprets content
 * compatible with other Base 64 codecs from Apache.org and Sun
 * 
 * @author wnm3
 */
public class MDfromHTMLBASE64Codec implements Serializable {

   static private final long serialVersionUID = 2712028472437499003L;
   static boolean bDebugFlag = false;
   static int LINE_LEN = 76;
   // "0_______________1_______________2_______________3_______________4"
   // "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0"
   static public final String val_safe = new String(
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_=");
   // Note: last char is the pad character ('=')

   // "0____0____1____1____2____2____3____3____4____4____5____5____6____
   // "01234567890123456789012345678901234567890123456789012345678901234
   static final String val_str = new String(
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=");
   static final byte[] b64Digits = val_str.getBytes();
   static final byte[] b64SafeDigits = val_safe.getBytes();
   static int PAD_OFFSET = b64Digits.length - 1;

   /**
    * Convert a String of BASE64 digits into a byte array, ignoring the equals
    * (=) pad characters at the end. This is not URL safe
    * 
    * @param strEncoded
    * @return array of Base 256 bytes
    */
   static public byte[] decode(String strEncoded) {
      return decode(strEncoded, false);
   }

   /**
    * Convert a String of BASE64 digits into a byte array, ignoring the equals
    * (=) pad characters at the end. This can be made URL safe by passing in
    * true
    * 
    * @param strEncoded
    * @param bURLSafe
    *           whether (true) or not (false) the encoding is URL safe meaning
    *           it uses "-" and "_" in lieu of "+" and "/"
    * @return array of Base 256 bytes
    */
   static public byte[] decode(String strEncoded, boolean bURLSafe) {
      if (strEncoded == null) {
         return new byte[0];
      }
      byte[] bEncoded = strEncoded.getBytes();
      int iLen = bEncoded.length;
      if (bDebugFlag) {
         System.out
            .println("[base64]decode received string " + iLen + " bytes long:");
         System.out.println(strEncoded);
      }
      int iPADIndex = strEncoded.indexOf(b64Digits[PAD_OFFSET]);
      // strip off any trailing new lines
      if (iPADIndex > 0) {
         iLen = iPADIndex;
      }
      int iNLCount = 0;
      int iNLIndex = 0;

      // count any embedded new lines
      while (iNLIndex < iLen) {
         if (bEncoded[iNLIndex++] == 0x0a) {
            iNLCount++;
         }
      }
      if (bDebugFlag) {
         System.out.println(
            "[base64]decode found " + iNLCount + " embedded newlines:");
      }
      if (iLen - iNLCount == 0) {
         // just contains newlines
         return new byte[0];
      }
      int iOutLen = ((iLen - iNLCount) * 3) / 4;
      if (bDebugFlag) {
         System.out.println("[base64]decode output length is " + iOutLen);
      }
      if (iOutLen == 0) {
         return new byte[0];
      }
      byte[] bOut = new byte[iOutLen];
      byte[] bWork = new byte[4];
      int iWorkOffset = 0;
      int iOutputOffset = 0;
      int i = 0;
      for (i = 0; i < iLen; i++) {
         if (bEncoded[i] == 0x0a) {
            continue; // strip new lines
         }
         if (bEncoded[i] == b64Digits[PAD_OFFSET]) {
            iOutputOffset = fromBASE64(bWork, iWorkOffset, bOut, iOutputOffset);
            break;
         }
         bWork[iWorkOffset++] = bEncoded[i];
         if (iWorkOffset == 4) {
            iOutputOffset = fromBASE64(bWork, iWorkOffset, bOut, iOutputOffset);
            iWorkOffset = 0;
            bWork[0] = 0x00;
            bWork[1] = 0x00;
            bWork[2] = 0x00;
            bWork[3] = 0x00;
         }
      }
      if (iWorkOffset > 0 && i > 0
         && bEncoded[i - 1] != b64Digits[PAD_OFFSET]) {
         iOutputOffset = fromBASE64(bWork, iWorkOffset, bOut, iOutputOffset);
      }
      return bOut;
   }

   /**
    * Transform a byte array into a string of the BASE64 characters. Each
    * grouping of 3 bytes is transformed into 4 BASE64 bytes which use 6 bits
    * rather than 8 bits. Pad with equals (=) when input length does not divide
    * by 3 evenly. The resulting String is not URL safe.
    * 
    * @param bArray
    *           Base 256 array of bytes to be encoded
    * @return String of Base 64 bytes containing the encoding of the input array
    * @see #encode(byte[], boolean)
    */
   static public String encode(byte[] bArray) {
      return encode(bArray, false);
   }

   /**
    * Transform a byte array into a string of the BASE64 characters. Each
    * grouping of 3 bytes is transformed into 4 BASE64 bytes which use 6 bits
    * rather than 8 bits. Pad with equals (=) when intput length does not divide
    * by 3 evenly.
    * 
    * @param bArray
    *           Base 256 array of bytes to be encoded
    * @param bURLSafe
    *           whether (true) or not (false) the encoding is URL safe meaning
    *           it uses "-" and "_" in lieu of "+" and "/"
    * @return String of Base 64 bytes containing the encoding of the input array
    */
   static public String encode(byte[] bArray, boolean bURLSafe) {
      if (bArray == null) {
         return "";
      }
      int iLen = bArray.length;
      if (iLen == 0) {
         return "";
      }
      // the output is 4/3 the size of the input rounded up to multiple of 4
      int iOutLen = (((iLen * 4) + 2) / 3);
      if (iOutLen % 4 != 0) {
         iOutLen += 4 - (iOutLen % 4);
      }
      // determine number of newlines to add at LINE_LEN length records
      int iNLCount = iOutLen / LINE_LEN;
      int[] nlMgr = new int[1];
      iOutLen += iNLCount;
      byte[] bOut = new byte[iOutLen];
      byte[] bWork = new byte[3];
      int iWorkOffset = 0;
      int iOutputOffset = 0;
      for (int i = 0; i < iLen; i++) {
         // accumulate 3 bytes at a time
         bWork[iWorkOffset++] = bArray[i];
         if (iWorkOffset == 3) {
            iOutputOffset = toBASE64(bWork, iWorkOffset, bOut, iOutputOffset,
               nlMgr, bURLSafe);
            iWorkOffset = 0;
         }
      }
      if (iWorkOffset != 0) {
         // there are some left overs to process (will be 1 or 2)
         iOutputOffset = toBASE64(bWork, iWorkOffset, bOut, iOutputOffset,
            nlMgr, bURLSafe);
      }
      return new String(bOut);
   }

   /**
    * Converts an array of base 64 bytes into their integer value
    * 
    * @param bWork
    * @param iWorkOffset
    * @param bOut
    * @param iOutputOffset
    * @return value represented by the base 64 bytes supplied
    */
   static int fromBASE64(byte[] bWork, int iWorkOffset, byte[] bOut,
      int iOutputOffset) {
      // note: all of these should be between 0x00 and 0x3F
      byte b1 = getBASE64index(bWork[0]); // top 6 bits of 1st byte
      byte b2 = getBASE64index(bWork[1]); // bottom 2 bits of 1st byte, top 4
                                          // bits of 2nd byte
      byte b3 = getBASE64index(bWork[2]); // bottom 4 bits of 2nd byte, top 2
                                          // bits of 3rd byte
      byte b4 = getBASE64index(bWork[3]); // bottom 6 bits of 3rd byte

      if (iWorkOffset >= 0) {
         // use all 6 from b1 + 2 from b2
         bOut[iOutputOffset] = (byte) (b1 << 2);
         bOut[iOutputOffset] |= (byte) (((b2 & 0x30) >> 4) & 0x03);
         iOutputOffset++;
      }
      if (iWorkOffset >= 1 && iOutputOffset < bOut.length) {
         // use 4 from b2 + 4 from b3
         bOut[iOutputOffset] = (byte) (((b2 & 0x0F) << 4) & 0xF0);
         bOut[iOutputOffset] |= (byte) (((b3 & 0x3C) >> 2) & 0x0F);
         iOutputOffset++;
      }
      if (iWorkOffset >= 2 && iOutputOffset < bOut.length) {
         // use 2 from b3 and all 6 from b4
         bOut[iOutputOffset] = (byte) (((b3 & 0x03) << 6) & 0xC0);
         bOut[iOutputOffset] |= b4;
         iOutputOffset++;
      }
      return iOutputOffset;
   }

   /**
    * This routine can handle conversion of URL Safe and non-URL Safe input
    * 
    * @param bIn
    *           Base 64 character input
    * @return the number (0..63) associated with the Base 64 input
    */
   static byte getBASE64index(byte bIn) {
      // A-Z
      if (bIn >= (byte) 0x41 && bIn <= (byte) 0x5A) {
         return (byte) (bIn - 0x41);
      }
      // a-z
      if (bIn >= (byte) 0x61 && bIn <= (byte) 0x7A) {
         // (-x61+ x1a) == -0x47
         return (byte) (bIn - 0x47);
      }
      // 0-9
      if (bIn >= (byte) 0x30 && bIn <= (byte) 0x39) {
         // (-x30+x34) == +0x04
         return (byte) (bIn + 0x04);
      }
      if (bIn == (byte) 0x2B) { // non-URL Safe plus (+)
         return (byte) 0x3E;
      }
      if (bIn == (byte) 0x2F) { // non-URL Safe solidus (/)
         return (byte) 0x3F;
      }
      if (bIn == (byte) 0x2D) { // URL Safe hyphen
         return (byte) 0x3E;
      }
      if (bIn == (byte) 0x5F) { // URL Safe underscore
         return (byte) 0x3F;
      }
      return 0x00;
   }

   static public void main(String[] args) {
      String strTest = "passw0rd";
      if (args.length > 0) {
         strTest = args[0];
      } else {
         strTest = MDfromHTMLUtils.prompt("Enter text to be converted:");
      }
      byte[] testBytes = strTest.getBytes();
      // URL safe version
      String strEncoded = encode(testBytes, true);
      System.out.println(strTest + "=" + strEncoded);
      byte[] decoded = decode(strEncoded, true);
      String strDecoded = new String(decoded);
      System.out.println(strDecoded);
      strTest = MDfromHTMLUtils.prompt("Enter text to be decoded:");
      decoded = decode(strTest, true);
      strDecoded = new String(decoded);
      System.out.println(strDecoded);
   }

   /**
    * Converts 3 sets of bytes (Base 256) into 4 sets of Base 64 bytes
    * 
    * @param bWork
    * @param iWorkOffset
    * @param bOut
    * @param iOutputOffset
    * @param nlMgr
    * @return updated offset in output
    */
   static int toBASE64(byte[] bWork, int iWorkOffset, byte[] bOut,
      int iOutputOffset, int[] nlMgr, boolean bURLSafe) {
      // converts 3 sets of 8 bits (24 bits)
      // to 4 sets of 6 bits (24 bits)
      // using high end of 1st byte of bWork for first 6 bits
      int iBASE64Offset = 0;
      if (iWorkOffset >= 0) {
         iBASE64Offset = ((bWork[0] & 0xFC) >> 2) & 0x3F; // top 6 bits
         bOut[iOutputOffset++] = (bURLSafe ? b64SafeDigits[iBASE64Offset]
            : b64Digits[iBASE64Offset]);
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      }
      if (iWorkOffset >= 1) {
         iBASE64Offset = (bWork[0] & 0x03) << 4; // low 2 bits as top 2
         iBASE64Offset |= (((bWork[1] & 0xF0) >> 4) & 0x0F); // top 4 bits as
                                                             // bottom 4
         bOut[iOutputOffset++] = (bURLSafe ? b64SafeDigits[iBASE64Offset]
            : b64Digits[iBASE64Offset]);
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      }
      if (iWorkOffset >= 2) {
         iBASE64Offset = (((bWork[1] & 0x0F) << 2) & 0x3C); // bottom 4 bits as
                                                            // top 4
         iBASE64Offset |= ((bWork[2] & 0xC0) >> 6) & 0x03; // top 2 bits as
                                                           // bottom 2
         bOut[iOutputOffset++] = (bURLSafe ? b64SafeDigits[iBASE64Offset]
            : b64Digits[iBASE64Offset]);
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      } else {
         bOut[iOutputOffset++] = b64Digits[PAD_OFFSET];
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      }
      if (iWorkOffset >= 3) {
         iBASE64Offset = bWork[2] & 0x3F; // bottom 6
         bOut[iOutputOffset++] = (bURLSafe ? b64SafeDigits[iBASE64Offset]
            : b64Digits[iBASE64Offset]);
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      } else {
         bOut[iOutputOffset++] = b64Digits[PAD_OFFSET];
         nlMgr[0]++;
         if (nlMgr[0] % LINE_LEN == 0) {
            bOut[iOutputOffset++] = 0x0a; // new line
         }
      }
      bWork[0] = 0x00;
      bWork[1] = 0x00;
      bWork[2] = 0x00;
      return iOutputOffset;
   }
}
