package gov.nasa.gibs.tie.handlers.sips.pdr;

import gov.nasa.horizon.common.api.file.FileProduct
import gov.nasa.horizon.common.api.util.FileProductUtility

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.text.SimpleDateFormat

public final class PDRErrorUtility {
	private static Log logger = LogFactory.getLog(PDRProcessor.class)

	//private String pdrFile
	//private String hostURI

	private PDRErrorUtility() {
		
	}
	
	public enum PDRDShortDisposition {
		UNREADABLE('INVALID PVL STATEMENT'),
		FILE_COUNT('INVALID FILE COUNT')

		private final String _value;

		PDRDShortDisposition(String value) {
			_value = value;
		}

		public String toString() {
			return _value;
		}
	}

	public enum PDRDLongDisposition {
		SUCCESS('SUCCESSFUL'),
		DATA_TYPE('INVALID DATA TYPE'),
		DIRECTORY('INVALID DIRECTORY'),
		FILE_SIZE('INVALID FILE SIZE'),
		FILE_ID('INVALID FILE ID'),
		FILE_TYPE('INVALID FILE TYPE'),
		MISSING_CKSUM_TYPE('MISSING FILE_CKSUM_TYPE PARAMETER'),
		UNSUPPORTED_CKSUM_TYPE('UNSUPPORTED CHECKSUM TYPE'),
		MISSING_CKSUM_VALUE('MISSING FILE_CKSUM_VALUE PARAMETER'),
		INVALID_CKSUM_VALUE('INVALID FILE_CKSUM_VALUE')

		private final String _value;

		PDRDLongDisposition(String value) {
			_value = value;
		}

		public String toString() {
			return _value;
		}
	}

	public enum PANLongDisposition {
		SUCCESS('SUCCESSFUL'),
		FILE_NOT_FOUND('ALL FILE GROUPS/FILES NOT FOUND'),
		FILE_SIZE('POST-TRANSFER FILE SIZE CHECK FAILURE'),
		CONNECTION_FAIL('UNABLE TO ESTABLISH FTP/KFTP CONNECTION'),
		DOWNLOAD_FAIL('FTP/KFTP FAILURE'),
		CKSUM_FAIL('CHECKSUM VERIFICATION FAILURE'),
		FILE_IO('FILE I/O ERROR'),
		MISSING_IMG_FILE('INCORRECT NUMBER OF SCIENCE FILES'), // Img handler
		MISSING_WORLD_FILE('INCORRECT NUMBER OF FILES'), // Img handler
		MISSING_META_FILE('INCORRECT NUMBER OF METADATA FILES'), // Img handler
		META_PARSE_FAIL('DATA CONVERSION FAILURE'), // Img handler
		SIP_FAIL('ECS INTERNAL ERROR'), // Img handler
		UNCAUGHT_ERROR('ECS INTERNAL ERROR')
		
		private final String _value;
		
		PANLongDisposition(String value) {
			_value = value;
		}
		
		public String toString() {
			return _value;
		}
	}
	
	public static class ProductJob {
		PDRProductType sipsProductType
		String pdrProductType
		String productType
		String productName
		String productVersion
		String directory
		String fileName
		String fileURL
		long fileSize
		String fileType
		//int crc32
		String checksumType
		String checksum
  
		Boolean valid
		Boolean success
		String dispositionPDRD
		String dispositionPAN
	 }
	
	public static void writePDRDShortFile(PDRProductType pt, String baseFileName, PDRDShortDisposition disposition) {
		// log a PDRD file
		if (!pt.panURL) {
			logger.warn("PDRD URL is not specified.  A PDRD file will not be logged.  Please inform the data provider.")
			return
		}
		FileProductUtility pdrd = new FileProductUtility("${pt.panURL}${File.separator}${baseFileName}.PDRD")

		if (pt.user) {
			pdrd.setAuthentication(pt.user, pt.pass)
		}
		StringBuffer sb = new StringBuffer()
		sb.append("MESSAGE TYPE = SHORTPDRD;\n")
		sb.append("DISPOSITION = ${disposition};\n")

		logger.debug("Writing to remote PDRD file...")
		logger.debug(sb.toString())

		try {
			pdrd.writeFile(new ByteArrayInputStream(sb.toString().bytes))
		} catch (Exception e) {
			logger.error("Unable to write PDRD to to the specified destination: ${pt.panURL}.  A PDRD file will not be logged.  Please inform the data provider.")
		} finally {
			pdrd.cleanUp()
		}
		logger.debug("Done handling remote PDRD file.")
	}
	
	public static void writeLongPdrdOrPanFile(PDRProductType pt, def jobs) {
		// log a Long PDRD or PAN file
		boolean validPDR = true

		if (!pt.panURL) {
			logger.warn("PDRD URL is not specified.  A PDRD file will not be logged.  Please inform the data provider.")
			return
		}

		StringBuffer bufferPDRD = new StringBuffer()
		StringBuffer bufferPAN = new StringBuffer()
		bufferPDRD.append('MESSAGE_TYPE = LONGPDRD;\n')
		bufferPDRD.append("NO_FILE_GRPS = ${jobs.size()};\n")

		bufferPAN.append('MESSAGE_TYPE = LONGPAN;\n')
		bufferPAN.append("NO_OF_FILES = ${jobs.size()};\n")

		jobs.each { ProductJob job ->
			bufferPDRD.append("DATA_TYPE = ${job.productType};\n")
			bufferPDRD.append("DISPOSITION = ${job.dispositionPDRD};\n")

			if(job.valid) {
				bufferPAN.append("FILE_DIRECTORY = ${job.directory};\n")
				bufferPAN.append("FILE_NAME = ${job.fileName};\n")
				bufferPAN.append("DISPOSITION = ${job.dispositionPAN};\n")
				Date curDate = new Date()
				SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-DD'T'hhmm:ssZ")
				bufferPAN.append("TIME_STAMP = ${format.format(curDate)};\n")
			} else {
				validPDR = false
				logger.debug("PDR for productType ${job.pdrProductType} failed validation. No PAN file will be generated.")
			}
		}
		if(validPDR) { //write a PAN file
			logger.debug("Writing to remote PAN file...")
			logger.debug(bufferPAN.toString())
			FileProductUtility pan = new FileProductUtility("${pt.panURL}${File.separator}${jobs[0].pdrProductType}.PAN")

			if (pt.user) {
				pan.setAuthentication(pt.user, pt.pass)
			}
			try {

				pan.writeFile(new ByteArrayInputStream(bufferPAN.toString().bytes))
			} catch (Exception e) {
				logger.error("Unable to write PAN to to the specified destination: ${pt.panURL}.  A PAN file will not be logged.  Please inform the data provider.")
			} finally {
				pan.cleanUp()
			}
			logger.debug("Done handling remote PAN file.")
		} else { //write a PDRD file
			logger.debug("Writing to remote PDRD file...")
			logger.debug(bufferPDRD.toString())

			FileProductUtility pdrd = new FileProductUtility("${pt.panURL}${File.separator}${jobs[0].pdrProductType}.PDRD")

			if (pt.user) {
				pdrd.setAuthentication(pt.user, pt.pass)
			}
			try {
				pdrd.writeFile(new ByteArrayInputStream(bufferPDRD.toString().bytes))
			} catch (Exception e) {
				logger.error("Unable to write PDRD to to the specified destination: ${pt.panURL}.  A PDRD file will not be logged.  Please inform the data provider.")
			} finally {
				pdrd.cleanUp()
			}
			logger.debug("Done handling remote PDRD file.")
		}
	}
}
