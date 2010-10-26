package com.affymetrix.igb.bookmarks;


/**
 * @version $Id$
 */
final class SymBookmark {
      private final String server;
      private final String path;
      private final boolean isgraph;

      SymBookmark(String server, String path, boolean isgraph){
			this.server = server;
			this.path = path;
			this.isgraph = isgraph;
      }

	  
      String getServer(){
          return server;
      }
            
      String getPath(){
          return path;
      }
        
	  boolean isGraph(){
		  return isgraph;
	  }
}

