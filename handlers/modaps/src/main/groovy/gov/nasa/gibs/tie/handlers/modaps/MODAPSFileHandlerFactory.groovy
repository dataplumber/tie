package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating MODAPS File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODAPSFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(MODAPSFileHandlerFactory.class)

   public MODAPSFileHandlerFactory() {
      logger.debug("MODAPSFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof MODAPSProductType)) {
         throw new DataHandlerException(
               'Input product type is not a MODAPS product type object.')
      }
      return new MODAPSFileHandler(productType as MODAPSProductType)
   }
}
