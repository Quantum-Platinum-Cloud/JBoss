/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.ejb.plugins.cmp.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.ejb.EJBException;

import org.jboss.ejb.EntityEnterpriseContext;
import org.jboss.ejb.plugins.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.ejb.plugins.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.logging.Logger;

/**
 * JDBCStoreEntityCommand updates the row with the new state.
 * In the event that no field is dirty the command just returns.
 * Note: read-only fields are never considered dirty.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard �berg</a>
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @author <a href="mailto:shevlandj@kpi.com.au">Joe Shevland</a>
 * @author <a href="mailto:justin@j-m-f.demon.co.uk">Justin Forder</a>
 * @author <a href="mailto:sebastien.alborini@m4x.org">Sebastien Alborini</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 1.13.2.15 $
 */
public final class JDBCStoreEntityCommand
{
   private final JDBCEntityBridge entity;
   private final JDBCCMPFieldBridge[] primaryKeyFields;
   private final Logger log;

   public JDBCStoreEntityCommand(JDBCStoreManager manager)
   {
      entity = manager.getEntityBridge();
      primaryKeyFields = entity.getPrimaryKeyFields();

      // Create the Log
      log = Logger.getLogger(
         this.getClass().getName() +
         "." +
         manager.getMetaData().getName());
   }

   public void execute(EntityEnterpriseContext ctx)
   {
      // scheduled for batch cascade-delete instance should not be updated
      // because foreign key fields could be updated to null and cascade-delete will fail.
      if(!entity.isDirty(ctx) || entity.isScheduledForBatchCascadeDelete(ctx))
      {
         if(log.isTraceEnabled())
         {
            log.trace("Store command NOT executed. Entity is not dirty "
               + " or scheduled for *batch* cascade delete: pk=" + ctx.getId());
         }
         return;
      }

      JDBCEntityBridge.FieldIterator dirtyIterator = entity.getDirtyIterator(ctx);

      // generate sql
      StringBuffer sql = new StringBuffer(200);
      sql.append(SQLUtil.UPDATE)
         .append(entity.getTableName())
         .append(SQLUtil.SET);
      SQLUtil.getSetClause(dirtyIterator, sql)
         .append(SQLUtil.WHERE);
      SQLUtil.getWhereClause(primaryKeyFields, sql);

      boolean hasLockedFields = entity.hasLockedFields(ctx);
      JDBCEntityBridge.FieldIterator lockedIterator = null;
      if(hasLockedFields)
      {
         lockedIterator = entity.getLockedIterator(ctx);
         while(lockedIterator.hasNext())
         {
            sql.append(SQLUtil.AND);
            JDBCCMPFieldBridge field = lockedIterator.next();
            if(field.getLockedValue(ctx) == null)
            {
               SQLUtil.getIsNullClause(false, field, "", sql);
               lockedIterator.remove();
            }
            else
            {
               SQLUtil.getWhereClause(field, sql);
            }
         }
      }

      Connection con = null;
      PreparedStatement ps = null;
      int rowsAffected = 0;
      try
      {
         // create the statement
         if(log.isDebugEnabled())
         {
            log.debug("Executing SQL: " + sql);
         }

         // get the connection
         con = entity.getDataSource().getConnection();
         ps = con.prepareStatement(sql.toString());

         // SET: set the dirty fields parameters
         int index = 1;
         dirtyIterator.reset();
         while(dirtyIterator.hasNext())
         {
            index = dirtyIterator.next().setInstanceParameters(ps, index, ctx);
         }

         // WHERE: set primary key fields
         index = entity.setPrimaryKeyParameters(ps, index, ctx.getId());

         // WHERE: set optimistically locked field values
         if(hasLockedFields)
         {
            lockedIterator.reset();
            while(lockedIterator.hasNext())
            {
               JDBCCMPFieldBridge field = lockedIterator.next();
               Object value = field.getLockedValue(ctx);
               index = field.setArgumentParameters(ps, index, value);
            }
         }

         // execute statement
         rowsAffected = ps.executeUpdate();
      }
      catch(EJBException e)
      {
         throw e;
      }
      catch(Exception e)
      {
         throw new EJBException("Store failed", e);
      }
      finally
      {
         JDBCUtil.safeClose(ps);
         JDBCUtil.safeClose(con);
      }

      // check results
      if(rowsAffected != 1)
      {
         throw new EJBException("Update failed. Expected one " +
            "affected row: rowsAffected=" + rowsAffected +
            "id=" + ctx.getId());
      }
      if(log.isDebugEnabled())
      {
         log.debug("Rows affected = " + rowsAffected);
      }

      // Mark the updated fields as clean.
      dirtyIterator.reset();
      while(dirtyIterator.hasNext())
      {
         dirtyIterator.next().setClean(ctx);
      }
   }
}
