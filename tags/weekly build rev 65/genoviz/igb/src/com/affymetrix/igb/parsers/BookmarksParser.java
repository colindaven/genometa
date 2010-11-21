/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.*;
import java.util.*;
import java.net.MalformedURLException;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.bookmarks.BookmarkList;

/**
 *  A class for reading and parsing a file of Bookmarks.
 *  Accepts several formats.
 */
public final class BookmarksParser {
  
  private static final boolean DEBUG = false;

  public static final int SIMPLE_HTML_FORMAT = 0;
  public static final int NETSCAPE_FORMAT = 1;
  public static final int BED_FORMAT = 2;

  public static void parse(BookmarkList bookmarks, File f) 
  throws FileNotFoundException, IOException {
    int format = getFormat(f);
    if (DEBUG) System.out.println("Format of '"+f.getAbsolutePath()+"' is "+format);
    if (format == BED_FORMAT) {
      GenometryModel gmodel = GenometryModel.getGenometryModel();
      parseBEDFormat(bookmarks, f, gmodel);
    } else if (format == NETSCAPE_FORMAT) {
      parseNetscapeBookmarks(bookmarks, f);
    } else if (format == SIMPLE_HTML_FORMAT) {
      parseSimpleHTML(bookmarks, f);
    } else {
      throw new RuntimeException("Unknown bookmark file format");
    }
  }
  
  public static int getFormat(File f) throws FileNotFoundException, IOException {
    int result = SIMPLE_HTML_FORMAT;
    if (f.getName().endsWith(".bed")) {
      result = BED_FORMAT;
    } else {
      FileInputStream fis = null;
      InputStreamReader isr = null;
      BufferedReader br = null;
      try {
        fis = new FileInputStream(f);
        isr = new InputStreamReader(fis);
        br = new BufferedReader(isr);
        String line = br.readLine();
        if (line != null) {
          if (line.trim().equals(BookmarkList.NETSCAPE_BOOKMARKS_DOCTYPE)) {
            result = NETSCAPE_FORMAT;
          } else {
            result = SIMPLE_HTML_FORMAT;
          }
        }
      } finally {
				GeneralUtils.safeClose(br);
				GeneralUtils.safeClose(isr);
				GeneralUtils.safeClose(fis);
      }
    }
    return result;
  }

  static String parseNetscapeBookmarkListName(String html) {
    String result = "List";
    String str = html.trim();
    String str_uc = str.toUpperCase();
    if (str_uc.startsWith("<DT><H")) { // usually <DT><H3>
      int ind1 = 1 + str_uc.indexOf(">", 7);
      int ind2 = str_uc.indexOf("</H", ind1);
      result = str.substring(ind1, ind2);
    }
    return result;
  }

  /** @return a Bookmark, or null if the bookmark couldn't be parsed. */
  static Bookmark parseNetscapeFormatBookmark(String html) {
    String name = null;
    String url = null;
    String str = html.trim();
    String str_uc = str.toUpperCase();
    if (str_uc.startsWith("<DT><A ")) {
      int ind_a = str_uc.indexOf("HREF");
      int ind_b = str_uc.indexOf('=', ind_a);
      int ind_c = str_uc.indexOf('"', ind_b);
      int ind_d = str_uc.indexOf('"', ind_c+1);
      if (ind_c > -1 && ind_d > ind_c) {
        url = str.substring(ind_c + 1, ind_d);
      }

      // Now get the Name
      int ind1 = 1 + str_uc.indexOf(">", 7);
      int ind2 = str_uc.indexOf("</A", ind1);
      if (ind1 > -1 && ind2 > ind1) {
        name = str.substring(ind1, ind2);
      }
    }

    Bookmark bm = null;
    if (url != null && name != null) {
      try {
        bm = new Bookmark(name, url);
      } catch (MalformedURLException mfe) {
        if (DEBUG) System.out.println("Couldn't make bookmark for '"+url+"'");
      }
    }

    return bm;
  }

  public static void parseNetscapeBookmarks(BookmarkList bookmarks, BufferedReader br) throws IOException {
    if (DEBUG) System.out.println("loading bookmarks in Netscape format from a BufferedReader");

    BookmarkList current_list = bookmarks;
    Stack<BookmarkList> parents = new Stack<BookmarkList>();
    String line;
    while ((line = br.readLine()) != null) {
      line = line.trim();
      String str = line.trim().toUpperCase();
      if (str.startsWith("<DT><H3")) { // Start and name a new list
        //if (DEBUG) System.out.println("FOLDER:   "+line);
        BookmarkList new_list = new BookmarkList(parseNetscapeBookmarkListName(line));
        current_list.addSublist(new_list);
        if (DEBUG) System.out.println("Made new list: "+new_list.getName()+", with parent: "+current_list.getName());
        parents.push(current_list);
        current_list = new_list;
      } else if (str.startsWith("<DL><P>")) { // Second part of starting a new list
        if (DEBUG) System.out.println("IGNORE:   "+line);
      } else if (str.startsWith("</DL>")) { // Finish that list
        if (DEBUG) System.out.println("END LIST: "+line);
        if (current_list != null && ! parents.isEmpty()) { 
          current_list = parents.pop();
          if (current_list == null) {
            if (DEBUG) System.out.println("parent list is null");
          } else {
            if (DEBUG) System.out.println("went back to parent list: "+current_list.getName());
          }
        }
        else if (DEBUG) {System.out.println("Still in null list");}
      } else if (str.startsWith("<DT>")) { // Add a bookmark to current list
        Bookmark b = parseNetscapeFormatBookmark(line);
        if (DEBUG) System.out.println("BOOKMARK: "+b);
        if (b != null) current_list.addBookmark(b);
      } else if (str.startsWith("<HR>")) { // Add divider to current list
        current_list.addSeparator();
      } else {
        if (DEBUG) System.out.println("????:     "+line);
      }
    }
  }

