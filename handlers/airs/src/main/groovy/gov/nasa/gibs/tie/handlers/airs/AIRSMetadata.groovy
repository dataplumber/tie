package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat

/**
 * Implementation of Metadata Harvester for the AIRS imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class AIRSMetadata implements MetadataHarvester {
   private static Log logger = LogFactory.getLog(AIRSMetadata.class)

   @Override
   public ServiceProfile createServiceProfile(Product product)
   throws ServiceProfileException {

      if (!(product.productType instanceof AIRSProductType)) {
         throw new ServiceProfileException("Invalid input product type.")
      }

      AIRSProductType productType = product.productType as AIRSProductType

      ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
      result.setSubmisson(result.createSubmission())

      SPAgent origin = result.createAgent()
      origin.name = "AIRS Data Handler"
      origin.address = InetAddress.localHost
      origin.time = new Date()
      result.messageOriginAgent = origin

      FileProduct pngFile  = product.files.find {
         it.name.endsWith('.png')
      } as FileProduct

      if (!pngFile) {
         throw new ServiceProfileException(
               "Unable to identify product ${product.name} data file.")
      }

      FileProduct pgwFile = product.files.find {
         it.name.endsWith('.pgw')
      } as FileProduct

      if (!pgwFile) {
         throw new ServiceProfileException(
               "Unable to local GEO file for ${product.name}.")
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

      acquire.agent = 'AIRS_HANDLER'
      acquire.operation = 'ACQUIRE'

      SPOperation ingestOpt = header.createOperation()
      header.addOperation(ingestOpt)
      ingestOpt.agent = 'AIRS_HANDLER'
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
      header.createTime = new Date(pngFile.lastModifiedTime)
      header.status = "READY"

      SPProductHistory history = metadata.createProductHistory()
      metadata.productHistory = history

      // extract the date from the last modification time as the start time
      Calendar cal = Calendar.instance
      cal.time = new Date(pngFile.lastModifiedTime)
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
      sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
      Date st = sdf.parse("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}")
      logger.debug("Product start time: ${st}")
      metadata.productStartTime = st.time
	  metadata.addDataDay(pngFile.lastModifiedTime)

      history.version = "1"
      history.createDate = new Date(pngFile.lastModifiedTime)
      history.lastRevisionDate = new Date(pngFile.lastModifiedTime)
      history.revisionHistory = "TBD"

         SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
         metadata.addBoundingRectangle(rectangle)
         rectangle.eastLongitude = 180.0
         rectangle.northLatitude = 90.0
         rectangle.southLatitude = -90.0
         rectangle.westLongitude = -180.0

      // register png file
      if (pngFile) {
         SPIngestProductFile pngpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(pngpf)

         SPProductFile pf = pngpf.createProductFile()
         pngpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.IMAGE

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = pngFile.name
         spf.size = pngFile.size
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(pngFile.digestAlgorithm as String)
         spf.checksumValue = pngFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${pngFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.PNG

         SPFileDestination des = pngpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${pngFile.name}"
         pngpf.ingestStartTime = product.ingestStart
         pngpf.ingestStopTime = product.ingestStop
         pngpf.fileDestination = des
      }

      // register pgw file
      if (pgwFile) {
         SPIngestProductFile pgwpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(pgwpf)

         SPProductFile pf = pgwpf.createProductFile()
         pgwpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.GEOMETADATA

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = pgwFile.name
         spf.size = pgwFile.size
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(pgwFile.digestAlgorithm as String)
         spf.checksumValue = pgwFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${pgwFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.PGW

         SPFileDestination des = pgwpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${pgwFile.name}"
         pgwpf.ingestStartTime = product.ingestStart
         pgwpf.ingestStopTime = product.ingestStop
         pgwpf.fileDestination = des

      }

      ingest.operationSuccess = true

      if (logger.traceEnabled) {
         logger.info(result.toString())
      }
      return result
   }
}
