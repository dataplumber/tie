package gov.nasa.gibs.tie.handlers.mls

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of MLS metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class MLSMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(MLSMetadataHarvesterFactory.class)

   public MLSMetadataHarvesterFactory() {
      logger.debug("MLSMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new MLSMetadata()
   }
}
