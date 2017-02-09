package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import groovy.transform.ToString
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Implementation of WMS configurator
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true, excludes = "productTypes")

public class WMSConfigurator extends ApplicationConfigurator {
	private static Log logger = LogFactory.getLog(WMSConfigurator.class)

	// property names
	private static String PROP_CONFIG_FILE = 'tie.config.file'
	private static String PROP_SCRIPT = 'horizon.user.application'
	private String[] args

	private String[] filteredProductTypes
	private String userQueryType = 'acquisition'

	public WMSConfigurator(String[] args) {
		this.args = args
		this.hasError = !this._parseArgs(args)
		logger.debug ("this.hasError = ${this.hasError}")
		if (!this.hasError) {
			this.setup()
		}
	}

	protected boolean _parseArgs(String[] args) {
		boolean result = true
		def cli = new CliBuilder(
				usage: "${System.getProperty(PROP_SCRIPT)} [options]",
				header: 'options:')
		cli.h(longOpt: 'help', 'print this message')
		cli.t(args: 1, longOpt: 'productTypes', argName: 'productTypes', 'comma-separated list of product types to process')
		cli.s(args: 1, longOpt: 'start', argName: 'startDate', 'start date yyyy-MM-dd')
		cli.e(args: 1, longOpt: 'end', argName: 'endDate', 'end date yyyy-MM-dd')
		cli.r(args: 1, longOpt: 'repo', argName: 'repo', 'local repository directory')
		cli.q(args: 1, longOpt: 'queryType', argName: 'queryType', 'revision or acquisition (default)')

        def options = cli.parse(args)
		if (!options) {
			logger.error('Invalid input parameter(s)')
			result = false
		}

		if (options.t) {
			this.filteredProductTypes = options.t.split(",")
		}

		if (options.s) {
			this.userStart = _parseDate(options.s)
			if (!userStart) {
				logger.error("Invalid start date value ${options.s}")
				result = false
			}
		}

		if (options.e) {
			this.userEnd = _parseDate(options.e)
			if (!this.userEnd) {
				logger.error("Invalid end date value ${options.s}")
				result = false
			}
		}

		if (options.r) {
			this.repo = options.r
		}

		if (options.h || !result) {
			cli.usage()
			result = false
		}

		if (options.q) {
			this.userQueryType = options.q
			if( !(this.userQueryType.equals('acquisition') || this.userQueryType.equals('revision')) ){
				logger.error("Invalid queryType value: ${options.q}")
				result = false
			}
		}

		logger.debug("return result ${result}")

		return result
	}

	private static Date _parseDate(String date) {
		Date result = null
		SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd')
		try {
			result = sdf.parse(date)
		} catch (ParseException e) {
			logger.debug("Invalid date value ${date}", e)
		}
		return result
	}

	protected boolean configure() throws DataHandlerException {
		boolean result = false

		logger.debug("Loading product type config from ${System.getProperty(PROP_CONFIG_FILE)}")
		this.productTypes = this.productTypeFactory.createProductTypes(this, System.getProperty(PROP_CONFIG_FILE))

		logger.debug("Done loading product type configuration")

		this.productTypes.values().each { ProductType pt ->
			if(this.filteredProductTypes == null || pt.name in this.filteredProductTypes) {
                logger.debug(pt.name + " passed name filter")
				if (this.userStart) {
					pt.start = userStart
				}
				if (this.userEnd) {
					pt.end = userEnd
					pt.batch = true
					pt.userEnd = true
				}
				if(this.userQueryType) {
					pt.queryType = userQueryType
				}

				pt.ready = true // set flag to enable the product type to harvest data
				
				pt.setup()
                if (logger.debugEnabled) {
                    logger.info("${pt.name} -> ${pt}")
                }
			}

		}
		return result
	}
}
