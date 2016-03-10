/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.api.util.ChecksumUtility
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.Product
import gov.nasa.horizon.sigevent.api.SigEvent
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * PDR file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class PDRFileHandler implements FileHandler, FileProductHandler {

   private static Log logger = LogFactory.getLog(PDRFileHandler.class)

   private PDRProductType productType

   private SigEvent sigEvent

   private def seenList = []

   public PDRFileHandler(PDRProductType productType) {
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
         this.onError(e)
      }
   }

   @Override
   void process(List<FileProduct> fps) throws DataHandlerException {

      // filter only the files that is within the start/end range
      // The clearTime method clears the time portion of the date
      // object, since we are filtering only by date range.
      Date sd = this.productType.startDate.clearTime()
      Date ed = this.productType.endDate ? this.productType.endDate : new Date()
      ed.clearTime()

      // we are expecting only one file product (i.e. PDR file), but just
      // to be safe to iterate through all of them in case we have
      // multiple PDR files.
      for (FileProduct filep : fps) {
         if (!filep.name.endsWith('.PDR') && !filep.name.endsWith('.pdr')) {
            // skip if it is not a PDR file
            logger.debug("${filep.name} is not a PDR file.  Skipping...")
            continue
         }

         if (filep.lastModifiedTime < sd.time || filep.lastModifiedTime > (ed + 1).time) {
            // skip if it is not within the specified time range
            logger.debug("${filep.name} was last modifed ${new Date(filep.lastModifiedTime)} is outside of the required range.  Skipping...")
            continue
         }

         String pdrProductName
         if (filep.name.endsWith('.PDR'))
            pdrProductName = filep.name.substring(0, filep.name.lastIndexOf('.PDR'))

         if (filep.name.endsWith('.pdr'))
            pdrProductName = filep.name.substring(0, filep.name.lastIndexOf('.pdr'))

         if (!pdrProductName) {
            logger.debug("${filep.name} is not a PDR file.  Skipping...")
         }

         CacheFileInfo cfi = new CacheFileInfo()
         cfi.product = pdrProductName
         cfi.name = filep.name
         cfi.modified = filep.lastModifiedTime
         cfi.size = filep.size

         // store this.  It will be used to cleanup the cache.  This keeps track
         // of all the ones the crawler has seen.  In the cleanup, we will
         // compare this list with that is in the cache file to identify the
         // ones that are no in the seeList, that is, the PDR has been removed
         // from the provider.  In this case, the cache object should be removed
         // from the cache file.
         this.seenList << cfi

         if (productType.isInCache(cfi)) {
            // skip if the product is already in cache
            logger.debug("${filep.name} is already in cache.  Skipping...")
            continue
         }

         // ready to fetch the PDR file
         Product product = new Product(this.productType, pdrProductName)
         product.ingestStart = new Date()
         String destination = "${productType.dataStorage}${File.separator}${pdrProductName}${File.separator}"
         String shadow = "${productType.dataStorage}${File.separator}.shadow${File.separator}${pdrProductName}${File.separator}"

         File shadowDir = new File(shadow)
         if (shadowDir.exists()) {
            // nothing was ingested.  Remove shadow directory
            shadowDir.deleteDir()
         }
         shadowDir.mkdirs()

         int max_retries = 5
         int current_retry = 1
         while (current_retry <= max_retries) {
            logger.debug("Start to download file ${filep.name}.. attempt ${current_retry}")
            String shadowLocation = "${shadow}${File.separator}${filep.name}"
            FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
            filep.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
            String checksum = ChecksumUtility.getDigest(filep.digestAlgorithm, filep.inputStream, fos)
            if (!checksum) {
               if (current_retry == max_retries) {
                  logger.error("Unable to download file after ${current_retry} attempts: " + filep.friendlyURI)
                  throw new DataHandlerException("Unable to download file: " + filep.friendlyURI)
               } else {
                  logger.debug("Download unsuccessful.  Handler will retry...")
                  // sleep 1 sec and retry
                  sleep(1000)
               }
            } else {
               filep.digestValue = checksum

               product.addFileProduct(filep)
               // download success
               logger.debug("Retrieved file: ${filep.name}")
               break
            }
            ++current_retry
         }
         product.ingestStop = new Date()
         filep.close()

         if (shadowDir?.listFiles()?.size() == 0) {
            // nothing was ingested.  Remove shadow directory
            shadowDir.deleteDir()
         } else {
            File finalDestination = new File(destination)
            if (finalDestination.exists()) {
               finalDestination.listFiles().each {
                  it.delete()
               }
               finalDestination.deleteDir()
            }

            if (!shadowDir.renameTo(finalDestination)) {
               logger.error("Unable to move downloaded product to ${destination}")
               _cleanup(shadow)
               throw new DataHandlerException("Unable to move downloaded product to ${destination}")
            }

            logger.info("Retrieved new product ${productType.name}:${pdrProductName}")

            cfi.checksumAlgorithm = filep.digestAlgorithm.toString()
            cfi.checksumValue = filep.digestValue

            if (productType.updateCache(cfi)) {
               logger.debug("${productType.name}:${cfi.name} has been added to cache.")
            }

            // time to process the PDR
            PDRProcessor pp = new PDRProcessor(productType, "${destination}${File.separator}${filep.name}")
            pp.run()

            // Done with a PDR file.  Remove it from the data store.
            def f = new File("${productType.dataStorage}${File.separator}${pdrProductName}${File.separator}")
            if (f.exists()) {
               f.deleteDir()
            }
         }
      }

   }

   @Override
   void postprocess() {
      logger.debug("inside postprocess.  Update cache")
      String cacheFile = "${this.productType.cache}${File.separator}${this.productType.name}.cache.xml"
      def cache = CacheFileInfo.load(cacheFile)
      def newCache = []
      cache.each { ca ->
         if (this.seenList.find { se ->
            se.product == ca.product &&
                  se.name == ca.name &&
                  se.size == ca.size &&
                  se.modified == ca.modified
         }) {
            newCache << ca
         }
      }
      CacheFileInfo.save(newCache, cacheFile)
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
