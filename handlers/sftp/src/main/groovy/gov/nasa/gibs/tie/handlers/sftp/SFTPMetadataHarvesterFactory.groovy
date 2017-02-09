package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of gov.nasa.gibs.tie.handlers.sftp.SFTP metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class SFTPMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPMetadataHarvesterFactory.class)

   public SFTPMetadataHarvesterFactory() {
      logger.debug("gov.nasa.gibs.tie.handlers.sftp.SFTPMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new gov.nasa.gibs.tie.handlers.sftp.SFTPMetadata()
   }
}
