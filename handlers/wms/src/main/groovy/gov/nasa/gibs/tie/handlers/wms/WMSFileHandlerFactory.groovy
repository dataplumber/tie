package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.FileHandlerFactory
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of File Handler Factory for creating WMS File Handlers
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSFileHandlerFactory implements FileHandlerFactory {

	private static Log logger = LogFactory.getLog(WMSFileHandlerFactory.class)

	public WMSFileHandlerFactory() {
		logger.debug("WMSFileHandlerFactory created")
	}

	@Override
	FileHandler createFileHandler(ProductType productType)
	throws DataHandlerException {
		if (!(productType instanceof WMSProductType)) {
			throw new DataHandlerException(
			'Input product type is not a WMS product type object.')
		}
		return new WMSFileHandler(productType as WMSProductType)
	}
}
