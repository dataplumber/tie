package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of AIRS metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class AIRSMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(AIRSMetadataHarvesterFactory.class)

   public AIRSMetadataHarvesterFactory() {
      logger.debug("AIRSMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new AIRSMetadata()
   }
}
