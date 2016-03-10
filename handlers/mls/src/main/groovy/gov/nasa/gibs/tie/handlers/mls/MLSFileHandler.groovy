package gov.nasa.gibs.tie.handlers.mls

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.api.file.LocalFileProduct
import gov.nasa.horizon.common.api.file.VFSFileProduct
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile
import gov.nasa.horizon.common.api.util.ChecksumUtility
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
 * MLS file handler class
 *
 * @author T. Huang
 * @version $Id: $
 */
class MLSFileHandler implements FileHandler, FileProductHandler {

   private static Log logger = LogFactory.getLog(MLSFileHandler.class)

   private MLSProductType productType

   private SigEvent sigEvent

   public MLSFileHandler(MLSProductType productType) {
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

      String productName = fps.find { FileProduct fp ->
         fp.name.endsWith(".png")
      }.name
      productName = productName?.substring(0, productName.lastIndexOf(".png"))

      if (!productName) {
         def fn = fps.collect {
            it.name
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

      SimpleDateFormat sdf = new SimpleDateFormat('yyyy-DDD')
      logger.debug("Filtering files between ${sd} and ${ed}")
      List<FileProduct> fileProducts = []
      for (FileProduct filep : fps) {
         def d = (filep.name =~ /(\d{4})d(\d{3})/)
         Date dat = sdf.parse("${d[0][1]}-${d[0][2]}")
         logger.debug("File ${filep.name} create: ${dat}")
         if (dat >= sd && dat <= new Date((ed+1).time -1)) {
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

      Product product = new Product(productType, productName)
      product.shadowLocation = shadow
      product.stageLocation = location
      product.ingestStart = new Date()
      int max_retries = 5

      fileProducts.each { VFSFileProduct fileProduct ->
         int retries = 1
         while (retries <= max_retries) {
            logger.debug("Start to download file ${fileProduct.name}.. attempt ${retries}")
            String shadowLocation = "${shadow}${File.separator}${fileProduct.name}"
            FileOutputStream fos = new FileOutputStream(new File(shadowLocation))
            fileProduct.digestAlgorithm = ChecksumUtility.DigestAlgorithm.MD5
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
               fileProduct.digestValue = checksum

               product.addFileProduct(fileProduct)

               // Generate geo file.  This is needed for MRF generation
               String geoName = "${shadow}${File.separator}${productName}.pgw"
               File geoFile = new File(geoName)
               geoFile << "0.333333333333\n0.000000000000\n0.000000000000\n-0.333333333333\n-179.833333333333\n89.833333333333"
               product.addFileProduct(new LocalFileProduct("${location}${File.separator}${productName}.pgw"))

               // download success
               logger.debug("Retrieved file: ${fileProduct.name}")
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

         ServiceProfile sp = productType.metadataHarvesterFactory.createMetadataHarvester().createServiceProfile(product)

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
            String errorDir = "${productType.validationError}${File.separator}${productName}"
            logger.error("Unable to extract metdata for ${productName}.  Moving product to ${errorDir}")

            new File(location).renameTo(new File(errorDir))

            this.sigEvent.create(EventType.Error, productType.name, 'MLSDataHandler',
                  'MLSDataHandler',
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
