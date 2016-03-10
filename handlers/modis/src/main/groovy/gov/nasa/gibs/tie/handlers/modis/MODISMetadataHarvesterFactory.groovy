package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of MODIS metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODISMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(MODISMetadataHarvesterFactory.class)

   public MODISMetadataHarvesterFactory() {
      logger.debug("MODISMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new MODISMetadata()
   }
}
