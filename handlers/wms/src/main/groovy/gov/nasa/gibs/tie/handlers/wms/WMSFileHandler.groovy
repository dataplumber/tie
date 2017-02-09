package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.LocalFileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.util.ChecksumUtility
import gov.nasa.horizon.common.httpfetch.api.HttpFileProduct
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.Product
import gov.nasa.horizon.sigevent.api.EventType
import gov.nasa.horizon.sigevent.api.SigEvent
import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.text.DecimalFormat
import java.awt.Image
import javax.imageio.ImageIO

/**
 * WMS file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSFileHandler implements FileHandler {

	private static Log logger = LogFactory.getLog(WMSFileHandler.class)

	private WMSProductType productType

	private SigEvent sigEvent
	
    String granuleId

	public WMSFileHandler(WMSProductType productType) {
		this.productType = productType
		this.sigEvent = new SigEvent(productType.sigEventURL)
	}

	@Override
	void preprocess() {

	}

	@Override
	void process(List<FileProduct> fps) throws DataHandlerException {

		String productName = fps.find { FileProduct fp ->
			fp.name.endsWith(productType.fileExtension)
		}.name
		productName = productName?.substring(0, productName.lastIndexOf(productType.fileExtension))

		if (!productName) {
			def fn = fps.collect {
				it.name
			}
			logger.warn("Incomplete product encountered.  Only contains ${fn}")
			return
		}

		Date timetag = new Date()
		String location = "${productType.dataStorage}${File.separator}${productName}${File.separator}"
		String shadow = "${productType.dataStorage}${File.separator}.shadow${File.separator}${productName}${File.separator}"

		// download to a shadow directory to prevent partial read by any scanners
		File s = new File(shadow)
		if (!s.exists()) {
			try {
				if (!s.mkdirs()) {
					logger.error("Unable to create shadow directory for download: ${shadow}")
					throw new DataHandlerException("Unable to create shadow directory for download: ${shadow}")
				}
			} catch (SecurityException e) {
				throw new DataHandlerException("Unable to create shadow directory for download: ${shadow}", e)
			}
		}

		Product product = new Product(productType, productName)
		product.shadowLocation = shadow
		product.stageLocation = location
		product.ingestStart = new Date()
		int max_retries = 5

		fps.each { HttpFileProduct fileProduct ->
			int retries = 1
			while (retries <= max_retries) {
				logger.debug("Start to download file ${fileProduct.name}.. attempt ${retries}")
				String shadowLocation = "${shadow}${File.separator}${fileProduct.name}"
				FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
				fileProduct.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
				//if (!alg) {
				//   alg = ChecksumUtility.DigestAlgorithm.MD5
				//}
				String checksum = ChecksumUtility.getDigest(fileProduct.digestAlgorithm, fileProduct.inputStream, fos)
				if (!checksum) {
					if (retries == max_retries) {
						logger.error("Unable to download file: " + fileProduct.friendlyURI)
						throw new DataHandlerException("Unable to download file after ${retries} attemps: " + fileProduct.friendlyURI)
					} else {
						logger.debug("Download unsuccessful.  Handler will retry...")
						// sleep 1 sec and retry
						sleep(1000)
					}
				} else {
					File sf = new File(shadowLocation)
					if (!sf.path.toLowerCase().endsWith("${productType.fileExtension}")) {
						logger.debug("No image file found at this time.")
						sf.delete()
					} else {
						try {
							Image image = ImageIO.read(sf);
							
							if (image == null) {
								logger.debug("The downlaoded file was not an image. Skip it.");
								sf.delete()
							} else {
								fileProduct.digestValue = checksum
								fileProduct.size = sf.size()
								CacheFileInfo cfi = new CacheFileInfo()
								cfi.product = productName
								cfi.name = fileProduct.name
								cfi.modified = fileProduct.lastModifiedTime
								cfi.size = fileProduct.size
								//cfi.granuleId = this.granuleId

								if (!(productType.batch || (fileProduct.lastModifiedTime < System.currentTimeMillis()) ) && productType.isInCache(cfi)) {
									logger.debug("File already previously downloaded.  Delete and skip it")
									sf.delete()
								} else {
									if(productType.batch) {logger.debug("Batch mode, ignoring initial cache check.")}
									product.addFileProduct(fileProduct)
		
									// download success
									logger.debug("Retrieved file: ${fileProduct.name}")
								}
								break
							}
						} catch(IOException ex) {
							logger.debug("Exception thrown while verifying downloaded image file.", e)
						}
					}
					++retries
				}
			}
		}

		// WMS endpoint doesn't provide actual size of the file and checksum.
		// At this point the handler ingested all the files and computed the checksum locally.
		// It is time to compare the locally computed values with previously ingested files
		// listed in cache
		def inCache = true
		for (FileProduct fp in product.files) {
			//logger.debug("Checking file ${fp.name} in cache.")
			CacheFileInfo cfi = new CacheFileInfo()
			cfi.product = productName
			cfi.name = fp.name
			cfi.modified = fp.lastModifiedTime
			cfi.size = fp.size
			cfi.checksumAlgorithm = fp.digestAlgorithm.toString()
			cfi.checksumValue = fp.digestValue
			//cfi.granuleId = this.granuleId
			
			if (productType.batch || !productType.isInCacheIgnoreTimestamp(cfi)) {
				if(productType.batch) {logger.debug("Batch mode, ignoring cache comparison.")}
				inCache = false
				break
			}
		}
		if (inCache) {
			logger.debug ("Product ${product.name} was previously ingested.. cleanup duplicate ingested product")
			for (FileProduct fp in product.files) {
				fp.delete()
			}
			s.deleteDir()
			return // breakout of current closure
		}

		product.ingestStop = new Date()

		File sh = new File(shadow)
		if (sh.exists() && sh.listFiles().size() == 0) {
			// no file needs to be archived
			sh.deleteDir()
		} else {
			// completed file downloads.  Move the product to a visible location

			// if a previous copy exists, then delete it first and use the latest
			File newLocation = new File(location)
			if (newLocation.exists()) {
				newLocation.listFiles().each {
					it.delete()
				}
				newLocation.deleteDir()
			}

			if (!s.renameTo(newLocation)) {
				logger.error("Unable to move downloaded product to ${location}")
				_cleanup(shadow)
				throw new DataHandlerException("Unable to move downloaded product to ${location}")
			}

			logger.info("Retrieved new product ${productType.name}:${productName}")

		 // Generate geo file.  This is needed for MRF generation
		String geoName = "${location}${File.separator}${productName}"
		geoName += productType.fileExtension.equals(".png") ? ".pgw" : ".jgw"
        LocalFileProduct geoFile = generateGeo(geoName)
        if(geoFile == null) {
            logger.error("Could not generate geo world file: "+geoName)
        }
        else {
            product.addFileProduct(geoFile)
        }
        

			ServiceProfile sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)

			if (sp != null) {

				// write SIP to pending directory
				String pendingSIP = "${productType.metadataPending}${File.separator}${productName}_${timetag.time}.xml"
				logger.debug("Store SIP in ${pendingSIP}")

				new File(pendingSIP).write(sp.toString())

				// update cache
				for (FileProduct fp in product.files) {
					//logger.debug("Checking file ${fp.name} in cache.")
					CacheFileInfo cfi = new CacheFileInfo()
					cfi.product = productName
					cfi.name = fp.name
					cfi.modified = fp.lastModifiedTime
					cfi.size = fp.size
					cfi.checksumAlgorithm = fp.digestAlgorithm.toString()
					cfi.checksumValue = fp.digestValue
					//cfi.granuleId = this.granuleId
					
					if(productType.batch) {logger.debug("Batch mode, skip cache update.")}
					// Don't update lastRetrieved date in cache file if we're processing data from the "batch" portion of extended daemon mode.
					// (.pgw/.jgw world files are excluded because they're always generated by the handler & thus shouldn't trigger an update of lastRetrieved date.)
					boolean updateLastRetrieved = (!fp.name.endsWith(".pgw") && !fp.name.endsWith(".jgw")) && (fp.lastModifiedTime >= new Date().clearTime().getTime())
					logger.debug("About to update cache from WMSFileHandler. fp.name: ${fp.name} fp.lastModifiedTime: ${fp.lastModifiedTime} updateLastRetrieved: ${updateLastRetrieved}")
					
					if (!productType.batch && productType.updateCache(cfi, this.granuleId, updateLastRetrieved)) {
						logger.debug("Product:file ${productType.name}:${cfi.name} updated in cache for granuleID ${this.granuleId}.")
					}
				}

			} else {
				String errorDir = "${productType.validationError}${File.separator}${productName}"
				logger.error("Unable to extract metdata for ${productName}.  Moving product to ${errorDir}")

				new File(location).renameTo(new File(errorDir))

				this.sigEvent.create(EventType.Error, productType.name, 'WMSDataHandler',
						'WMSDataHandler',
						InetAddress.localHost.hostAddress,
						"Unable to extract metadata for ${productName}.  Proudct moved to ${errorDir}")

			}
		}

	}

	@Override
	void postprocess() {

	}

	@Override
	void onError(Throwable throwable) {

	}

	protected static void _cleanup(String downloadLocation) {
		File dir = new File(downloadLocation)
		if (dir.exists()) {
			dir.listFiles().each {
				it.delete()
			}
			dir.deleteDir()
		}
	}
    
    private LocalFileProduct generateGeo(String path) {
        LocalFileProduct result = null
        File geoFile
        if(this.productType.left_x != null && this.productType.right_x && this.productType.upper_y && this.productType.lower_y) { 
            geoFile = new File(path)
            
            Float geoLine1 = (this.productType.right_x - this.productType.left_x) / this.productType.width
            Float geoLine4 = (this.productType.upper_y - this.productType.lower_y) / (this.productType.height * -1)
            Float geoLine5 = this.productType.left_x + (geoLine1 / 2)
            Float geoLine6 = this.productType.upper_y + (geoLine4 / 2)
			
            DecimalFormat df = new DecimalFormat("0.000000000000")
			
			String geoString = df.format(geoLine1) + "\n0.000000000000\n0.000000000000\n" + df.format(geoLine4) + "\n" +df.format(geoLine5) +"\n" +df.format(geoLine6)
            geoFile << geoString
        }
        else {
            logger.warn("Bounding box values not properly set for product type:` "+this.productType.name)
        }
        if(geoFile && geoFile.exists())
            result = new LocalFileProduct(path)
        return result
    }
    
}
