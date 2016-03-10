package gov.nasa.gibs.tie.handlers.cmr

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the CMR Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class CMRProductTypeFactory implements ProductTypeFactory {
	private static Log log = LogFactory.getLog(CMRProductTypeFactory.class)

	public CMRProductTypeFactory() {
		log.debug("CMRProductTypeFactory created.")
	}

	@Override
	ProductType createProductType(String name) {
		CMRProductType pt = new CMRProductType(name);
		return pt;
	}

	@Override
	Map<String, ProductType> createProductTypes(ApplicationConfigurator configurator,
			String productTypeConfig) {
		Map<String, ProductType> result = [:]
		try {
			def pts = new XmlSlurper().parseText(new File(productTypeConfig).text)
			pts.productTypes.each { def productTypes ->
				String sigevent = productTypes.sigevent
				productTypes.productType.each { productType ->

					CMRProductType pt = new CMRProductType(productType.name.toString())

					pt.configurator = configurator
                    
                    //Get bounding box values from config
                    pt.left_lon = productType.bbox.left_lon.toString() as Long
                    pt.lower_lat = productType.bbox.lower_lat.toString() as Long
                    pt.right_lon = productType.bbox.right_lon.toString() as Long
                    pt.upper_lat = productType.bbox.upper_lat.toString() as Long
                    
                    //Concat the values above for a wms_url string replacement below!
                    def bboxString = pt.left_lon.toString() + "," + pt.lower_lat.toString() + "," + pt.right_lon.toString() + "," + pt.upper_lat.toString()
                    
                    pt.width = productType.width.toString() as Long
                    pt.height = productType.height.toString() as Long
					log.debug("Done initial configuration.  Set attributes")
					pt.cmrURL = productType.cmrURL.toString().replace('${collectionId}', productType.collection.toString())
								.replace('${pageSize}', productType.cmrPageSize.toString())
					pt.sourceURL = productType.wmsURL.toString().replace('${layer}', productType.layer.toString())
			   					.replace('${bbox}', bboxString)
								.replace('${width}', productType.width.toString())
								.replace('${height}', productType.height.toString())		
								
					if( !productType.colormapParam.equals("") ) {
						pt.sourceURL += "&${productType.colormapParam.type.toString()}=${productType.colormapParam.value.toString()}"
						log.debug("WMS URL constructed from config values: ${pt.sourceURL}")
					}
					
					pt.initializeWmsDateTimeFormat(productType.dateTimeFormat.toString())
								
					pt.sigEventURL = sigevent
					pt.interval = (productType.interval).toInteger()

					log.debug("Invoke product type setup")
					pt.setup()

					log.debug("Register product type to map")

					result[pt.name] = pt
					log.debug("ProductType[${pt.name}] created.")
				}
			}
		} catch (Exception e) {
			log.error("Unable to process configuration file: ${System.getProperty(productTypeConfig)}", e)
		}
		return result
	}


}
