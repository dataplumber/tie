package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of MODAPS metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODAPSMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(MODAPSMetadataHarvesterFactory.class)

   public MODAPSMetadataHarvesterFactory() {
      logger.debug("MODAPSMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new MODAPSMetadata()
   }
}
