package gov.nasa.gibs.tie.handlers.cmr

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of CMR metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class CMRMetadataHarvesterFactory implements MetadataHarvesterFactory {

	private static Log logger = LogFactory.getLog(CMRMetadataHarvesterFactory.class)

	public CMRMetadataHarvesterFactory() {
		logger.debug("CMRMetadataHarvesterFactory created.")
	}

	@Override
	MetadataHarvester createMetadataHarvester() {
		return new CMRMetadata()
	}
}
