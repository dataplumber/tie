/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import java.util.regex.Matcher
import java.util.regex.Pattern
import gov.nasa.gibs.tie.handlers.common.TarGzipUtil
import gov.nasa.gibs.tie.handlers.sips.common.PdrSlurper
import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.util.FileProductUtility
import gov.nasa.horizon.common.api.util.URIPath
import gov.nasa.gibs.tie.handlers.sips.pdr.PDRErrorUtility
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
   
   boolean validateFileGroup(PDRErrorUtility.ProductJob pj) {  
	   pj.valid = false
	   PDRErrorUtility.PDRDLongDisposition disposition
	   
	   if( pj.productType == null || pj.productType.equals("") || pj.productVersion == null || pj.productVersion.equals("") ) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.DATA_TYPE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.directory == null || pj.directory.equals("")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.DIRECTORY
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.fileSize == null || pj.fileSize == 0) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.FILE_SIZE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.productName == null || pj.productName.equals("")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.FILE_ID
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.fileType == null || pj.fileType.equals("") || !pj.fileType.equals("TGZ")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.FILE_TYPE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.checksumType == null || pj.checksumType.equals("")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.MISSING_CKSUM_TYPE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }
	   else if(pj.checksum == null || pj.checksum.equals("")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.MISSING_CKSUM_VALUE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   } 
	   else if(pj.checksumType.equals("MD5")) {
			Pattern p = Pattern.compile("[0-9a-f]{32}")
			Matcher m = p.matcher(pj.checksum)

			if(m.matches()) {
				PDRErrorUtility.PDRDLongDisposition d = PDRErrorUtility.PDRDLongDisposition.SUCCESS
				pj.dispositionPDRD = d.toString()
				PDRErrorUtility.PANLongDisposition d2 = PDRErrorUtility.PANLongDisposition.SUCCESS
				pj.dispositionPAN = d2.toString()
				pj.valid = true
				return true
			} else {
				disposition = PDRErrorUtility.PDRDLongDisposition.INVALID_CKSUM_VALUE
				pj.dispositionPDRD = disposition.toString()
				return false
			}
		}
	   else if(pj.checksumType.equals("SHA1")) {
		   Pattern p = Pattern.compile("[0-9a-f]{40}")
		   Matcher m = p.matcher(pj.checksum)
		   
			if(m.matches()) {
				PDRErrorUtility.PDRDLongDisposition d = PDRErrorUtility.PDRDLongDisposition.SUCCESS
				pj.dispositionPDRD = d.toString()
				PDRErrorUtility.PANLongDisposition d2 = PDRErrorUtility.PANLongDisposition.SUCCESS
				pj.dispositionPAN = d2.toString()
				pj.valid = true
				return true
			} else {
				disposition = PDRErrorUtility.PDRDLongDisposition.INVALID_CKSUM_VALUE
				pj.dispositionPDRD = disposition.toString()
				return false
			}
	   }
	   else if(!pj.checksumType.equals("MD5") && !pj.checksumType.equals("SHA1")) {
		   disposition = PDRErrorUtility.PDRDLongDisposition.UNSUPPORTED_CKSUM_TYPE
		   pj.dispositionPDRD = disposition.toString()
		   return false
	   }   
   }

   def ingest = { PDRErrorUtility.ProductJob pj ->
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
         pj.dispositionPAN = "\"FILE ${pj.fileURL} NOT EXISTS\""
         logger.error pj.dispositionPAN
         new File(shadow).deleteDir()
         pj.success = false
         return
      }

      logger.debug "downloading ${shadow}${File.separator}${pj.fileName}"

      FileProduct fileProduct = fpu.file
      try {
         new File("${shadow}${File.separator}${pj.fileName}") << fileProduct.inputStream
      } catch (Exception e) {
         pj.dispositionPAN = "\"FAILED TO DOWNLOAD FILE ${pj.fileURL}\""
         logger.error pj.dispositionPAN
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
         pj.dispositionPAN = "\"FAILED TO OPEN FILE ${pj.fileName}\""
         logger.debug(pj.dispositionPAN)
         new File(shadow).deleteDir()
         pj.success = false
      }

      new File(shadow).renameTo(new File(destination))
      pj.dispositionPAN = '"SUCCESSFUL"'
      pj.success = true
   }

   public void run() {
	  if( this.pdr.TOTAL_FILE_COUNT != null && !this.pdr.TOTAL_FILE_COUNT.equals("") ) {
			this.expFileCount = this.pdr.TOTAL_FILE_COUNT.toInteger()  
	  }
	  if(this.expFileCount < 1) { // If PDR fails TOTAL_FILE_COUNT validation, skip all other logic
		  //TODO SigEvent
		  PDRErrorUtility.writePDRDShortFile(this.sipsProductType, this.pdrProductType, PDRErrorUtility.PDRDShortDisposition.FILE_COUNT)
	  } 
		else {
			int invalidFileGroups = 0
			def jobs = []
			pdr.OBJECT.each { group ->
				if (group.OBJECT_NAME == 'FILE_GROUP') {
					//String pt = group.DATA_TYPE
					group.OBJECT.each { spec ->
						if (spec.OBJECT_NAME == 'FILE_SPEC') {
							PDRErrorUtility.ProductJob pj = new PDRErrorUtility.ProductJob()
							pj.sipsProductType = this.sipsProductType
							pj.pdrProductType = this.pdrProductType
							pj.productType = group.DATA_TYPE
							pj.productVersion = group.DATA_VERSION
							pj.directory = spec.DIRECTORY_ID

							if(spec.FILE_ID == null || spec.FILE_ID.equals("")) {
								pj.productName = ""
							} else {
								String fn = spec.FILE_ID
								pj.fileName = fn
								pj.productName = fn.substring(0, fn.lastIndexOf('.tar.gz'))
							}
							pj.fileType = spec.FILE_TYPE

							if(spec.FILE_SIZE == null || spec.FILE_SIZE.equals("")) {
								pj.fileSize = 0
							} else {
								pj.fileSize = spec.FILE_SIZE.toLong()
							}
							pj.checksumType = spec.FILE_CKSUM_TYPE
							//pj.crc32 = spec.FILE_CKSUM_VALUE.toLong()
							pj.checksum = spec.FILE_CKSUM_VALUE

							if (hostURI == null) {
								URI baseURI = new URI(this.sipsProductType.sourceURL)
								hostURI = "${baseURI.getScheme()}://${baseURI.getHost()}"
							}
							pj.fileURL = "${hostURI}${File.separator}${spec.DIRECTORY_ID}${File.separator}${spec.FILE_ID}"

							boolean fileGroupValid = this.validateFileGroup(pj)
							jobs << pj

							if(fileGroupValid) {
								this.sipsProductType.ingestPool.execute("${pj.productType}:${pj.productName}") {

									this.ingest(pj)
								}
							} else {
								this.expFileCount--
								invalidFileGroups++
							}
						}
					}
				}
			}
		  
	      logger.debug "check while loop"
	      while (jobs.count { it.valid == null && it.success == null } != 0) {
	         try {
	            Thread.sleep(1000)
	            if (logger.debugEnabled) {
	               jobs.each { PDRErrorUtility.ProductJob p ->
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
	
	      //Call PDRD / PAN function
		 PDRErrorUtility.writeLongPdrdOrPanFile(this.sipsProductType, jobs)
	   }
   }
}
