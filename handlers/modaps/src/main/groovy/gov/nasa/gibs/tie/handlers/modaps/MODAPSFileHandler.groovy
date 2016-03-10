package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.util.ChecksumUtility
import gov.nasa.horizon.common.httpfetch.api.HttpFileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfileException
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.Product
import gov.nasa.horizon.sigevent.api.EventType
import gov.nasa.horizon.sigevent.api.SigEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * MODAPS file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODAPSFileHandler implements FileHandler {

   private static Log logger = LogFactory.getLog(MODAPSFileHandler.class)

   private MODAPSProductType productType

   private SigEvent sigEvent

   public MODAPSFileHandler(MODAPSProductType productType) {
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
      
      // BEGIN: Create product map

      // group file name list by the file name prefix without the extension.
      // this will result in duplicate product groups becasue the .txt file has
      // the same prefix without the image resolution subfix.
      def groupedFileMap = fps.groupBy {
         it.name.substring(0, it.name.lastIndexOf('.'))
      }
      def keys = groupedFileMap.keySet()
      def mc = [
         compare: {a, b -> a.equals(b) ? 0: a.length() < b.length() ? -1 : 1 }
      ] as Comparator
      def sortedKeys = keys.sort(mc)
      def minLength = sortedKeys.first().length()

      // the assumption here is the group name created by the .txt file has shorter
      // string length compare to the group named with the imagery resolution.  the
      // sorted list should only have group names of two lengths, one with resolution subfix
      // and one without.  This loop drops all the group names (keys) that has 
      // shorter length.
      def reducedKeys = sortedKeys.dropWhile {
         it.length() == minLength
      }

      logger.trace ("Original keys: ${sortedKeys}.  Reduced keys: ${reducedKeys}")

      // create a new proudct-to-files map with the reduced list of keys.
      def fileMap = [:]
      reducedKeys.each {
         fileMap[it] = groupedFileMap[it]
      }

      if (logger.traceEnabled) {
         long count = 0
         fileMap.each { productName, files ->
            logger.debug ("${productName} -> ${files}\n")
            count += files.size
            logger.debug("${productType.name}:${productName} file count: ${files.size}")
         }
         logger.debug ("${productType.name} total file count: ${count}")
      }
      // END

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
	  
	  if(this.productType.batch) {
		  ed = ed.clearTime()
		  ed = new Date ((ed+1).time -1)
	  }
	  
      def names = []
      fileMap.each { productName, files ->
		 logger.debug("Filtering files between ${sd} and ${ed}")
         logger.trace ("Processing product: ${productName}")

         boolean skip = false
         files.each { 
            // skip file if it is outside of the specified time
            if (it.lastModifiedTime < sd.time || it.lastModifiedTime > ed.time) {
               skip = true
            }
         }

         // the product is outside of the specified time... skip
         if (skip) return

         for (FileProduct fp : files) {
            MODAPSCacheFileInfo cfi = new MODAPSCacheFileInfo()
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

         fps.each { HttpFileProduct fileProduct ->
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
                  if (fileProduct.digestAlgorithm && fileProduct.digestValue) {
                     if (!fileProduct.digestValue.equals(checksum)) {
                     //if (fileProduct.digestAlgorithm == alg
                        //&& !fileProduct.digestValue.equals(checksum)) {
                        if (retries == max_retries) {
                           logger.error("Checksum validation failure for file after ${retries} attemps: ${fileProduct.name}")
                           _cleanup(shadow)
                           throw new DataHandlerException("Checksum validation failure for file: ${fileProduct.name}")
                        } else {
                           logger.debug("Checksum validation failure.  Handler will retry...")
                           // sleep 1 sec and retry
                           sleep(1000)
                        }
                     }
                  }
                  fileProduct.digestValue = checksum
                  product.addFileProduct(fileProduct)

                  // download success
                  logger.debug("Retrieved file: ${fileProduct.name}")
                  break
               }
               ++retries
            }
         }

		 
		 // MODAPS website doesn't provide actual size of the file and checksum.
		 // At this point the handler ingested all the files and computed the checksum locally.
		 // It is time to compare the locally computed values with previously ingested files
		 // listed in cache
		 inCache = true
		 for (FileProduct fp in product.files) {
			logger.trace("Checking file ${fp.name} in cache.")
			MODAPSCacheFileInfo cfi = new MODAPSCacheFileInfo()
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

         // if a previous copy exists, then delete it first and use the latest
         File newLocation = new File(location)
         if (newLocation.exists()) {
            newLocation.listFiles().each {
               it.delete()
            }
            newLocation.deleteDir()
         }

         // completed file downloads.  Move the product to a visible location
         if (!s.renameTo(newLocation)) {
            logger.error("Unable to move downloaded product to ${location}")
            _cleanup(shadow)
            throw new DataHandlerException("Unable to move downloaded product to ${location}")
         }

         logger.info("Retrieved new product ${productType.name}:${productName}")

         ServiceProfile sp

         try {
            sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)
         } catch (ServiceProfileException e) {
             throw new DataHandlerException (e, e.message)
         }

         if (sp != null) {

            // write SIP to pending directory
            String pendingSIP = "${productType.metadataPending}${File.separator}${productName}_${timetag.time}.xml"
            logger.debug("Store SIP in ${pendingSIP}")

            new File(pendingSIP).write(sp.toString())

            // update cache
            for (FileProduct fp in product.files) {
               logger.debug("Checking file ${fp.name} in cache.")
               MODAPSCacheFileInfo cfi = new MODAPSCacheFileInfo()
               cfi.product = productName
               cfi.name = fp.name
               cfi.modified = fp.lastModifiedTime
			   if(fp.size == 0) {
				   File tmpFile = new File("${location}${fp.name}")
				   cfi.size = tmpFile.length()
			   } else {
			   		cfi.size = fp.size
			   } 
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

            this.sigEvent.create(EventType.Error, productType.name, 'MODAPSDataHandler',
               'MODAPSDataHandler',
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
}
