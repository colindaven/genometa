/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genometryImpl.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.regex.*;

/**
 * Converts URLs to filenames that are supported on most file systems.
 * Trying to use something similar to the XML character reference format, but use it 
 *    for encoding characters that aren't supported in filenames on some systems,
 *    for info on XML character references see XML spec at http://www.w3.org/TR/2000/REC-xml-20001006
 *
 *  So when encoding a URL as a filename, a character CHAR in the URL that isn't supported 
 *    as a filename character gets encoded to "character reference" format, which is
 *    '&#' + (int)CHAR + ';'
 *  And when decoding filename to URL, any string in filename of form '&#' + (int)CHAR + ';' gets
 *    converted back to character CHAR
 *
 * UrlToFileName is intended mainly for caching URL content, so that a URL's content can be written to
 *    a file whose name is the encoded form of the URL
 *
 *  GAH 11-2006
 *  Changed to handle long URLs -- Windows has 256-character limit for filenames, other OSes 
 *    may have similar restrictions, an throws errors when trying to create files with longer names.
 *    Approach is to break up URL into pieces shorter than 256 characters, and create subdirectories 
 *    with those pieces.

 *  Current implementation assumes that all of URL up to "?" before query parameters is <= 256 characters, 
 *    and that no every query tag=val parameter is <= 256 characters
 *
 *  Strategy
 *    start with input URL, if length() > 250 
 *       split by first '?' ==> path+? , query_string
 *       convert path+? to legal file name ==> conv_path+
 *       make conv_path+ into subdirectory
 *       query_string_left = query_string
 *       while (query_string_left.size() > 250)  {
 *           split ==> head = first query parameter (query_string_left up to first ';'), query_string_left = rest
 *           converty head to legal file name ==> conv_head
 *           make conv_head into subdirectory
 *       }
 *       make query_string_left into file
 *
 *   Example using DAS/2 feature query:
 *   URL:   http://localhost:9092/das2/genome/H_sapiens_Mar_2006/features?segment=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2Fchr21;overlaps=0%3A46944323;type=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2FHuEx-1_0-st-Probes;format=bp2
 *   Human readable form:
 *      http://localhost:9092/das2/genome/H_sapiens_Mar_2006/features?
 *           segment=http://localhost:9092/das2/genome/H_sapiens_Mar_2006/chr21;
 *           overlaps=0:46944323;
 *           type=http://localhost:9092/das2/genome/H_sapiens_Mar_2006/HuEx-1_0-st-Probes;
 *           format=bp2
 *
 *  Resulting cached file path, old approach:
 *  C:\Documents and Settings\ghelt\Application Data\IGB\cache\http&#58;&#47;&#47;localhost&#58;9092&#47;das2&#47;genome&#47;H_sapiens_Mar_2006&#47;features&#63;segment=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2Fchr21;overlaps=0%3A46944323;type=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2FHuEx-1_0-st-Probes;format=bp2
 *
 *  File name, old approach -- 306 characters
http&#58;&#47;&#47;localhost&#58;9092&#47;das2&#47;genome&#47;H_sapiens_Mar_2006&#47;features&#63;segment=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2Fchr21;overlaps=0%3A46944323;type=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2FHuEx-1_0-st-Probes;format=bp2


*   New approach:
*    subdirectories under cache root:

http&#58;&#47;&#47;localhost&#58;9092&#47;das2&#47;genome&#47;H_sapiens_Mar_2006&#47;features&#63;
segment=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2Fchr21; overlaps=0%3A46944323; type=http%3A%2F%2Flocalhost%3A9092%2Fdas2%2Fgenome%2FH_sapiens_Mar_2006%2FHuEx-1_0-st-Probes; format=bp2

 *  
 */
public final class UrlToFileName {

  static MessageDigest md5_generator;
  
  static boolean md5_init = false;
  static void initializeMd5() {
    try {
      md5_generator = MessageDigest.getInstance("MD5");
    }
    catch (Exception ex) {
      md5_generator = null;
      ex.printStackTrace();
    }
    md5_init = true;
  }
  
