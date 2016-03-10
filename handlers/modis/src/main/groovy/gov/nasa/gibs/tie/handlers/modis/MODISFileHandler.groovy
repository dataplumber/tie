package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfileException
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

import java.text.SimpleDateFormat

/**
 * MODIS file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODISFileHandler implements FileHandler {

   private static Log logger = LogFactory.getLog(MODISFileHandler.class)

   private MODISProductType productType

   private SigEvent sigEvent

   public MODISFileHandler(MODISProductType productType) {
      this.productType = productType
      this.sigEvent = new SigEvent(productType.sigEventURL)
   }

   @Override
   void preprocess() {

   }

   @Override
   void process(List<FileProduct> fps) throws DataHandlerException {

      boolean inCache = true

      logger.trace ("Convert list to map")

      if (logger.traceEnabled) {
         fps.each {
            logger.trace ("${it}")
         }
      }
      
      def fileMap = fps.groupBy {
         it.name.substring(0, it.name.lastIndexOf('.'))
      }

      if (logger.traceEnabled) {
         fileMap.each { productName, files ->
            logger.debug ("${productName} -> ${files}\n")
         }
      }

      def rejects

      fileMap.each {productName, files ->
         if (files.size != 2) {
            logger.warn ("Product ${productName} is incomplete.  It will not be ingested.")
            rejects += [productName: files]
         }
      }

      fileMap -= rejects

	  
      // filter only the files that is within the start/end range
	  Date sd
	  if (this.productType.batch) {
			sd = this.productType.startDate
	  } else {
			//Compute start date. If handler was last run today, we should fetch the previous day's data.
			//If handler was last run yesterday, we should fetch the previous 2 days of data.
	  		def startDateOffset = -1
	  		def lastRetrievedDate = this.productType.lastRetrievedDate //We expect null on the handler's first iteration.
			if(lastRetrievedDate != null && lastRetrievedDate.day != new Date().day) {
				startDateOffset -2
			}
			
			sd = (new Date() + startDateOffset).clearTime()
			logger.trace("FileHandler startDate: ${sd} ${sd.time}")
	  }
	  
      Date ed = this.productType.endDate
      if (!ed) {
         ed = new Date()
      }
      ed = ed.clearTime()
      // the end time is 1 millisecond before midnight
      ed = new Date ((ed+1).time -1)

      fileMap.each { productName, files ->
         logger.debug("Filtering files between ${sd} and ${ed}")
         List<FileProduct> fileProducts = []

         boolean skip = false
         files.each {
            if (it.lastModifiedTime < sd.time || it.lastModifiedTime > ed.time) {
               skip = true
            }
         }

         if (skip) {
            // nothing to process.. exit the current closure
            return
         }

         fileProducts += files

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
         }

         // don't download if we already have the file
         if (inCache) {
            if (logger.debugEnabled) {
               logger.info("Files [${names}] already in cache... skipping.")
            }
            return
         }

         Date timetag = new Date()
         String location = "${productType.dataStorage}${File.separator}${productName}_${timetag.time}${File.separator}"
         String shadow = "${productType.dataStorage}${File.separator}.shadow${File.separator}${productName}_${timetag.time}${File.separator}"

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

         logger.trace ("Ready to download.")

         fileProducts.each { HttpFileProduct fileProduct ->
            logger.trace ("Attempt to download file: ${fileProduct.name}")
            int retries = 1
            while (retries <= max_retries) {
               logger.debug("Start to download file ${fileProduct.name}.. attempt ${retries}")
               String shadowLocation = "${shadow}${File.separator}${fileProduct.name}"
               FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
               fileProduct.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
               String checksum = ChecksumUtility.getDigest(fileProduct.digestAlgorithm, fileProduct.inputStream, fos)
               logger.debug ("Expected file size ${fileProduct.size}; Actual file size ${new File(shadowLocation).size()}")
               fileProduct.size = new File(shadowLocation).size()
               if (!checksum) {
                  if (retries == max_retries) {
                     logger.error("Unable to download file after ${retries} attemps: " + fileProduct.friendlyURI)
                     throw new DataHandlerException("Unable to download file: " + fileProduct.friendlyURI)
                  } else {
                     logger.debug("Download unsuccessful.  Handler will retry...")
                     // sleep 1 sec and retry
                     sleep(1000)
                  }
               } else {
                  fileProduct.digestValue = checksum

                  product.addFileProduct(fileProduct)

                  // download success
                  logger.info("Retrieved file: ${productType.name}/${productName}/${fileProduct.name}")
                  break
               }
               ++retries
            }
         }

         // MODIS website doesn't provide actual size of the file and checksum.
         // At this point the handler ingested all the files and computed the checksum locally.
         // It is time to compare the locally computed values with previously ingested files
         // listed in cache
         inCache = true
         for (FileProduct fp in product.files) {
            logger.trace("Checking file ${fp.name} in cache.")
            CacheFileInfo cfi = new CacheFileInfo()
            cfi.product = productName
            cfi.name = fp.name
            cfi.modified = fp.lastModifiedTime
            cfi.size = fp.size
            cfi.checksumAlgorithm = fp.digestAlgorithm.toString()
            cfi.checksumValue = fp.digestValue
            if (!productType.isInCache(cfi)) {
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

         logger.debug("Retrieved new product ${productType.name}:${productName}")

         ServiceProfile sp

         try {
            sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)

            if (sp != null) {

               // write SIP to pending directory
               String pendingSIP = "${productType.metadataPending}${File.separator}${productName}_${timetag.time}.xml"
               logger.trace("Store SIP in ${pendingSIP}")

               new File(pendingSIP).write(sp.toString())

               // update cache
               for (FileProduct fp in product.files) {
                  logger.trace("Updating file ${fp.name} in cache.")
                  CacheFileInfo cfi = new CacheFileInfo()
                  cfi.product = productName
                  cfi.name = fp.name
                  cfi.modified = fp.lastModifiedTime
                  cfi.size = fp.size
                  cfi.checksumAlgorithm = fp.digestAlgorithm.toString()
                  cfi.checksumValue = fp.digestValue
                  if (productType.updateCache(cfi)) {
                     logger.debug("${productType.name}:${cfi.name} has been added to cache.")
                     logger.info("Retrieved new product ${productType.name}:${productName}")
                  }
               }
            } else {
               String errorDir = "${productType.validationError}${File.separator}${productName}"
               logger.error("Unable to extract metdata for ${productName}.  Moving product to ${errorDir}")

               new File(location).renameTo(new File(errorDir))

               this.sigEvent.create(EventType.Error, productType.name, 'MODISDataHandler',
                     'MODISDataHandler',
                     InetAddress.localHost.hostAddress,
                     "Unable to extract metadata for ${productName}.  Proudct moved to ${errorDir}")
            }
         } catch (ServiceProfileException e) {
            throw new DataHandlerException(e)
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
}
