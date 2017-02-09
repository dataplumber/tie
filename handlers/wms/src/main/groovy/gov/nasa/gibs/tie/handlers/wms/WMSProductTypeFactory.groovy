package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the WMS Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSProductTypeFactory implements ProductTypeFactory {
	private static Log log = LogFactory.getLog(WMSProductTypeFactory.class)

	public WMSProductTypeFactory() {
		log.debug("WMSProductTypeFactory created.")
	}

	@Override
	ProductType createProductType(String name) {
		WMSProductType pt = new WMSProductType(name);
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
				
				boolean detectionTypeError = true
				String detectionType
				if(productTypes.detectionType) {
					detectionType = productTypes.detectionType.toString()
					if(detectionType.equals("NRT") || detectionType.equals("CMR")) {
						detectionTypeError = false
					}
				}
				if(detectionTypeError) {
					log.error("Detection type is missing or invalid! tie_producttypes.xml must specify NRT or CMR detection mode.")
					System.exit(-1)
				}
		
				int cacheRetention = (productTypes.cacheRetention).toInteger()
				productTypes.productType.each { productType ->

					WMSProductType pt = new WMSProductType(productType.name.toString())
					
					pt.detectionType = detectionType
					pt.cacheRetention = cacheRetention
							log.trace("cacheRetention set on product type: ${pt.cacheRetention}")
					pt.configurator = configurator
                    
                    //Get bounding box values from config
                    pt.left_x = productType.bbox.left_x.toString() as Long
                    pt.lower_y = productType.bbox.lower_y.toString() as Long
                    pt.right_x = productType.bbox.right_x.toString() as Long
                    pt.upper_y = productType.bbox.upper_y.toString() as Long
                    
                    //Concat the values above for a wms_url string replacement below!
                    def bboxString = pt.left_x.toString() + "," + pt.lower_y.toString() + "," + pt.right_x.toString() + "," + pt.upper_y.toString()
                    
                    pt.width = productType.width.toString() as Long
                    pt.height = productType.height.toString() as Long
					log.debug("Done initial configuration.  Set attributes")
					
					if(detectionType.equals("CMR")) {
						pt.cmrURL = productType.cmrURL.toString().replace('${collectionId}', productType.collection.toString())
							.replace('${pageSize}', productType.cmrPageSize.toString())
					}
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

					boolean formatError = true
					String format
					if(productType.format) {
						format = productType.format.toString().toLowerCase()
						if(format.equals("png") || format.equals("jpeg") || format.equals("jpg")) {
							formatError = false
							pt.fileExtension = format.equals("jpeg") ? ".jpg" : ".${format}"
						}
					}
					if(formatError) {
						log.error("File format is missing or invalid! tie_producttypes.xml must specify PNG or JPEG format.")
						System.exit(-1)
					}
					
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
