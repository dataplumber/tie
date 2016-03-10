/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.gibs.tie.handlers.sips.common.MetSlurper
import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.handlers.framework.MetadataHarvester
import gov.nasa.horizon.handlers.framework.Product
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.SimpleDateFormat

/**
 * Implementation of Metadata Harvester for the SipsImagery imagery products
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryMetadata implements MetadataHarvester {
	private static Log logger = LogFactory.getLog(SipsImageryMetadata.class)

	@Override
	public ServiceProfile createServiceProfile(Product product)
	throws ServiceProfileException {

		if (!(product.productType instanceof SipsImageryProductType)) {
			throw new ServiceProfileException("Invalid input product type.")
		}

		SipsImageryProductType productType = product.productType as SipsImageryProductType

		ServiceProfile result = ServiceProfileFactory.instance.createServiceProfile()
		result.setSubmisson(result.createSubmission())

		SPAgent origin = result.createAgent()
		origin.name = "SipsImagery Data Handler"
		origin.address = InetAddress.localHost
		origin.time = new Date()
		result.messageOriginAgent = origin

		FileProduct imageryFile
		FileProduct worldFile
		FileProduct metFile
		FileProduct textFile
        
		Boolean hasWorldFile = true
        

		for (FileProduct fp : product.files) {
			if (fp.name.endsWith('.png') || fp.name.endsWith('.jpg') || fp.name.endsWith('.tif') || fp.name.endsWith('.tiff')) {
				imageryFile = fp
			} else if (fp.name.endsWith('.pgw') || fp.name.endsWith('.jgw')) {
				worldFile = fp
			} else if (fp.name.endsWith('.met')) {
				metFile = fp
			} else if (fp.name.endsWith('.txt')) {
				textFile = fp
			}
			if ( fp.name.endsWith('.tif') || fp.name.endsWith('.tiff') ) {
				hasWorldFile = false
			}
		}

		if (!imageryFile || (!worldFile && hasWorldFile) || !metFile) {
			throw new ServiceProfileException(
			"Unable to identify product ${product.name} data file.")
		}

		logger.debug("Start to harvest metadata for ${product.productType.name}:${product.name}")

		// convert MET data into XML to simplify harvesting
		def metFileLoc = "${product.stageLocation}${File.separator}${metFile.name}"
		def met = new MetSlurper().parseText(new File(metFileLoc).text)

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

		acquire.agent = 'SipsImagery_HANDLER'
		acquire.operation = 'ACQUIRE'

		SPOperation ingestOpt = header.createOperation()
		header.addOperation(ingestOpt)
		ingestOpt.agent = 'SipsImagery_HANDLER'
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
		header.status = "READY"
		header.createTime = new Date(imageryFile.lastModifiedTime)

		SPProductHistory history = metadata.createProductHistory()
		metadata.productHistory = history

		history.version = "1"
		history.createDate = new Date(imageryFile.lastModifiedTime)
		history.lastRevisionDate = new Date(imageryFile.lastModifiedTime)
		history.revisionHistory = "TBD"


		def archiveData = met.GROUP.find { it.GROUP_NAME == 'ARCHIVEDMETADATA' }
		if (archiveData) {
			try {
				def boundingRectangle = archiveData.GROUP.find { it.GROUP_NAME == 'BOUNDINGRECTANGLE' }
				if (boundingRectangle) {
					SPBoundingRectangle rectangle = metadata.createBoundingRectangle()
					metadata.addBoundingRectangle(rectangle)
	
					def north = boundingRectangle.OBJECT.find { it.OBJECT_NAME == 'NORTHBOUNDINGCOORDINATE' }
					if (north) {
						rectangle.northLatitude = north.VALUE.toDouble()
					}
	
					def south = boundingRectangle.OBJECT.find { it.OBJECT_NAME == 'SOUTHBOUNDINGCOORDINATE' }
					if (south) {
						rectangle.southLatitude = south.VALUE.toDouble()
					}
	
					def east = boundingRectangle.OBJECT.find { it.OBJECT_NAME == 'EASTBOUNDINGCOORDINATE' }
					if (east) {
						rectangle.eastLongitude = east.VALUE.toDouble()
					}
	
					def west = boundingRectangle.OBJECT.find { it.OBJECT_NAME == 'WESTBOUNDINGCOORDINATE' }
					if (west) {
						rectangle.westLongitude = west.VALUE.toDouble()
					}
				}
	
				// extract file creation time
				def processingDatetime = archiveData.OBJECT.find { it.OBJECT_NAME == 'PROCESSINGDATETIME' }
				if (processingDatetime) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
					sdf.timeZone = TimeZone.getTimeZone("UTC")
					header.setCreateTime(sdf.parse(processingDatetime.VALUE as String))
				}
			} catch(Exception e) {
				throw new ServiceProfileException("Invalid metadata: BOUNDINGRECTANGLE | metadata file location: ${metFileLoc}")
			}
		}

		def inventoryData = met.GROUP.find { it.GROUP_NAME == 'INVENTORYMETADATA' }

		if (inventoryData) {
			def rangeDatetime = inventoryData.GROUP.find { it.GROUP_NAME == 'RANGEDATETIME' }
			if (rangeDatetime) {
				try{
					def rangeBeginningDate = rangeDatetime.OBJECT.find { it.OBJECT_NAME == 'RANGEBEGINNINGDATE' }
					def rangeBeginningTime = rangeDatetime.OBJECT.find {it.OBJECT_NAME == 'RANGEBEGINNINGTIME'}
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
					sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
					if (rangeBeginningDate) {
						String btime = (rangeBeginningTime.VALUE as String)
						Date d = sdf.parse("${rangeBeginningDate.VALUE as String} ${btime}")
						//d.clearTime()
						metadata.setProductStartTime(d.time)
						logger.debug("Product start time ${d}")
					}
	
					def rangeEndingDate = rangeDatetime.OBJECT.find { it.OBJECT_NAME == 'RANGEENDINGDATE' }
					def rangeEndingTime = rangeDatetime.OBJECT.find {it.OBJECT_NAME == 'RANGEENDINGTIME'}
					if (rangeEndingDate) {
						String etime = (rangeEndingTime.VALUE as String)
						Date d = sdf.parse("${rangeEndingDate.VALUE as String}  ${etime}") //${(rangeEndingTime.VALUE as String).replaceAll('z$', '+0000')}")
						//d.clearTime()
						metadata.setProductStopTime(d.time)
						logger.debug("Product stop time ${d}")
					}
				} catch (Exception e) {
					throw new ServiceProfileException("Invalid metadata: RANGEDATETIME | metadata file location: ${metFileLoc}")
				}
			}
			def ecsDataGranule = inventoryData.GROUP.find { it.GROUP_NAME == 'ECSDATAGRANULE' }
			if (ecsDataGranule) {
				try {
					def dayNightFlag = ecsDataGranule.OBJECT.find { it.OBJECT_NAME == 'DAYNIGHTFLAG' }
					if (dayNightFlag) {
						metadata.dayNightMode = dayNightFlag.VALUE as String
					}
				} catch(Exception e) {
					throw new ServiceProfileException("Invalid metadata: ECSDATAGRANULE | metadata file location: ${metFileLoc}")
				}
			}

			def inputGranule = inventoryData.GROUP.find { it.GROUP_NAME == 'INPUTGRANULE' }
			if (inputGranule) {
				try {
					def inputPointer = inputGranule.OBJECT.find { it.OBJECT_NAME == 'INPUTPOINTER' }
					if (inputPointer) {
						inputPointer.VALUE.each {
							String g = it as String
							if (!g || g.size() == 0) {
								return
							}
							SPSourceProduct sp = history.createSourceProduct()
							history.addSourceProduct(sp)
	
							String ptn = product.productType.name
							if (ptn.endsWith('_SRC')) {
								ptn = ptn.substring(0, ptn.lastIndexOf('_SRC'))
							}
							sp.productType = ptn
							sp.product = g.trim()
						}
					}
				} catch (Exception e) {
					throw new ServiceProfileException("Invalid metadata: INPUTGRANULE | metadata file location: ${metFileLoc}")
				}
			}

			def additionalAttributes = inventoryData.GROUP.find { it.GROUP_NAME == 'ADDITIONALATTRIBUTES' }
			if (additionalAttributes) {
				try {
					def additionalAttributesContainers = additionalAttributes.OBJECT.findAll { it.OBJECT_NAME == 'ADDITIONALATTRIBUTESCONTAINER' }
	
					if (additionalAttributesContainers) {
						outerloop:
						for (currentContainer in additionalAttributesContainers) {
	
							def additionalAttributeName = currentContainer.OBJECT.find { it.OBJECT_NAME == 'ADDITIONALATTRIBUTENAME'}
	
							if (additionalAttributeName && additionalAttributeName.VALUE == "\"TILEID\"") {
	
								def informationContent = currentContainer.GROUP.find { it.GROUP_NAME == 'INFORMATIONCONTENT' }
	
								if (informationContent) {
									def parameterValue = informationContent.OBJECT.find { it.OBJECT_NAME == 'PARAMETERVALUE' }
	
									if (parameterValue) {
										logger.debug("Setting partialID: " + parameterValue.VALUE)
										metadata.setPartialId(parameterValue.VALUE as String)
									}
								}
							}
							
							else if (additionalAttributeName && additionalAttributeName.VALUE == "\"TILEDATADAY\"") {
								
								def informationContent = currentContainer.GROUP.find { it.GROUP_NAME == 'INFORMATIONCONTENT' }
	
								if (informationContent) {
									def parameterValues = informationContent.OBJECT.findAll{ it.OBJECT_NAME == 'PARAMETERVALUE' }
	
									if (parameterValues) {
										for (currentParamValue in parameterValues) {
											def dataDay = currentParamValue.VALUE as String
											logger.debug("Setting dataDay: " + dataDay)
											metadata.addDataDay(Long.parseLong(dataDay))
										}
									}
								}
							}
						}
					}
				} catch (Exception e) {
					throw new ServiceProfileException("Invalid metadata: ADDITIONALATTRIBUTES | metadata file location: ${metFileLoc}")
				}
			}
		}

		// register product file
		if (imageryFile) {
			SPIngestProductFile imagerypf = ingest.createIngestProductFile()
			ingest.addIngestProductFile(imagerypf)

			SPProductFile pf = imagerypf.createProductFile()
			imagerypf.productFile = pf
			pf.fileType = SPCommon.SPFileClass.IMAGE

			SPFile spf = pf.createFile()
			pf.file = spf
			spf.name = imageryFile.name
			spf.size = imageryFile.size
			spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(imageryFile.digestAlgorithm as String)
			spf.checksumValue = imageryFile.digestValue
			spf.addLink("file://${product.stageLocation}${File.separator}${imageryFile.name}")
			if (imageryFile.name.endsWith('.png'))
				spf.dataFormat = SPCommon.SPDataFormat.PNG
			if (imageryFile.name.endsWith('.jpg'))
				spf.dataFormat = SPCommon.SPDataFormat.JPG
            if (imageryFile.name.endsWith('.tif') || imageryFile.name.endsWith('.tiff'))
                    spf.dataFormat = SPCommon.SPDataFormat.GEOTIFF

			SPFileDestination des = imagerypf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${imageryFile.name}"
			imagerypf.setIngestStartTime(product.ingestStart)
			imagerypf.setIngestStopTime(product.ingestStop)
			imagerypf.fileDestination = des
		}

		// register world file
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
			if (worldFile.name.endsWith('.pgw'))
				spf.dataFormat = SPCommon.SPDataFormat.PGW
			if (worldFile.name.endsWith('.jgw'))
				spf.dataFormat = SPCommon.SPDataFormat.JGW

			SPFileDestination des = worldpf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${worldFile.name}"
			worldpf.setIngestStartTime(product.ingestStart)
			worldpf.setIngestStopTime(product.ingestStop)
			worldpf.fileDestination = des

		}

		// register met file
		if (metFile) {
			SPIngestProductFile metpf = ingest.createIngestProductFile()
			ingest.addIngestProductFile(metpf)

			SPProductFile pf = metpf.createProductFile()
			metpf.productFile = pf
			pf.fileType = SPCommon.SPFileClass.METADATA

			SPFile spf = pf.createFile()
			pf.file = spf
			spf.name = metFile.name
			spf.size = metFile.size
			spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(metFile.digestAlgorithm as String)
			spf.checksumValue = metFile.digestValue
			spf.addLink("file://${product.stageLocation}${File.separator}${metFile.name}")
			spf.dataFormat = SPCommon.SPDataFormat.TEXT

			SPFileDestination des = metpf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${metFile.name}"
			metpf.setIngestStartTime(product.ingestStart)
			metpf.setIngestStopTime(product.ingestStop)
			metpf.fileDestination = des
		}


		// register text file
		if (textFile) {
			SPIngestProductFile textpf = ingest.createIngestProductFile()
			ingest.addIngestProductFile(textpf)

			SPProductFile pf = textpf.createProductFile()
			textpf.productFile = pf
			pf.fileType = SPCommon.SPFileClass.METADATA

			SPFile spf = pf.createFile()
			pf.file = spf
			spf.name = textFile.name
			spf.size = textFile.size
			spf.checksumType = SPCommon.SPChecksumAlgorithm.valueOf(textFile.digestAlgorithm as String)
			spf.checksumValue = textFile.digestValue
			spf.addLink("file://${product.stageLocation}${File.separator}${textFile.name}")
			spf.dataFormat = SPCommon.SPDataFormat.TEXT

			SPFileDestination des = textpf.createFileDestination()
			des.location = "file://${product.stageLocation}${File.separator}${textFile.name}"
			textpf.setIngestStartTime(product.ingestStart)
			textpf.setIngestStopTime(product.ingestStop)
			textpf.fileDestination = des
		}

		ingest.operationSuccess = true

		if (logger.traceEnabled) {
			logger.info(result.toString())
		}
		return result
	}
}
