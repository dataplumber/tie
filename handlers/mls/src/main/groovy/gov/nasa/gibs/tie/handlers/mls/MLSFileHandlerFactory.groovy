package gov.nasa.gibs.tie.handlers.mls

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating MLS File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class MLSFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(MLSFileHandlerFactory.class)

   public MLSFileHandlerFactory() {
      logger.debug("MLSFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof MLSProductType)) {
         throw new DataHandlerException(
               'Input product type is not a MLS product type object.')
      }
      return new MLSFileHandler(productType as MLSProductType)
   }
}
