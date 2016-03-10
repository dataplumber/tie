package gov.nasa.gibs.tie.handlers.amsr2

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of gov.nasa.gibs.tie.handlers.amsr2.AMSR2 metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class AMSR2MetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.amsr2.AMSR2MetadataHarvesterFactory.class)

   public AMSR2MetadataHarvesterFactory() {
      logger.debug("gov.nasa.gibs.tie.handlers.amsr2.AMSR2MetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new gov.nasa.gibs.tie.handlers.amsr2.AMSR2Metadata()
   }
}
