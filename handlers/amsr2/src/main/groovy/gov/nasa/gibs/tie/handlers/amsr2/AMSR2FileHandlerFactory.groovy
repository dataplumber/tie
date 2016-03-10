package gov.nasa.gibs.tie.handlers.amsr2

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating gov.nasa.gibs.tie.handlers.amsr2.AMSR2 File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class AMSR2FileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.amsr2.AMSR2FileHandlerFactory.class)

   public AMSR2FileHandlerFactory() {
      logger.debug("gov.nasa.gibs.tie.handlers.amsr2.AMSR2FileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof gov.nasa.gibs.tie.handlers.amsr2.AMSR2ProductType)) {
         throw new DataHandlerException(
               'Input product type is not a gov.nasa.gibs.tie.handlers.amsr2.AMSR2 product type object.')
      }
      return new gov.nasa.gibs.tie.handlers.amsr2.AMSR2FileHandler(productType as gov.nasa.gibs.tie.handlers.amsr2.AMSR2ProductType)
   }
}
