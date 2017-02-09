package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Implementation of Metadata Harvester for the gov.nasa.gibs.tie.handlers.sftp.SFTP imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class SFTPMetadata implements MetadataHarvester {
   private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPMetadata.class)

   @Override
   public ServiceProfile createServiceProfile(Product product)
   throws ServiceProfileException {

      if (!(product.productType instanceof gov.nasa.gibs.tie.handlers.sftp.SFTPProductType)) {
         throw new ServiceProfileException("Invalid input product type.")
      }

      gov.nasa.gibs.tie.handlers.sftp.SFTPProductType productType = product.productType as gov.nasa.gibs.tie.handlers.sftp.SFTPProductType

      ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
      result.setSubmisson(result.createSubmission())

      SPAgent origin = result.createAgent()
      origin.name = "gov.nasa.gibs.tie.handlers.sftp.SFTP Data Handler"
      origin.address = InetAddress.localHost
      origin.time = new Date()
      result.messageOriginAgent = origin

      FileProduct imgFile  = product.files.find {
         it.name.endsWith('.png') || it.name.endsWith('.jpg') || it.name.endsWith('.jpeg') ||  it.name.endsWith('.tiff') ||  it.name.endsWith('.tif')
      } as FileProduct

      if (!imgFile) {
         throw new ServiceProfileException(
               "Unable to identify product ${product.name} data file.")
      }

      FileProduct worldFile = product.files.find {
         it.name.endsWith('.pgw') || it.name.endsWith('.jgw')
      } as FileProduct

      if (!worldFile) {
         throw new ServiceProfileException(
               "Unable to find local GEO file for ${product.name}.")
      }
	  
	  FileProduct metadataFile = product.files.find {
		  it.name.endsWith('.xml')
	   } as FileProduct
 
	   if (!metadataFile) {
		  throw new ServiceProfileException(
				"Unable to find metadata file for ${product.name}.")
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

      acquire.agent = 'gov.nasa.gibs.tie.handlers.sftp.SFTP_HANDLER'
      acquire.operation = 'ACQUIRE'

      SPOperation ingestOpt = header.createOperation()
      header.addOperation(ingestOpt)
      ingestOpt.agent = 'gov.nasa.gibs.tie.handlers.sftp.SFTP_HANDLER'
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
      header.createTime = new Date(imgFile.lastModifiedTime)
      header.status = "READY"

	 // Parse data from metadata XML file
	  def metadataXml = new XmlSlurper().parseText(new File (metadataFile.filename).text).declareNamespace('georss':'box')
	 
	  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	  sdf.setTimeZone(TimeZone.getTimeZone('GMT'))
	   
	  SPProductHistory history = metadata.createProductHistory()
	  metadata.productHistory = history
	  history.version = "1"
	  history.createDate = sdf.parse( formatTimeString( metadataXml.ProductionDateTime.toString() ) )
	  history.lastRevisionDate = new Date(imgFile.lastModifiedTime)
	  history.revisionHistory = "TBD"
	  
	  metadataXml.InputGranules.GranuleId.each { def granule ->
		  SPSourceProduct sourceProduct = history.createSourceProduct()
		  sourceProduct.setProductType(metadataXml.ProviderProductId.toString())
		  sourceProduct.setProduct(granule.toString())
		  sourceProduct.setMetadataRepo(SPSourceProduct.SPMetadateaRepo.ECHO_REST)
		  history.addSourceProduct(sourceProduct)
	  }
	  
	  // Get start/end dates from metadata file
	  Calendar cal = Calendar.instance
	  Date st = sdf.parse( formatTimeString( metadataXml.DataStartDateTime.toString() ) )
	  metadata.productStartTime = st.time
	  Date et = sdf.parse( formatTimeString( metadataXml.DataEndDateTime.toString() ) )
	  metadata.productStopTime = et.time
	  
	  metadataXml.DataDays.each { def dataDay ->
		  if(dataDay != null) { metadata.addDataDay(dataDay.toString() as Long) }
	  }
	  metadata.partialId = metadataXml.PartialId.toString()
	  
/*	  // Parse native spatial coverage from metadata file
	  //def coords = metadataXml.GROUP.find { it.GROUP_NAME == 'ARCHIVEDMETADATA' }
	  def coords = metadataXml.NativeSpatialCoverage.'georss:box'.text
	  logger.info("coords: ${coords} | ${coords.class}")
	  //def coords = metadataXml.NativeSpatialCoverage.'georss:box'.toString().split(" ")
	  //logger.info("test: " + metadataXml.NativeSpatialCoverage..toString())
	  logger.info(coords.split(" "))
	  SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
	  rectangle.southLatitude = coords[0]
	  rectangle.westLongitude = coords[1]
	  rectangle.northLatitude = coords[2]
	  rectangle.eastLongitude = coords[3]
	  metadata.addBoundingRectangle(rectangle)*/
		 
      // register png file
      if (imgFile) {
         SPIngestProductFile pngpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(pngpf)

         SPProductFile pf = pngpf.createProductFile()
         pngpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.IMAGE

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = imgFile.name
         spf.size = imgFile.size
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(imgFile.digestAlgorithm as String)
         spf.checksumValue = imgFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${imgFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.PNG

         SPFileDestination des = pngpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${imgFile.name}"
         pngpf.ingestStartTime = product.ingestStart
         pngpf.ingestStopTime = product.ingestStop
         pngpf.fileDestination = des
      }

      // register pgw file
      if (worldFile) {
         SPIngestProductFile pgwpf = ingest.createIngestProductFile()
         ingest.addIngestProductFile(pgwpf)

         SPProductFile pf = pgwpf.createProductFile()
         pgwpf.productFile = pf
         pf.fileType = SPCommon.SPFileClass.GEOMETADATA

         SPFile spf = pf.createFile()
         pf.file = spf
         spf.name = worldFile.name
         spf.size = worldFile.size
         spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(worldFile.digestAlgorithm as String)
         spf.checksumValue = worldFile.digestValue
         spf.addLink("file://${product.stageLocation}${File.separator}${worldFile.name}")
         spf.dataFormat = SPCommon.SPDataFormat.PGW

         SPFileDestination des = pgwpf.createFileDestination()
         des.location = "file://${product.stageLocation}${File.separator}${worldFile.name}"
         pgwpf.ingestStartTime = product.ingestStart
         pgwpf.ingestStopTime = product.ingestStop
         pgwpf.fileDestination = des

      }
	  
	  // register xml metadata file
	  if (metadataFile) {
		 SPIngestProductFile xmlpf = ingest.createIngestProductFile()
		 ingest.addIngestProductFile(xmlpf)

		 SPProductFile pf = xmlpf.createProductFile()
		 xmlpf.productFile = pf
		 pf.fileType = SPCommon.SPFileClass.METADATA

		 SPFile spf = pf.createFile()
		 pf.file = spf
		 spf.name = metadataFile.name
		 spf.size = metadataFile.size
		 spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(metadataFile.digestAlgorithm as String)
		 spf.checksumValue = metadataFile.digestValue
		 spf.addLink("file://${product.stageLocation}${File.separator}${metadataFile.name}")
		 spf.dataFormat = SPCommon.SPDataFormat.XML

		 SPFileDestination des = xmlpf.createFileDestination()
		 des.location = "file://${product.stageLocation}${File.separator}${metadataFile.name}"
		 xmlpf.ingestStartTime = product.ingestStart
		 xmlpf.ingestStopTime = product.ingestStop
		 xmlpf.fileDestination = des

	  }

      ingest.operationSuccess = true

      if (logger.traceEnabled) {
         logger.info(result.toString())
      }
      return result
   }
   
   /*
    * Timestamps parsed from the XML metadata file can have any arbitrary number of decimal seconds.
    * We wish to truncate or pad the string so that all timestamps have 3 digits after the decimal.
    * (GIBS-1189)
    */
   public String formatTimeString(String timeString) {
	   Pattern pattern = Pattern.compile("(?<=\\.)(\\d+)(?=Z)")
	   Matcher matcher = pattern.matcher(timeString)
	   
	   if(matcher.find()) {
		   def decimalSeconds = matcher.group()
		   
		   String formattedDecimalSeconds = String.format("%03d", Integer.parseInt(decimalSeconds))
		   formattedDecimalSeconds = String.format("%3.3s", formattedDecimalSeconds)
		   
		   timeString = matcher.replaceFirst(formattedDecimalSeconds)
	   } else {
	   		timeString = timeString.replace("Z", ".000Z")
	   }
	   return timeString
   }
}
