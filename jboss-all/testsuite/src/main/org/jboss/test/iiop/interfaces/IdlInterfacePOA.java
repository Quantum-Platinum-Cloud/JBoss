package org.jboss.test.iiop.interfaces;


/**
* org/jboss/test/iiop/interfaces/IdlInterfacePOA.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from IdlInterface.idl
* Tuesday, October 21, 2003 3:27:17 PM BRST
*/

public abstract class IdlInterfacePOA extends org.omg.PortableServer.Servant
 implements org.jboss.test.iiop.interfaces.IdlInterfaceOperations, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("echo", new java.lang.Integer (0));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {
       case 0:  // org/jboss/test/iiop/interfaces/IdlInterface/echo
       {
         String s = in.read_string ();
         String $result = null;
         $result = this.echo (s);
         out = $rh.createReply();
         out.write_string ($result);
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:org/jboss/test/iiop/interfaces/IdlInterface:1.0"};

  public String[] _all_interfaces (org.omg.PortableServer.POA poa, byte[] objectId)
  {
    return (String[])__ids.clone ();
  }

  public IdlInterface _this() 
  {
    return IdlInterfaceHelper.narrow(
    super._this_object());
  }

  public IdlInterface _this(org.omg.CORBA.ORB orb) 
  {
    return IdlInterfaceHelper.narrow(
    super._this_object(orb));
  }


} // class IdlInterfacePOA