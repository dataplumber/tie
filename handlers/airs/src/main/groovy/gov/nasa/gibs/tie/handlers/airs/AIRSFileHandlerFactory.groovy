package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating AIRS File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class AIRSFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(AIRSFileHandlerFactory.class)

   public AIRSFileHandlerFactory() {
      logger.debug("AIRSFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof AIRSProductType)) {
         throw new DataHandlerException(
               'Input product type is not a AIRS product type object.')
      }
      return new AIRSFileHandler(productType as AIRSProductType)
   }
}
