package com.affymetrix.igb.util;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;

public final class ThreadUtils {
  static Map<Object,Executor> obj2exec = new HashMap<Object,Executor>();

  /**
   *   Gets the primary executor for a given object key
   *   Creates a new exector if didn't exist before
   *   Currently returns  an Executor that uses a single worker thread operating off an unbounded queue
   *      therefore tasks (Runnables added via exec.execute()) on the Executor are guaranteed to 
   *      execute sequentially in order, and no more than one task will be active at any given time
   */
  public synchronized static Executor getPrimaryExecutor(Object key) {
    Executor exec = obj2exec.get(key);
    if (exec == null) {
      exec = Executors.newSingleThreadExecutor();
      obj2exec.put(key, exec);
    }
    return exec;
  }

  public static void runOnEventQueue(Runnable r) {
	  if (SwingUtilities.isEventDispatchThread()) {
		  r.run();
	  } else {
		  SwingUtilities.invokeLater(r);
	  }
  }
}
