package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating gov.nasa.gibs.tie.handlers.sftp.SFTP File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class SFTPFileHandlerFactory implements FileHandlerFactory {

   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPFileHandlerFactory.class)

   public SFTPFileHandlerFactory() {
      logger.debug("gov.nasa.gibs.tie.handlers.sftp.SFTPFileHandlerFactory created")
   }

   @Override
   FileHandler createFileHandler(ProductType productType)
   throws DataHandlerException {
      if (!(productType instanceof gov.nasa.gibs.tie.handlers.sftp.SFTPProductType)) {
         throw new DataHandlerException(
               'Input product type is not a gov.nasa.gibs.tie.handlers.sftp.SFTP product type object.')
      }
      return new gov.nasa.gibs.tie.handlers.sftp.SFTPFileHandler(productType as gov.nasa.gibs.tie.handlers.sftp.SFTPProductType)
   }
}
