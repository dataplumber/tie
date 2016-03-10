/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.gibs.tie.handlers.common.TarGzipUtil
import gov.nasa.gibs.tie.handlers.sips.common.PdrSlurper
import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.util.FileProductUtility
import gov.nasa.horizon.common.api.util.URIPath
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @author T. Huang
 * @version $Id:$
 */
class PDRProcessor {
   private static Log logger = LogFactory.getLog(PDRProcessor.class)

   private PDRProductType sipsProductType
   private String pdrProductType
   private String pdrFile
   private String hostURI
   private def pdr
   private int fileCount = 0
   private int expFileCount = 0

   public PDRProcessor(PDRProductType sipsProductType, String pdrFile) {
      this.sipsProductType = sipsProductType
      this.pdrFile = pdrFile
      if (this.pdrFile.endsWith('.pdr')) {
         this.pdrProductType = this.pdrFile.substring(this.pdrFile.lastIndexOf('/') + 1, this.pdrFile.lastIndexOf('.pdr'))
      } else if (this.pdrFile.endsWith('.PDR')) {
         this.pdrProductType = this.pdrFile.substring(this.pdrFile.lastIndexOf('/') + 1, this.pdrFile.lastIndexOf('.PDR'))
      }
      this.hostURI = URIPath.createURIPath(this.sipsProductType.sourceURL).hostURI
      pdr = new PdrSlurper().parseText(new File(this.pdrFile).text)
   }

   class ProductJob {
      PDRProductType sipsProductType
      String pdrProductType
      String productType
      String productName
      String directory
      String fileName
      String fileURL
      long fileSize
      //int crc32
      String checksum

      Boolean success
      String disposition
   }

   def ingest = { ProductJob pj ->
      logger.debug("inside ingest closure")
      def productTypeRoot = "${pj.sipsProductType.dataStorageRoot}${File.separator}${pj.productType}"
      def ptroot = new File(productTypeRoot)
      if (!ptroot.exists()) {
         ptroot.mkdirs()
      }
      def destination = "${productTypeRoot}${File.separator}${pj.productName}"
      def shadow = "${productTypeRoot}${File.separator}.shadow${File.separator}${pj.productName}"
      File dir = new File(shadow)
      if (!dir.exists()) {
         dir.mkdirs()
      }
      FileProductUtility fpu = new FileProductUtility(pj.fileURL)
      if (pj.sipsProductType.user) {
         fpu.setAuthentication(pj.sipsProductType.user, pj.sipsProductType.pass)
      }

      if (!fpu.verifyFileExistence()) {
         pj.disposition = "\"FILE ${pj.fileURL} NOT EXISTS\""
         logger.error pj.disposition
         new File(shadow).deleteDir()
         pj.success = false
         return
      }

      logger.debug "downloading ${shadow}${File.separator}${pj.fileName}"

      FileProduct fileProduct = fpu.file
      try {
         new File("${shadow}${File.separator}${pj.fileName}") << fileProduct.inputStream
      } catch (Exception e) {
         pj.disposition = "\"FAILED TO DOWNLOAD FILE ${pj.fileURL}\""
         logger.error pj.disposition
         new File(shadow).deleteDir()
         pj.success = false
         return
      } finally {
         fileProduct.close()
         fpu.cleanUp()
      }

      logger.debug "done downloading ${shadow}${File.separator}${pj.fileName}"

      // untar the file
      try {
         logger.debug "unzipping file ${shadow}${File.separator}${pj.fileName}"
         File unzipped = TarGzipUtil.unGzip(new File("${shadow}${File.separator}${pj.fileName}"), new File(shadow))
         def files = TarGzipUtil.unTar(unzipped, new File(shadow))
         if (files.size() != 0) {
            if (logger.debugEnabled) {
               logger.debug "Successfully unzipped and untared file ${pj.fileName}.  File listing..."
               files.each { File f ->
                  logger.debug("${f.name} ${f.size()}")
               }
            }
            new File("${shadow}${File.separator}${pj.fileName}").delete()
            unzipped.delete()
         }
      } catch (Exception e) {
         pj.disposition = "\"FAILED TO OPEN FILE ${pj.fileName}\""
         logger.debug(pj.disposition)
         new File(shadow).deleteDir()
         pj.success = false
      }

      new File(shadow).renameTo(new File(destination))
      pj.disposition = '"SUCCESSFUL"'
      pj.success = true
   }

