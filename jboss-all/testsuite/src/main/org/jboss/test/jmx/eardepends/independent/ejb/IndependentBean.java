/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 */
package org.jboss.test.jmx.eardepends.independent.ejb;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * @author vescoc
 * 
 * @ejb.bean description = "Independent SessionBean"
 *           display-name = "Independent SessionBean"
 *           name = "IndependentEJB"
 *           view-type = "remote"
 *           jndi-name = "test/Independent"
 */
public class IndependentBean implements SessionBean
{
   /**
    * 
    * @throws CreateException
    * 
    * @ejb.create-method
    */
   public void ejbCreate() throws CreateException
   {
   }

   /* (non-Javadoc)
    * @see javax.ejb.SessionBean#ejbActivate()
    */
   public void ejbActivate()
   {
   }

   /* (non-Javadoc)
    * @see javax.ejb.SessionBean#ejbPassivate()
    */
   public void ejbPassivate()
   {
   }

   /* (non-Javadoc)
    * @see javax.ejb.SessionBean#ejbRemove()
    */
   public void ejbRemove()
   {
   }

   /* (non-Javadoc)
    * @see javax.ejb.SessionBean#setSessionContext(javax.ejb.SessionContext)
    */
   public void setSessionContext(SessionContext ctx)
   {
   }
   
   /**
    * 
    * @param value
    * 
    * @ejb.interface-method
    * @return
    */
   public Object echo(Object value)
   {
      return value;
   }
}
