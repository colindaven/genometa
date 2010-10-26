/*
 * InvokeUtils.java
 * 
 * Originally written by Joseph Bowbeer and released into the public domain.
 * This may be used for any purposes whatsoever without acknowledgment.
 */

package com.affymetrix.genoviz.swing.threads;

import java.awt.Component;
import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * More invoke-and-wait utilities for Swing. Background threads
 * may use these methods to get a value, or user input, from the
 * event-dispatch thread and transfer it to the background thread. 
 * 
 * @author  Joseph Bowbeer
 * @version 1.0
 */
public final class InvokeUtils {

    /**
     * Causes <i>function.call()</i> to be executed synchronously on the
     * AWT event dispatching thread and returns the result to the caller.
     * This call will block until all pending AWT events have been
     * processed and (then) <i>function.call()</i> returns. This method
     * should not be called from the EventDispatchThread.
     *
     * Note: If the Callable.call() method throws an uncaught exception
     * (on the event dispatching thread) it's caught and rethrown, as
     * an InvocationTargetException, on the caller's thread.
     * <p>
     * @exception  InterruptedException If we're interrupted while waiting for
     *             the event dispatching thread to finish excecuting <i>function.call()</i>
     * @exception  InvocationTargetException  If <i>function.call()</i> throws
     */
   
    /**
     * Causes <i>JOptionPane.showOptionDialog()</i> to be executed synchronously
     * on the AWT event dispatching thread and returns the result to the caller.
     * <p>
     * @exception  InterruptedException If we're interrupted while waiting for
     *             the user to select an option.
     */
    public static int invokeOptionDialog(
            final Component parentComponent,
            final Object message,
            final String title,
            final int optionType,
            final int messageType,
            final Icon icon,
            final Object[] options,
            final Object initialValue)
                throws InterruptedException {

        Callable showOptionDialog = new Callable() {
            public Object call() {
                int n = JOptionPane.showOptionDialog(parentComponent,
                            message, title, optionType, messageType,
                            icon, options, initialValue);
                return new Integer(n);
            }
        };

		try {
			if (SwingUtilities.isEventDispatchThread()) {
				throw new Error("Cannot call invokeAndWait from the event dispatcher thread");
			}

			FutureResult result = new FutureResult();
			SwingUtilities.invokeLater(result.setter(showOptionDialog));
			Object obj = result.get();

			return ((Integer) obj).intValue();
		} catch (InvocationTargetException e) {
			/* showOptionDialog doesn't throw checked exceptions
			so ex must be a RuntimeException or an Error. */
			Throwable ex = e.getTargetException();
			if (ex instanceof RuntimeException) {
				throw (RuntimeException) ex;
			} else {
				throw (Error) ex;
			}
		}
    }

}

