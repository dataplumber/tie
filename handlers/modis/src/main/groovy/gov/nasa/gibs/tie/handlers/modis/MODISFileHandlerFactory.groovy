package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating MODIS File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODISFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(MODISFileHandlerFactory.class)

   public MODISFileHandlerFactory() {
      logger.debug("MODISFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof MODISProductType)) {
         throw new DataHandlerException(
               'Input product type is not a MODIS product type object.')
      }
      return new MODISFileHandler(productType as MODISProductType)
   }
}
