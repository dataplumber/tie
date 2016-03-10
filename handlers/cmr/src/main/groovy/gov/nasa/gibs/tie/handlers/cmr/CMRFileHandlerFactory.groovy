package gov.nasa.gibs.tie.handlers.cmr

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating CMR File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class CMRFileHandlerFactory implements FileHandlerFactory {

	private static Log logger = LogFactory.getLog(CMRFileHandlerFactory.class)

	public CMRFileHandlerFactory() {
		logger.debug("CMRFileHandlerFactory created")
	}

	@Override
	FileHandler createFileHandler(ProductType productType)
	throws DataHandlerException {
		if (!(productType instanceof CMRProductType)) {
			throw new DataHandlerException(
			'Input product type is not a CMR product type object.')
		}
		return new CMRFileHandler(productType as CMRProductType)
	}
}
