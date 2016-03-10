/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of SipsImagery metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryMetadataHarvesterFactory implements MetadataHarvesterFactory {

   private static Log logger = LogFactory.getLog(SipsImageryMetadataHarvesterFactory.class)

   public SipsImageryMetadataHarvesterFactory() {
      logger.debug("SipsImageryMetadataHarvesterFactory created.")
   }

   @Override
   MetadataHarvester createMetadataHarvester() {
      return new SipsImageryMetadata()
   }
}
