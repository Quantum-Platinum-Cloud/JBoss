/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/

package org.jboss.iiop.rmi.marshal.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.rmi.UnexpectedException;
import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.UserException;
import org.omg.CORBA.portable.IDLEntity;
import org.omg.CORBA_2_3.portable.InputStream;
import org.omg.CORBA_2_3.portable.OutputStream;

import org.jboss.iiop.rmi.marshal.CDRStream;
import org.jboss.iiop.rmi.marshal.CDRStreamReader;
import org.jboss.iiop.rmi.marshal.CDRStreamWriter;

/**
 * An <code>StubStrategy</code> for a given method knows how to marshal
 * the sequence of method parameters into a CDR output stream, how to unmarshal
 * from a CDR input stream the return value of the method, and how to unmarshal
 * from a CDR input stream an application exception thrown by the method.
 *
 * @author  <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision: 1.1.2.2 $
 */
public class StubStrategy
{
   // Fields ------------------------------------------------------------------

   /**
    * Each <code>CDRStreamWriter</code> in the array marshals a method 
    * parameter.
    */
   private CDRStreamWriter[] paramWriters;

   /**
    * List of exception classes.
    */
   private List exceptionList;

   /**
    * Maps exception repository ids into ExceptionReader instances.
    */
   private Map exceptionMap;

   /**
    * A <code>CDRStreamReader</code> that unmarshals the return value of the 
    * method.
    */
   private CDRStreamReader retvalReader;

   /**
    * If this <code>StubStrategy</code> is for a method that returns a
    * remote interface, this field contains the remote interface's 
    * <code>Class</code>. Otherwise it contains null.
    */
   private Class retvalRemoteInterface;
   // Static ------------------------------------------------------------------

   /**
    * Returns an <code>StubStrategy</code> for a method, given descriptions
    * of the method parameters, exceptions, and return value. Parameter and 
    * return value descriptions are "marshaller abbreviated names".
    *
    * @param paramTypes  a string array with marshaller abbreviated names for
    *                    the method parameters
    * @param excepIds    a string array with the CORBA repository ids of the
    *                    exceptions thrown by the method
    * @param excepTypes  a string array with the Java class names of the 
    *                    exceptions thrown by the method
    * @param retvalType  marshaller abbreaviated name for the return value of
    *                    the method
    * @param cl          a <code>ClassLoader</code> to load value classes 
    *                    (if null, the current thread's context class loader 
    *                    will be used)
    * @return an <code>StubStrategy</code> for the operation with the 
    * parameters, exceptions, and return value specified.
    * @see org.jboss.iiop.marshal.CDRStream#abbrevFor(Class clz)
    */
   public static StubStrategy forMethod(String[] paramTypes, 
                                        String[] excepIds,
                                        String[] excepTypes, 
                                        String retvalType, 
                                        ClassLoader cl) 
   {
      // This "factory method" exists just because I have found it easier 
      // to invoke a static method (rather than invoking operator new) 
      // from a stub class dynamically assembled by an instance of
      // org.jboss.proxy.ProxyAssembler.

      return new StubStrategy(paramTypes, excepIds, 
                              excepTypes, retvalType, cl);
   }


   // Constructor -------------------------------------------------------------

   /**
    * Constructs an <code>StubStrategy</code> for a method, given 
    * descriptions of the method parameters, exceptions, and return value. 
    * Parameter and return value descriptions are "marshaller abbreviated 
    * names".
    *
    * @param paramTypes  a string array with marshaller abbreviated names for
    *                    the method parameters
    * @param excepIds    a string array with the CORBA repository ids of the
    *                    exceptions thrown by the method
    * @param excepTypes  a string array with the Java class names of the 
    *                    exceptions thrown by the method
    * @param retvalType  marshaller abbreaviated name for the return value of
    *                    the method
    * @param cl          a <code>ClassLoader</code> to load value classes 
    *                    (if null, the current thread's context class loader 
    *                    will be used)
    * @see org.jboss.iiop.marshal.CDRStream#abbrevFor(Class clz)
    */
   private StubStrategy(String[] paramTypes, String[] excepIds, 
                        String[] excepTypes, String retvalType, 
                        ClassLoader cl) 
   {
      if (cl == null) {
         cl = Thread.currentThread().getContextClassLoader();
      }
      
      // Initialize paramWriters
      int len = paramTypes.length;
      paramWriters = new CDRStreamWriter[len];
      for (int i = 0; i < len; i++) {
            paramWriters[i] = CDRStream.writerFor(paramTypes[i], cl);
      }

      // Initialize exception list and exception map
      exceptionList = new ArrayList();
      exceptionMap = new HashMap();
      len = excepIds.length;
      for (int i = 0; i < len; i++) {
         try {
            Class clz = cl.loadClass(excepTypes[i]);
            exceptionList.add(clz);
            ExceptionReader exceptionReader = 
               new ExceptionReader(clz, excepIds[i]);
            exceptionMap.put(exceptionReader.getReposId(), exceptionReader);
         }
         catch (ClassNotFoundException e) {
            throw new RuntimeException("Error loading class " 
                                       + excepTypes[i] + ": " + e);
         }
      }

      // Initialize retvalReader
      retvalReader = CDRStream.readerFor(retvalType, cl);

      // Initialize retvalRemoteInterface
      if (retvalType.charAt(0) == 'R') {
         try {
            retvalRemoteInterface = cl.loadClass(retvalType.substring(1));
         }
         catch (ClassNotFoundException e) {
            throw new RuntimeException("Error loading class " 
                                       + retvalType.substring(1) + ": " + e);
         }
      }
   }