   public void run() {
      this.expFileCount = this.pdr.TOTAL_FILE_COUNT.toInteger()
      def jobs = []
      pdr.OBJECT.each { group ->
         if (group.OBJECT_NAME == 'FILE_GROUP') {
            //String pt = group.DATA_TYPE
            group.OBJECT.each { spec ->
               if (spec.OBJECT_NAME == 'FILE_SPEC') {
                  ProductJob pj = new ProductJob()
                  pj.sipsProductType = this.sipsProductType
                  pj.pdrProductType = this.pdrProductType
                  String fn = spec.FILE_ID
                  pj.productName = fn.substring(0, fn.lastIndexOf('.tar.gz'))
                  pj.productType = group.DATA_TYPE
                  pj.directory = spec.DIRECTORY_ID
                  pj.fileName = spec.FILE_ID
                  pj.fileSize = spec.FILE_SIZE.toLong()
                  //pj.crc32 = spec.FILE_CKSUM_VALUE.toLong()
                  pj.checksum = spec.FILE_CKSUM_VALUE

                  if (hostURI == null) {
                     URI baseURI = new URI(this.sipsProductType.sourceURL)
                     hostURI = "${baseURI.getScheme()}://${baseURI.getHost()}"
                  }
                  pj.fileURL = "${hostURI}${File.separator}${spec.DIRECTORY_ID}${File.separator}${spec.FILE_ID}"

                  jobs << pj
                  this.sipsProductType.ingestPool.execute("${pj.productType}:${pj.productName}") {
                     this.ingest(pj)
                  }
               }
            }
         }
      }

      logger.debug "check while loop"
      while (jobs.count { it.success == null } != 0) {
         try {
            Thread.sleep(1000)
            if (logger.debugEnabled) {
               jobs.each { ProductJob p ->
                  logger.trace("${p.productType}:${p.productName} status=${p.success}")
               }
            }
         } catch (InterruptedException e) {
            logger.error(e.message, e)
         }
      }

      logger.debug "Shutting down workers"
      this.fileCount = jobs.count { it.success == true }
      if (fileCount != this.expFileCount) {
         logger.error("Expecting ${this.expFileCount} files, but only received ${fileCount} files.")
      }

      // log a PAN file
      if (!this.sipsProductType.panURL) {
         logger.warn("PAN URL is not specified.  A PAN file will not be logged.  Please inform the data provider.")
         return
      }
      FileProductUtility pan = new FileProductUtility("${this.sipsProductType.panURL}${File.separator}${this.pdrProductType}.PAN")
      if (this.sipsProductType.user) {
         pan.setAuthentication(this.sipsProductType.user, this.sipsProductType.pass)
      }
      StringBuffer sb = new StringBuffer()
      sb.append('MESSAGE_TYPE = LONGPAN;\n')
      sb.append("NO_OF_FILES = ${jobs.size()};\n")
      jobs.each { ProductJob job ->
         sb.append("FILE_DIRECTORY = ${job.directory};\n")
         sb.append("FILE_NAME = ${job.fileName};\n")
         sb.append("DISPOSITION = ${job.disposition};\n")
         sb.append("TIME_STAMP = ;\n\n")
      }

      logger.debug("Writing to remote PAN file...")
      logger.debug(sb.toString())

      try {
         pan.writeFile(new ByteArrayInputStream(sb.toString().bytes))
      } catch (Exception e) {
         logger.error("Unable to write PAN to to the specified destination: ${this.sipsProductType.panURL}.  A PAN file will not be logged.  Please inform the data provider.")
      } finally {
         pan.cleanUp()
      }
      logger.debug("Done handling remote PAN file.")
   }
}
