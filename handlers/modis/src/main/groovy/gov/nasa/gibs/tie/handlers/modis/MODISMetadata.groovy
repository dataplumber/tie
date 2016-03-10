package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat

/**
 * Implementation of Metadata Harvester for the MODIS imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODISMetadata implements MetadataHarvester {
   private static Log logger = LogFactory.getLog(MODISMetadata.class)

   @Override
   public ServiceProfile createServiceProfile(Product product)
   throws ServiceProfileException {

      if (!(product.productType instanceof MODISProductType)) {
         throw new ServiceProfileException("Invalid input product type.")
      }

      MODISProductType productType = product.productType as MODISProductType

      ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
      result.setSubmisson(result.createSubmission())

	  try{
	      SPAgent origin = result.createAgent()
	      origin.name = "MODIS Data Handler"
	      origin.address = InetAddress.localHost
	      origin.time = new Date()
	      result.messageOriginAgent = origin
	  } catch(Exception e) {
		logger.error(e)
	  }
	  
      FileProduct pngFile
      FileProduct pgwFile

      for (FileProduct fp : product.files) {
         if (fp.name.endsWith('.png')) {
            pngFile = fp
         } else if (fp.name.endsWith('.pgw')) {
            pgwFile = fp
         }
      }

      if (!pngFile || !pgwFile) {
         throw new ServiceProfileException(
               "Unable to identify product ${product.name} data file.")
      }

      logger.debug("Start to harvest metadata for ${product.productType.name}:${product.name}")

      SPAgent target = result.createAgent()
      target.name = "Manager:${product.productType.name}"
      result.messageTargetAgent = target

      logger.trace ("Create SIP header")
      SPHeader header = result.submission.createHeader()
      result.submission.header = header

      SPIngest ingest = result.submission.createIngest()
      result.submission.ingest = ingest

	  
      SPMetadata metadata = result.submission.createMetadata()
      result.submission.metadata = metadata

      logger.trace ("Capture some ingest stats")

      // Captures some ingest stats
      SPOperation acquire = header.createOperation()
      header.addOperation(acquire)

      acquire.agent = 'MODIS_HANDLER'
      acquire.operation = 'ACQUIRE'

      SPOperation ingestOpt = header.createOperation()
      header.addOperation(ingestOpt)
      ingestOpt.agent = 'MODIS_HANDLER'
      ingestOpt.operation = 'INGEST'

      Date optstart = product.ingestStart
      Date optend = product.ingestStop

      acquire.operationStartTime = optstart
      acquire.operationStopTime = optend
      ingestOpt.operationStartTime = optstart
      ingestOpt.operationStopTime = optend

      logger.trace ("Register product type to SIP header")

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
      /*      
      Calendar cal = Calendar.instance
      cal.time = new Date(pgwFile.lastModifiedTime)
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
      sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
      Date st = sdf.parse("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}")
      logger.debug("Product start time: ${st}")
      metadata.productStartTime = st.time
      */

      logger.trace ("Extract date from file name ${pngFile.name}")
      
      // get start date from file name
      String fileName = pngFile.getName()
      def matches = fileName =~ /(\d{4})(\d{3})/
      logger.trace (matches)
      String year = matches[0][1]
      String jday = matches[0][2]
      
      logger.trace("year $year}")
      logger.trace("jday $jday}")
      
      def dateOfData = new GregorianCalendar()
	  dateOfData.setTimeZone(TimeZone.getTimeZone("GMT"))
      dateOfData.set(Calendar.YEAR, year as Integer)
      dateOfData.set(Calendar.DAY_OF_YEAR, jday as Integer)
      dateOfData.set(GregorianCalendar.HOUR_OF_DAY, 0)
      dateOfData.set(GregorianCalendar.MINUTE, 0)
      dateOfData.set(GregorianCalendar.SECOND, 0)
      dateOfData.set(GregorianCalendar.MILLISECOND,0)
      metadata.productStartTime = dateOfData.getTimeInMillis()
	  metadata.addDataDay(dateOfData.getTimeInMillis()) //GIBS-655 extract data day to be included in SIP.
      
      logger.trace ("Update history object")

      history.version = "1"
      history.createDate = new Date(pngFile.lastModifiedTime)
      history.lastRevisionDate = new Date(pngFile.lastModifiedTime)
      history.revisionHistory = "TBD"

      /*
      def contents = [:]
      if (txtFile) {
         new File("${product.stageLocation}${File.separator}${txtFile.name}").eachLine { String line ->
            def t = line.split(':')
            if (t.size() == 2) {
               contents[t[0].trim()] = t[1].trim()
            }
         }
      }
      */

      //if (contents.size() > 1) {
         SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
         metadata.addBoundingRectangle(rectangle)
         rectangle.eastLongitude = 180.0
         rectangle.northLatitude = 90.0
         rectangle.southLatitude = -90.0
         rectangle.westLongitude = -180.0
      //}

      /*
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
      */

      logger.trace("Process PNG file")

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

      logger.trace ("Process pgw file")

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
