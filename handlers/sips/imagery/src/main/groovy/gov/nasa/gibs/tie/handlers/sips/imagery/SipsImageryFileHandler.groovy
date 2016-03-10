/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfileException 
import gov.nasa.horizon.common.api.util.ChecksumUtility
import gov.nasa.horizon.common.api.util.URIPath
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.FileHandler
import gov.nasa.horizon.handlers.framework.Product
import gov.nasa.horizon.sigevent.api.EventType
import gov.nasa.horizon.sigevent.api.SigEvent
import org.apache.commons.httpclient.util.URIUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * SipsImagery file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryFileHandler implements FileHandler, FileProductHandler {

   private static Log logger = LogFactory.getLog(SipsImageryFileHandler.class)

   private SipsImageryProductType productType

   private SigEvent sigEvent

   public SipsImageryFileHandler(SipsImageryProductType productType) {
      logger.debug ("SipsImageryFileHandler constructor")
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

      boolean inCache = true

      logger.trace ("Convert list to map")

      if (logger.traceEnabled) {
         fps.each {
            logger.trace ("${it}")
         }
      }
      
      def fileMap = fps.groupBy {
         String pname
         // check to see if the product has a specified product naming scheme
         if (!productType.productNaming)
            pname = it.name.substring(0, it.name.lastIndexOf('.'))
         else {
            def match = it.name =~ /(${productType.productNaming})/
            pname = match[0][1]
         }
         logger.debug ("Product Name: ${pname}")
         return pname
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

      // filter only the files that is within the start/end range
      Date sd = this.productType.startDate.clearTime()
      Date ed = this.productType.endDate
      if (!ed) {
         ed = new Date()
      }
      ed = ed.clearTime()


      def names = []
      fileMap.each { productName, files ->
         logger.trace ("Processing product: ${productName}")

         boolean skip = false
         files.each { 
            // skip file if it is outside of the specified time
            if (it.lastModifiedTime < sd.time || it.lastModifiedTime > (ed +1).time-1) {
               skip = true
            }
         }

         // the product is outside of the specified time... skip
         if (skip) return

         for (FileProduct fp : files) {
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

         if (inCache) {
            logger.debug ("Files [${names}] already in cache... skipping.")
            return
         }

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

         logger.trace ("Ready to download.")

         files.each { FileProduct fileProduct ->
            int retries = 1
            while (retries <= max_retries) {
               logger.debug("Start to download file ${fileProduct.name}.. attempt ${retries}")
               String shadowLocation = "${shadow}${File.separator}${fileProduct.name}"
               FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
               fileProduct.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
               String checksum = ChecksumUtility.getDigest(fileProduct.digestAlgorithm, fileProduct.inputStream, fos)
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
                  logger.debug("Retrieved file: ${fileProduct.name}")

                  // remove file from PDR repo
                  if (fileProduct.isWriteable()) {
                     fileProduct.delete()
                     URIPath up = URIPath.createURIPath(fileProduct.friendlyURI)
                     File p = new File(up.pathOnly)
                     if (p.list().size() == 0) {
                        p.deleteDir()
                     }

                  }
                  break
               }
               ++retries
            }
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

         logger.info("Retrieved new product ${productType.name}:${productName}")

         ServiceProfile sp
         
         try {
            sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)

            if (sp != null) {

               // write SIP to pending directory
               String pendingSIP = "${productType.metadataPending}${File.separator}${productName}.xml"
               logger.debug("Store SIP in ${pendingSIP}")

               new File(pendingSIP).write(sp.toString())

               // update cache
               for (FileProduct fp in product.files) {
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
               throw new ServiceProfileException("Failed to create ServiceProfile.")
            }
         } catch (ServiceProfileException e) {
		 	 String errorDir = "${productType.validationError}${File.separator}${productName}"
               logger.error("Unable to extract metdata for ${productName}.  Moving product to ${errorDir}")

               new File(location).renameTo(new File(errorDir))

               this.sigEvent.create(EventType.Error, productType.name, 'SipsImageryDataHandler',
                  'SipsImageryDataHandler',
                  InetAddress.localHost.hostAddress,
                  "Unable to extract metadata for ${productName}.  Proudct moved to ${errorDir}")
		 
            throw new DataHandlerException (e)
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
