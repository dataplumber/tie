/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating SipsImagery File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(SipsImageryFileHandlerFactory.class)

   public SipsImageryFileHandlerFactory() {
      logger.debug("SipsImageryFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof SipsImageryProductType)) {
         throw new DataHandlerException(
               'Input product type is not a SipsImagery product type object.')
      }
      return new SipsImageryFileHandler(productType as SipsImageryProductType)
   }
}