  /** Converts an arbitrary string into an MD5 hash code as a HEX String. 
   *  If for some reason it was impossible to initialize the md5 generator,
   *  the string will be returned unchanged.
   */
  public static String toMd5(String s) {
    if (! md5_init) {
      initializeMd5();
    }

    if ( md5_generator != null) {
      byte[] md5_digest = md5_generator.digest(s.getBytes());
      BigInteger md5_big_int = new BigInteger(md5_digest);
      return md5_big_int.toString(16);
    } else {
      return s;
    }
  }

  /**
   *  Matches chars in URLs that need to encoded/escaped when converted to filename.
   *  Those are: 
   *  <pre>
   *  \ / : * ? " &gt; &lt; | +
   *  </pre>
   */
    public static final Pattern char_encode_pattern = Pattern.compile(
							 "[" +
                                                         "\\\\" +   //  match \
                                                         "/" +      //  match /
							 "\\:" +    //  match :
							 "\\*" +    //  match *
							 "\\?" +    //  match ?
							 "\\\"" +   //  match "
							 "\\<" +    //  match <
							 "\\>" +    //  match >
                                                         "\\|" +    //  match |
                                                         "\\+" +    //  match +
                                                         "]" );

  static final Pattern char_decode_pattern = Pattern.compile("&#(\\d+);"); 
  static String encoded_query_start = "&#" + (int)'?' + ";";


  /**
   *  Convert URL to filename.
   */
  public static String encode(String url) {
    Matcher char_encode_matcher = char_encode_pattern.matcher(url);
    StringBuffer buf = new StringBuffer();
    while (char_encode_matcher.find()) {
      String grp = char_encode_matcher.group();
      char ch = grp.charAt(0);
      char_encode_matcher.appendReplacement(buf, "&#" + (int)ch + ";");
    }
    char_encode_matcher.appendTail(buf);
    String result = buf.toString();
    // Many OSes have limit of 255 or 256 characters for filename, so split up
    // This won't work!  Java (and some underlying OSes) have limits on 
    //    total length of file _path_, not just leaf name
    //
    // Alternative solution: 
    //   If filename too long (>240? see web refs)
    //   then map to an integer for filename, and keep a map (that is loaded when IGB starts) 
    //       of too_long_filename==>integer 
    //   Maybe separate in subdirectory cache_root/too_long ?
    //
    //
    /*
    if (result.length() > 255) {
      // assuming length is due to query params, so first split by '?' before query params
      int cindex = result.indexOf(encoded_query_start);
      if (cindex > 0) {
	String base = result.substring(0, cindex + encoded_query_start.length());
	String query = result.substring(cindex + encoded_query_start.length());
	//	System.out.println("file name is too long, splitting: ");
	//	System.out.println("base: " + base);
	//	System.out.println("query: " + query);
	result = base + "/" + query;
      }
      else {
	// no query param, so just split by length?
      }
    }
    */
    return result;
  }


  /**
   *  Convert filename to URL.
   */
  /*public static String decode(String filename) {
    Matcher char_decode_matcher = char_decode_pattern.matcher(filename);
    StringBuffer buf = new StringBuffer();
    while (char_decode_matcher.find()) {
      String int_str = char_decode_matcher.group(1);
      int char_int = Integer.parseInt(int_str);
      char ch = (char)char_int;
      String char_str = Character.toString(ch);
      // can't add just '\' back, because then it will signal escaping the next char, so
      //     need to add '\\' instead
      if (char_str.equals("\\")) { char_str = "\\\\"; }
      char_decode_matcher.appendReplacement(buf, char_str);
    }
    char_decode_matcher.appendTail(buf);
    return buf.toString();
  }*/


  /*static String[] default_test_urls = { "http://test.url.com/testing/url/to/filename/encoding",
					"this\\should/test:all*the?chars\"that<need>encoding|I+hope" };*/
  /**
   *  a main() for testing purposes
   */
  /*public static void main(String[] args) {
    String test_urls[] = null;
    if (args.length > 0) {  test_urls = args; }
    else { test_urls = default_test_urls; }
    for (int i=0; i<test_urls.length; i++) {
      String test_url = test_urls[i];
      System.out.println("test " + i);
      System.out.println("original url:         " + test_url);
      System.out.println("encode(url):          " + encode(test_url));;
      System.out.println("decode(encode(url)):  " + decode(encode(test_url)));
    }
  }*/




}