   // Public  -----------------------------------------------------------------

   /**
    * Marshals the sequence of method parameters into an output stream.
    *
    * @param out    a CDR output stream
    * @param params an object array with the parameters.
    */
   public void writeParams(OutputStream out, Object[] params) 
   {
      int len = params.length;
      
      if (len != paramWriters.length) {
          throw new RuntimeException("Cannot marshal parameters: "
                                     + "unexpected number of parameters");
      }
      for (int i = 0; i < len; i++ ) {
         paramWriters[i].write(out, params[i]);
      }
   }
   
   /**
    * Returns true if this <code>StubStrategy</code>'s method is non void.
    */
   public boolean isNonVoid() 
   {
      return (retvalReader != null);
   }

   /**
    * Unmarshals from an input stream the return value of the method.
    *
    * @param in    a CDR input stream
    * @return      a value unmarshaled from the stream.
    */
   public Object readRetval(InputStream in) 
   {
      return retvalReader.read(in);
   }

   /**
    * Unmarshals from an input stream an exception thrown by the method.
    *
    * @param id    the repository id of the exception to unmarshal
    * @param in    a CDR input stream
    * @return      an exception unmarshaled from the stream.
    */
   public Exception readException(String id, InputStream in) 
   {
      ExceptionReader exceptionReader = (ExceptionReader)exceptionMap.get(id);
      if (exceptionReader == null) {
         return new UnexpectedException(id);
      }
      else {
         return exceptionReader.read(in);
      }
   }

   /**
    * Checks if a given <code>Throwable</code> instance corresponds to an 
    * exception declared by this <code>StubStrategy</code>'s method.
    *
    * @param t     an exception class
    * @return      true if <code>t</code> is an instance of any of the 
    *              exceptions declared by this <code>StubStrategy</code>'s 
    *              method, false otherwise.
    */
   public boolean isDeclaredException(Throwable t)
   {
      Iterator iterator = exceptionList.iterator();
      while (iterator.hasNext()) {
         if (((Class)iterator.next()).isInstance(t)) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Converts the return value of a local invocation into the expected type.
    * A conversion is needed if the return value is a remote interface
    * (in this case <code>PortableRemoteObject.narrow()</code> must be called).
    * 
    * @param obj the return value to be converted
    * @return the converted value.
    */
   public Object convertLocalRetval(Object obj) 
   {
      if (retvalRemoteInterface == null)
         return obj;
      else 
         return PortableRemoteObject.narrow(obj, retvalRemoteInterface);
   }
   
   // Static inner class (private) --------------------------------------------

   /**
    * An <code>ExceptionReader</code> knows how to read exceptions of a given
    * class from a CDR input stream.
    */
   private static class ExceptionReader
   {
      /**
       * The exception class.
       */
      private Class clz;
  
      /**
       * The CORBA repository id of the exception class.
       */
      private String reposId;
      
      /*
       * If the exception class corresponds to an IDL-defined exception, this
       * field contains the read method of the associated helper class. 
       * A null value indicates that the exception class does not correspond
       * to an IDL-defined exception.
       */
      private java.lang.reflect.Method readMethod = null;

      /**
       * Constructs an <code>ExceptionReader</code> for a given exception 
       * class.
       */
      ExceptionReader(Class clz, String reposId) 
      {
         this.clz = clz;
         if (IDLEntity.class.isAssignableFrom(clz) 
             && UserException.class.isAssignableFrom(clz)) {

            // This ExceptionReader corresponds to an IDL-defined exception
            String helperClassName = clz.getName() + "Helper";
            try {
               Class helperClass =
                  clz.getClassLoader().loadClass(helperClassName);
               Class[] paramTypes =
                  { org.omg.CORBA.portable.InputStream.class };
               readMethod = helperClass.getMethod("read", paramTypes);

               // Ignore the reposId parameter and use the id
               // returned by the IDL-generated helper class
               java.lang.reflect.Method idMethod =
                  helperClass.getMethod("id", null);
               this.reposId = (String)idMethod.invoke(null, null);
            }
            catch (ClassNotFoundException e) {
               throw new RuntimeException("Error loading class " 
                                          + helperClassName + ": " + e);
            }
            catch (NoSuchMethodException e) {
                throw new RuntimeException("No read/id method in helper class "
                                           + helperClassName + ": " + e);
            }
            catch (IllegalAccessException e) {
               throw new RuntimeException("Internal error: " + e);
            }
            catch (java.lang.reflect.InvocationTargetException e) {
               throw new RuntimeException("Exception in call to " 
                                          + helperClassName + ": "
                                          + e.getTargetException());
            }
         }
         else {
            // This ExceptionReader does not correspond to an IDL-defined 
            // exception: store the reposId parameter
            this.reposId = reposId;
         }
      }

      public String getReposId()
      {
         return reposId;
      }
      
      /**
       * Reads an exception from a CDR input stream.
       */
      public Exception read(InputStream in) 
      {
         if (readMethod != null) {
            try {
               return (Exception)readMethod.invoke(null, new Object[] { in });
            }
            catch (IllegalAccessException e) {
               throw new RuntimeException("Internal error: " + e);
            }
            catch (java.lang.reflect.InvocationTargetException e) {
               throw new RuntimeException("Exception unmarshaling IDLEntity: "
                                          + e.getTargetException());
            }
         }
         else {
            in.read_string(); // read and discard the repository id
            return (Exception)in.read_value(clz);
         }
      }

   } // end of inner class ExceptionReader

}