  /** Parses bookmarks from a given file (formatted in Netscape bookmark format)
   *  and adds them to the given BookmarkList.
   */
  public static void parseNetscapeBookmarks(BookmarkList bookmarks, File f) throws FileNotFoundException, IOException {
    if (DEBUG) System.out.println("loading bookmarks in Netscape format from '"+f.getAbsolutePath()+"'");
    FileInputStream fis = new FileInputStream(f);
    InputStreamReader isr = new InputStreamReader(fis);
    BufferedReader br = new BufferedReader(isr);
    try {
      parseNetscapeBookmarks(bookmarks, br);
    } finally {
		GeneralUtils.safeClose(br);
		GeneralUtils.safeClose(isr);
		GeneralUtils.safeClose(fis);
    }
  }

  /** Parses bookmarks from a given file (formatted in BED format)
   *  and adds them to the given BookmarkList.
   */
  public static void parseBEDFormat(BookmarkList bookmarks, File f, GenometryModel gmodel) throws IOException {
    if (DEBUG) System.out.println("loading bookmarks in BED format from '"+f.getAbsolutePath()+"'");
    FileInputStream fis = new FileInputStream(f);
    BufferedInputStream bis = new BufferedInputStream(fis);
    try {
      parseBEDFormat(bookmarks, bis, gmodel);
    } finally {
		GeneralUtils.safeClose(bis);
		GeneralUtils.safeClose(fis);
    }
  }
  
  public static void parseBEDFormat(BookmarkList bookmarks, BufferedInputStream istr, GenometryModel gmodel) throws IOException {
    boolean had_errors = false;

    BedParser bparser = new BedParser();
    
    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("unknown");
    List annots = bparser.parse(istr, gmodel, seq_group, true, "bookmarks", false);
    
    if ((annots != null)) {
      for (int k=0; k<annots.size(); k++) {
        SeqSymmetry annot = (SeqSymmetry)annots.get(k);
        String annotname = "Bookmark "+k;
        String path = null;
        if (annot instanceof SymWithProps) {
          SymWithProps swp = (SymWithProps) annot;
          if (swp.getProperty("name") != null) {
            annotname = (String) swp.getProperty("name");
            path = (String) swp.getProperty("type");
          }
        }
        BookmarkList sub_list = bookmarks;
        if (path != null) {
          sub_list = bookmarks.getSubListByPath(path, "/", true);
        }
        try {
          if (DEBUG) System.out.println("Bookmark: "+annotname);
          Bookmark bm = BookmarkController.makeBookmark(annot, annotname);
          if (DEBUG) System.out.println("Adding bookmark "+bm+" to list "+sub_list.getName());
          sub_list.addBookmark(bm);
        } catch (MalformedURLException mfe) {
          mfe.printStackTrace();
          had_errors = true;
        }
      }
    }
    if (had_errors) {
      throw new IOException("Some bookmarks could not be read");
    }
  }

  /** Parses bookmarks from a given HTML file
   *  and adds them to the given BookmarkList.
   *  This routine parses the very simple format used by earlier versions of
   *  this program.  We don't usually write bookmark files in this format now.
   *  Any line that contains "http://" will be parsed as a bookmark.
   *  Only one bookmark can be parsed from any line, and there is no
   *  heirarchical, tree-like structure.
   */
  public static void parseSimpleHTML(BookmarkList bm_list, File fil) throws IOException {
    FileInputStream fis = null;
    InputStreamReader isr = null;
    BufferedReader br = null;
    
    try {
      fis = new FileInputStream(fil);
      isr = new InputStreamReader(fis);
      br = new BufferedReader(isr);
      String line;
      while ((line = br.readLine()) != null) {
        int uindex = line.indexOf("http://");
        if (uindex >= 0) {
          int qstart = uindex;
          int qend = line.indexOf("\"", qstart);
          String query_string;
          if (qend < 0) {
            query_string= line.substring(qstart);
          }
          else {
            query_string= line.substring(qstart, qend);
          }
          if (DEBUG) {System.out.println("bookmark query_string = " + query_string);}
          int cstart = line.indexOf(">", qend) + 1;
          int cend = line.indexOf("</A>", cstart);
          String label_string = line.substring(cstart, cend);
          if (DEBUG) {System.out.println("bookmark label_string = " + label_string);}
          try { // if a MalformedURLExcption occurs, the bookmark won't be loaded
            Bookmark bm = new Bookmark(label_string, query_string);
            bm_list.addBookmark(bm);
          } catch (java.net.MalformedURLException mfe) {
            System.err.println("Bad URL: "+query_string);
          }
        }
      }
    } finally {
		  GeneralUtils.safeClose(br);
		  GeneralUtils.safeClose(isr);
		  GeneralUtils.safeClose(fis);
	  }
  }  
}
