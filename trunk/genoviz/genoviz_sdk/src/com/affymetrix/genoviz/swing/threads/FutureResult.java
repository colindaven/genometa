/*
  File: FutureResult.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  30Jun1998  dl               Create public version
*/

package com.affymetrix.genoviz.swing.threads;
import java.lang.reflect.*;

/**
 * A  class maintaining a single reference variable serving as the result
 * of an operation. The result cannot be accessed until it has been set.
 * <p>
 * <b>Sample Usage</b> <p>
 * <pre>
 * class ImageRenderer { Image render(byte[] raw); }
 * class App {
 *   Executor executor = ...
 *   ImageRenderer renderer = ...
 *   void display(byte[] rawimage) {
 *     try {
 *       FutureResult futureImage = new FutureResult();
 *       Runnable command = futureImage.setter(new Callable() {
 *          public Object call() { return renderer.render(rawImage); }
 *       });
 *       executor.execute(command);
 *       drawBorders();             // do other things while executing
 *       drawCaption();
 *       drawImage((Image)(futureImage.get())); // use future
 *     }
 *     catch (InterruptedException ex) { return; }
 *     catch (InvocationTargetException ex) { cleanup(); return; }
 *   }
 * }
 * </pre>
 * <p>[<a href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
 **/

final class FutureResult {
  /** The result of the operation **/
  private Object value_ = null;
  
  /** Status -- true after first set **/
  private boolean ready_ = false;

  /** the exception encountered by operation producing result **/
  private InvocationTargetException exception_ = null;


  /** 
   * Return a Runnable object that, when run, will set the result value.
   * @param function - a Callable object whose result will be
   * held by this FutureResult.
   * @return A Runnable object that, when run, will call the
   * function and (eventually) set the result.
   **/

  Runnable setter(final Callable function) {
    return new Runnable() {
      public void run() {
        try {
          set(function.call());
        }
        catch(Throwable ex) {
          setException(ex);
        }
      }
    };
  }

  /** internal utility: either get the value or throw the exception **/
  private Object doGet() throws InvocationTargetException {
    if (exception_ != null) 
      throw exception_;
    else
      return value_; 
  }

  /**
   * Access the reference, waiting if necessary until it is ready.
   * @return current value
   * @exception InterruptedException if current thread has been interrupted
   * @exception InvocationTargetException if the operation
   * producing the value encountered an exception.
   **/
  synchronized Object get() 
    throws InterruptedException, InvocationTargetException {
    while (!ready_) wait();
    return doGet();
  }


  /**
   * Set the reference, and signal that it is ready. It is not
   * considered an error to set the value more than once,
   * but it is not something you would normally want to do.
   * @param newValue The value that will be returned by a subsequent get();
   **/
  private synchronized void set(Object newValue) {
    value_ = newValue;
    ready_ = true;
    notifyAll();
  }

  /**
   * Set the exception field, also setting ready status.
   * @param ex The exception. It will be reported out wrapped
   * within an InvocationTargetException 
   **/
  private synchronized void setException(Throwable ex) {
    exception_ = new InvocationTargetException(ex);
    ready_ = true;
    notifyAll();
  }

}



