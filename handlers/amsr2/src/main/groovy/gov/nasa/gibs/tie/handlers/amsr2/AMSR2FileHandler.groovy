package gov.nasa.gibs.tie.handlers.amsr2

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.api.file.LocalFileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.util.ChecksumUtility
import gov.nasa.horizon.common.api.util.ChecksumUtility.DigestAlgorithm;
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.Product
import gov.nasa.horizon.sigevent.api.EventType
import gov.nasa.horizon.sigevent.api.SigEvent
import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.gibs.tie.handlers.common.TarGzipUtil
import gov.nasa.horizon.common.api.serviceprofile.*

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.text.SimpleDateFormat
import java.text.DecimalFormat

/**
 * gov.nasa.gibs.tie.handlers.amsr2.AMSR2 file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class AMSR2FileHandler implements FileHandler, FileProductHandler {

   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.amsr2.AMSR2FileHandler.class)

   private gov.nasa.gibs.tie.handlers.amsr2.AMSR2ProductType productType

   private SigEvent sigEvent

   public AMSR2FileHandler(gov.nasa.gibs.tie.handlers.amsr2.AMSR2ProductType productType) {
      this.productType = productType
      this.sigEvent = new SigEvent(productType.sigEventURL)
   }

   @Override
   void preprocess() {

   }

   @Override
   void onProducts(Set<FileProduct> fileProducts) {
      try {
         this.process(fileProducts as List<FileProduct>)
      } catch (DataHandlerException e) {
  		logger.info("EXCEPTION: ${e}")
         this.onError(e)
      }
   }

   @Override
   void process(List<FileProduct> fps) throws DataHandlerException {
      boolean inCache = true
     
	   String productName = fps.find { FileProduct fp ->
         fp.name.endsWith(".tgz")
      }.name
      productName = productName?.substring(0, productName.lastIndexOf(".tgz"))

      if (!productName) {
         def fn = fps.collect { FileProduct fp ->
            fp.name
         }
         logger.warn("Incomplete product encountered.  Only contains ${fn}")
         return
      }

      // filter only the files that is within the start/end range
      Date sd = this.productType.startDate.clearTime()
      Date ed = this.productType.endDate
      if (!ed) {
         ed = new Date()
      }
      ed = ed.clearTime()

      SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd')
      logger.debug("Filtering files between ${sd} and ${ed}")
      List<FileProduct> fileProducts = []
      for (FileProduct filep : fps) {
         def d = (filep.name =~ /(\d{4})(\d{2})(\d{2})/)
         Date dat = sdf.parse("${d[0][1]}-${d[0][2]}-${d[0][3]}")
         logger.debug("File ${filep.name} create: ${dat}")
         if (dat >= sd && dat <= new Date ((ed+1).time -1)) {
            fileProducts.add(filep)
         }
      }

      // checking if the any of the file already in cache
      def names = []
      for (FileProduct fp : fileProducts) {
         CacheFileInfo cfi = new CacheFileInfo()
         cfi.product = productName
         cfi.name = fp.name
         cfi.modified = fp.lastModifiedTime
         cfi.size = fp.size

         if (!productType.isInCache(cfi)) {
            inCache = false
            break
         }
         names << fp.name
         logger.debug("Checking file ${productName}:${fp.name} cache status: ${inCache}")
      }

      // don't download if we already have the file
      if (inCache && fileProducts.size() > 0) {
         if (logger.debugEnabled) {
            logger.info("Files [${names}] already in cache... skipping.")
         }
         return
      }

      if (fileProducts.size() == 0) {
         return
      }
	  
		Date timetag = new Date()
		String location = "${productType.dataStorage}${File.separator}${productName}_${timetag.time}${File.separator}"
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
		File newLocation = new File(location)
	  
		Product product = new Product(productType, productName)
		product.shadowLocation = shadow
		product.stageLocation = location
		product.ingestStart = new Date()
		int max_retries = 5

		List<FileProduct> downloadedTarFileProducts = []
		def generateWorldFile = this.productType.right_x.equals("") ? false : true //Don't generate a world file if the config didn't specify ALL bounding box data. This indicates that the tar already included a world file.
		def generateJgw = false // Default behavior is to generate a pgw, only generate jgw if JPG is found.

		fileProducts.each { FileProduct fileProduct ->
			int retries = 1
			while (retries <= max_retries) {
				logger.debug("Start to download file ${fileProduct.name}.. attempt ${retries}")
				String shadowLocation = "${shadow}${File.separator}${fileProduct.name}"
				FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
				fileProduct.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
				String checksum = ChecksumUtility.getDigest(fileProduct.digestAlgorithm, fileProduct.inputStream, fos)
				if (!checksum) {
					if (retries == max_retries) {
						logger.error("Unable to download file after ${retries} attempts: " + fileProduct.friendlyURI)
						throw new DataHandlerException("Unable to download file: " + fileProduct.friendlyURI)
					} else {
						logger.debug("Download unsuccessful.  Handler will retry...")
						// sleep 1 sec and retry
						sleep(1000)
					}
				} else {

					// Dont add the tar to the file list. Gets deleted later anyways so why bother
					//product.addFileProduct(fileProduct)
					downloadedTarFileProducts.add(fileProduct)

					// download success
					logger.debug("Retrieved file: ${fileProduct.name}")

					// untar the file
					try {
						File tarball = new File("${shadow}${File.separator}${fileProduct.name}")
						File unzipped = TarGzipUtil.unGzip(tarball, new File(shadow))
						TarGzipUtil.unTar(unzipped, new File(shadow))
						unzipped.delete()
						tarball.delete()
						
						// if a previous copy exists, then delete it first and use the latest
						if (newLocation.exists()) {
							logger.trace("newLocation exists! ${newLocation}")
						   newLocation.listFiles().each {
							  it.delete()
						   }
						}
						
						if (!s.renameTo(newLocation)) {
							logger.error("Unable to move downloaded product to ${location}")
							_cleanup(shadow)
							throw new DataHandlerException("Unable to move downloaded product to ${location}")
						 }
						
						new File("${shadow}${File.separator}${fileProduct.name}").delete()
						
						def files = new File(location).listFiles()
						if (files.size() != 0) {

							if (logger.debugEnabled) { logger.debug "Successfully unzipped and untared file ${fileProduct.name}.  File listing..." }
								
							files.each { File f ->
								if( f.name.endsWith('.jpg') ) {
									generateJgw = true
								} else if ( f.name.endsWith('gw')) {
									generateWorldFile &= false
								}
								
								FileProduct newProduct = new LocalFileProduct(location + f.name)
								newProduct.setDigestValue(ChecksumUtility.getDigest(ChecksumUtility.DigestAlgorithm.MD5, f))
								product.addFileProduct(newProduct)

								if (logger.debugEnabled) { logger.debug("${f.name} ${f.size()}") }
							}
						}
					} catch (Exception e) { //TODO sigevent here?
						logger.debug("FAILED TO UNTAR FILE ${fileProduct.name}", e)
						new File(shadow).deleteDir()
					}

					break
				}
				++retries
			}
		}

      product.ingestStop = new Date()

      File sh = new File(shadow)
      if (sh.exists() && sh.listFiles().size() == 0) {
         // no file needs to be archived
         sh.deleteDir()
      } 
	  

	  if (newLocation.exists() && newLocation.listFiles().size() > 0) {
         // completed file downloads.  Move the product to a visible location


         logger.info("Retrieved new product ${productType.name}:${productName}")
		 
		 if(generateWorldFile) {
			 // Generate geo file.  This is needed for MRF generation
			 String geoExtension = generateJgw ? ".jgw" : ".pgw"
			 String geoName = "${location}${File.separator}${productName}${geoExtension}"
			 LocalFileProduct geoFile = generateGeo(geoName)
			 if(geoFile == null) {
				 logger.error("Could not generate geo world file: "+geoName)
			 }
			 else {
				 product.addFileProduct(geoFile)
			 }
		 }

	     ServiceProfile sp;
		 try{
			 sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)
	     } catch(ServiceProfileException spe) {
		 	logger.error("Unable to create SIP. Encountered ServiceProfileException during object creation.", spe)
			this.sigEvent.create(EventType.Error, productType.name, 'gov.nasa.gibs.tie.handlers.amsr2.AMSR2DataHandler',
				 'gov.nasa.gibs.tie.handlers.amsr2.AMSR2DataHandler',
				 InetAddress.localHost.hostAddress,
				 "Encountered ServiceProfileException, unable to create SIP for ${productName}.")
	     }
		 
         if (sp != null) {
            // write SIP to pending directory
            String pendingSIP = "${productType.metadataPending}${File.separator}${productName}_${timetag.time}.xml"
            logger.debug("Store SIP in ${pendingSIP}")

            new File(pendingSIP).write(sp.toString())

            // update cache
            for (FileProduct fp in downloadedTarFileProducts) {
               logger.debug("Checking file ${fp.name} in cache.")
               CacheFileInfo cfi = new CacheFileInfo()
               cfi.product = productName
               cfi.name = fp.name
               cfi.modified = fp.lastModifiedTime
               cfi.size = fp.size
               cfi.checksumAlgorithm = fp.digestAlgorithm.toString()
               cfi.checksumValue = fp.digestValue
               if (productType.updateCache(cfi)) {
                  logger.debug("${productType.name}:${cfi.name} has been added to cache.")
               }
            }

         } else {
            String errorDir = "${productType.validationError}${File.separator}${productName}"
            logger.error("Unable to extract metdata for ${productName}.  Moving product to ${errorDir}")

            new File(location).renameTo(new File(errorDir))

            this.sigEvent.create(EventType.Error, productType.name, 'gov.nasa.gibs.tie.handlers.amsr2.AMSR2DataHandler',
                  'gov.nasa.gibs.tie.handlers.amsr2.AMSR2DataHandler',
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
	   if(geoFile && geoFile.exists()) {
		   result = new LocalFileProduct(path)
		   result.digestValue = ChecksumUtility.getDigest(ChecksumUtility.DigestAlgorithm.MD5, geoFile)
	   }
	   return result
   }
}
