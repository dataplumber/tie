package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.MetadataHarvesterFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of WMS metadata harvester factory
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSMetadataHarvesterFactory implements MetadataHarvesterFactory {

	private static Log logger = LogFactory.getLog(WMSMetadataHarvesterFactory.class)

	public WMSMetadataHarvesterFactory() {
		logger.debug("WMSMetadataHarvesterFactory created.")
	}

	@Override
	MetadataHarvester createMetadataHarvester() {
		return new WMSMetadata()
	}
}
