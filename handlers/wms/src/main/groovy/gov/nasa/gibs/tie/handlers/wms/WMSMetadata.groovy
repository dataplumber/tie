package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat

/**
 * Implementation of Metadata Harvester for the WMS imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class WMSMetadata implements MetadataHarvester {
	private static Log logger = LogFactory.getLog(WMSMetadata.class)

	@Override
	public ServiceProfile createServiceProfile(Product product)
	throws ServiceProfileException {

		if (!(product.productType instanceof WMSProductType)) {
			throw new ServiceProfileException("Invalid input product type.")
		}

		WMSProductType productType = product.productType as WMSProductType

		ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
		result.setSubmisson(result.createSubmission())

		SPAgent origin = result.createAgent()
		origin.name = "WMS Data Handler"
		origin.address = InetAddress.localHost
		origin.time = new Date()
		result.messageOriginAgent = origin

		FileProduct imgFile  = product.files.find {
			it.name.endsWith(productType.fileExtension)
		} as FileProduct

		if (!imgFile) {
			throw new ServiceProfileException(
			"Unable to identify product ${product.name} data file.")
		}

		FileProduct worldFile = product.files.find {
			(it.name.endsWith('.pgw') || it.name.endsWith('.jgw'))
		} as FileProduct

		if (!worldFile) {
			throw new ServiceProfileException(
			"Unable to locate GEO file for ${product.name}.")
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

		acquire.agent = 'WMS_HANDLER'
		acquire.operation = 'ACQUIRE'

		SPOperation ingestOpt = header.createOperation()
		header.addOperation(ingestOpt)
		ingestOpt.agent = 'WMS_HANDLER'
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

		SPProductHistory history = metadata.createProductHistory()
		metadata.productHistory = history

		// extract the date from the last modification time as the start time
		Calendar cal = Calendar.instance
		cal.time = new Date(imgFile.lastModifiedTime)
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
		sdf.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
		Date st = sdf.parse("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}")
		logger.debug("Product start time: ${st}")
		metadata.productStartTime = st.time
		metadata.addDataDay(imgFile.lastModifiedTime)

		history.version = "1"
		history.createDate = new Date(imgFile.lastModifiedTime)
		history.lastRevisionDate = new Date(imgFile.lastModifiedTime)
		history.revisionHistory = "TBD"

		SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
		metadata.addBoundingRectangle(rectangle)
		rectangle.eastLongitude = 180.0
		rectangle.northLatitude = 90.0
		rectangle.southLatitude = -90.0
		rectangle.westLongitude = -180.0

		// register image file
		if (imgFile) {
			SPIngestProductFile imgPf = ingest.createIngestProductFile()
			ingest.addIngestProductFile(imgPf)

			SPProductFile pf = imgPf.createProductFile()
			imgPf.productFile = pf
			pf.fileType = SPCommon.SPFileClass.IMAGE

			SPFile spf = pf.createFile()
			pf.file = spf
			spf.name = imgFile.name
			spf.size = imgFile.size
			spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(imgFile.digestAlgorithm as String)
			spf.checksumValue = imgFile.digestValue
			spf.addLink("file://${product.stageLocation}${File.separator}${imgFile.name}")
			
			if(productType.fileExtension.equals(".png")) {
				spf.dataFormat = SPCommon.SPDataFormat.PNG
			} else {
				spf.dataFormat = SPCommon.SPDataFormat.JPG
			}

			SPFileDestination des = imgPf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${imgFile.name}"
			imgPf.ingestStartTime = product.ingestStart
			imgPf.ingestStopTime = product.ingestStop
			imgPf.fileDestination = des
		}

		// register world file
		if (worldFile) {
			SPIngestProductFile worldPf = ingest.createIngestProductFile()
			ingest.addIngestProductFile(worldPf)

			SPProductFile pf = worldPf.createProductFile()
			worldPf.productFile = pf
			pf.fileType = SPCommon.SPFileClass.GEOMETADATA

			SPFile spf = pf.createFile()
			pf.file = spf
			spf.name = worldFile.name
			spf.size = worldFile.size
			spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(worldFile.digestAlgorithm as String)
			spf.checksumValue = worldFile.digestValue
			spf.addLink("file://${product.stageLocation}${File.separator}${worldFile.name}")
			spf.dataFormat = SPCommon.SPDataFormat.PGW

			SPFileDestination des = worldPf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${worldFile.name}"
			worldPf.ingestStartTime = product.ingestStart
			worldPf.ingestStopTime = product.ingestStop
			worldPf.fileDestination = des

		}

		ingest.operationSuccess = true

		if (logger.traceEnabled) {
			logger.info(result.toString())
		}
		return result
	}
}
