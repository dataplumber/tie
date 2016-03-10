package gov.nasa.gibs.tie.handlers.omi

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating OMI File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class OMIFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(OMIFileHandlerFactory.class)

   public OMIFileHandlerFactory() {
      logger.debug("OMIFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof OMIProductType)) {
         throw new DataHandlerException(
               'Input product type is not a OMI product type object.')
      }
      return new OMIFileHandler(productType as OMIProductType)
   }
}
