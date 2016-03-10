/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating PDR File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class PDRFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(PDRFileHandlerFactory.class)

   public PDRFileHandlerFactory() {
      logger.debug("PDRFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
         throws DataHandlerException {
      if (!(productType instanceof PDRProductType)) {
         throw new DataHandlerException(
               'Input product type is not a PDR product type object.')
      }
      return new PDRFileHandler(productType as PDRProductType)
   }
}
