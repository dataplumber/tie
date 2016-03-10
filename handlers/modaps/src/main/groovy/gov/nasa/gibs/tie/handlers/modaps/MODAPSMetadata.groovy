package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Implementation of Metadata Harvester for the MODAPS imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODAPSMetadata implements MetadataHarvester {
   private static Log logger = LogFactory.getLog(MODAPSMetadata.class)

   @Override
   public ServiceProfile createServiceProfile(Product product)
   throws ServiceProfileException {

      if (!(product.productType instanceof MODAPSProductType)) {
         throw new ServiceProfileException("Invalid input product type.")
      }

      MODAPSProductType productType = product.productType as MODAPSProductType

      ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
      result.setSubmisson(result.createSubmission())

      SPAgent origin = result.createAgent()
      origin.name = "MODAPS Data Handler"
      origin.address = InetAddress.localHost
      origin.time = new Date()
      result.messageOriginAgent = origin

      FileProduct imageryFile
      FileProduct worldFile
      FileProduct txtFile

      for (FileProduct fp : product.files) {
         if (fp.name.endsWith('.jpg') || fp.name.endsWith('.png') || fp.name.endsWith('.tif')) {
            imageryFile = fp //"${product.stageLocation}${File.separator}${fp.name}"
         } else if (fp.name.endsWith('.jgw') || fp.name.endsWith('.pgw')) {
            worldFile = fp //"${product.stageLocation}${File.separator}${fp.name}"
         } else if (fp.name.endsWith('.txt')) {
            txtFile = fp //"${product.stageLocation}${File.separator}${fp.name}"
         }
      }

      if (!imageryFile) {
         throw new ServiceProfileException(
               "Unable to identify product ${product.name} data file.")
      }

      logger.debug("Start to harvest metadata for ${product.productType.name}:${product.name}")

      SPAgent target = result.createAgent()
      target.name = "Manager:${product.productType.name}"
      result.messageTargetAgent = target


      SPHeader header = result.submission.createHeader()
      result.submission.header = header

      SPIngest ingest = result.submission.createIngest()
      result.submission.ingest = ingest

      SPMetadata metadata = result.submission.createMetadata()
      result.submission.metadata = metadata

      // Captures some ingest stats
      SPOperation acquire = header.createOperation()
      header.addOperation(acquire)

      acquire.agent = 'MODAP_HANDLER'
      acquire.operation = 'ACQUIRE'

      SPOperation ingestOpt = header.createOperation()
      header.addOperation(ingestOpt)
      ingestOpt.agent = 'MODAP_HANDLER'
      ingestOpt.operation = 'INGEST'

      Date optstart = product.ingestStart
      Date optend = product.ingestStop

      acquire.operationStartTime = optstart
      acquire.operationStopTime = optend
      ingestOpt.operationStartTime = optstart
      ingestOpt.operationStopTime = optend


      header.productType = product.productType.name
      header.productName = product.name
      header.replace = product.name
      header.catalogOnly = false
      header.officialName = product.name
      header.version = "1"
      header.createTime = new Date(imageryFile.lastModifiedTime)
      header.status = "READY"

      if (productType.isTiled()) {
         metadata.numberOfColumns = productType.lastColumn + 1
         metadata.numberOfLines = productType.lastRow + 1
		 
		 // Get row & column from filename to extract PartialID (GIBS-688)
		 def pattern = /_r(\d{2,3})c(\d{2,3})./
		 def matcher = imageryFile.name =~ pattern
		 def row = String.format("%03d", matcher[0][1].toInteger())
		 def col = String.format("%03d", matcher[0][2].toInteger())
		  
         logger.trace("Extracted partial ID: ${productType.partialIdPrefix}${row}${col}")
		 metadata.setPartialId("${productType.partialIdPrefix}${row}${col}")

      }

      SPProductHistory history = metadata.createProductHistory()
      metadata.productHistory = history

   // get start date from file name
      Pattern p = Pattern.compile ("\\.(\\d{4})(\\d{3})\\.")
      Matcher m = p.matcher(imageryFile.name)
      if (m.find()) {
         Calendar cal = Calendar.instance
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-DDD")
         sdf.setTimeZone(TimeZone.getTimeZone('GMT'))
         Date st = sdf.parse("${m.group(1)}-${m.group(2)}")
         logger.debug("Product start time: ${st}")
         metadata.productStartTime = st.time
         metadata.productStopTime = (st + 1).time - 1  // we assumed this is a daily image
		 metadata.addDataDay(st.getTime())
      } else {
         throw new ServiceProfileException("Bad file naming encountered '${imageryFile.name}.  Aborting...")
      }

      history.version = "1"
      history.createDate = new Date(imageryFile.lastModifiedTime)
      history.lastRevisionDate = new Date(imageryFile.lastModifiedTime)
      history.revisionHistory = "TBD"

      def contents = [:]
      if (txtFile) {
         new File("${product.stageLocation}${File.separator}${txtFile.name}").eachLine { String line ->
            def t = line.split(':')
            if (t.size() == 2) {
               contents[t[0].trim()] = t[1].trim()
            }
         }
      }

      if (contents.size() > 1) {
         SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
         metadata.addBoundingRectangle(rectangle)
         rectangle.eastLongitude = contents['UR lon'] as Double
         rectangle.northLatitude = contents['UL lat'] as Double
         rectangle.southLatitude = contents['LR lat'] as Double
         rectangle.westLongitude = contents['LL lon'] as Double
      }

      List<String> gs = (contents['L2 granules'] as String).split(' ')
      gs.each { String g ->
         SPSourceProduct sp = history.createSourceProduct()
         history.addSourceProduct(sp)

         // TODO: this should be the official dataset name from ECHO.
         // need to get this from provider
         String ptn = product.productType.name
         if (ptn.endsWith('_SRC')) {
            ptn = ptn.substring(0, ptn.lastIndexOf('_SRC'))
         }
         sp.productType = ptn
         sp.product = g.trim()
      }

      if (txtFile) {
         SPIngestProductFile txtpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(txtpf)

         // create txt product file
         SPProductFile pf = txtpf.createProductFile()
         txtpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.METADATA

         // register file object
         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = txtFile.name
         spf.size = txtFile.size
         logger.debug("${txtFile.name} digest alg = ${txtFile.digestAlgorithm as String}")
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(txtFile.digestAlgorithm as String)
         spf.checksumValue = txtFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${txtFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.ASCII

         SPFileDestination des = txtpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${txtFile.name}"
         txtpf.ingestStartTime = product.ingestStart
         txtpf.ingestStopTime = product.ingestStop
         txtpf.fileDestination = des
      }

      // register jpg file
      if (imageryFile) {
         SPIngestProductFile imagerypf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(imagerypf)

         SPProductFile pf = imagerypf.createProductFile()
         imagerypf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.IMAGE

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = imageryFile.name
		 if(imageryFile.size == 0) {
			 File tmpFile = new File("${product.stageLocation}${File.separator}${imageryFile.name}")
			 spf.size = tmpFile.length()
		 } else {
			 spf.size = imageryFile.size
		 }

         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(imageryFile.digestAlgorithm as String)
         spf.checksumValue = imageryFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${imageryFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.JPEG

         SPFileDestination des = imagerypf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${imageryFile.name}"
         imagerypf.ingestStartTime = product.ingestStart
         imagerypf.ingestStopTime = product.ingestStop
         imagerypf.fileDestination = des
      }

      // register jpw file
      if (worldFile) {
         SPIngestProductFile worldpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(worldpf)

         SPProductFile pf = worldpf.createProductFile()
         worldpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.GEOMETADATA

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = worldFile.name
         spf.size = worldFile.size
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(worldFile.digestAlgorithm as String)
         spf.checksumValue = worldFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${worldFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.JGW

         SPFileDestination des = worldpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${worldFile.name}"
         worldpf.ingestStartTime = product.ingestStart
         worldpf.ingestStopTime = product.ingestStop
         worldpf.fileDestination = des

      }

      ingest.operationSuccess = true

      if (logger.traceEnabled) {
         logger.info(result.toString())
      }
      return result
   }
}
