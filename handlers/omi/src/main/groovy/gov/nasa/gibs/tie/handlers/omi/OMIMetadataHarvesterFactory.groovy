package gov.nasa.gibs.tie.handlers.omi

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of OMI metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class OMIMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(OMIMetadataHarvesterFactory.class)

   public OMIMetadataHarvesterFactory() {
      logger.debug("OMIMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new OMIMetadata()
   }
}
