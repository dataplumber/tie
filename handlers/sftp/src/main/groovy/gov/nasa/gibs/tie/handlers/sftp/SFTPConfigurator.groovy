package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.ProductType
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Implementation of gov.nasa.gibs.tie.handlers.sftp.SFTP configurator
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true, excludes = "productTypes")

public class SFTPConfigurator extends ApplicationConfigurator {

    private static Log logger = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPConfigurator.class)

    // property names
    private static String PROP_CONFIG_FILE = 'tie.config.file'
    private static String PROP_SCRIPT = 'horizon.user.application'
    private String[] args
    private String[] filteredProductTypes

    public SFTPConfigurator(String[] args) {
        this.args = args
        this.hasError = !this._parseArgs(args)
        logger.debug("this.hasError = ${this.hasError}")
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
        cli.t(args: 1, longOpt: 'productType', argName: 'productType', 'target product type')
        cli.s(args: 1, longOpt: 'start', argName: 'startDate', 'start date yyyy-MM-dd')
        cli.e(args: 1, longOpt: 'end', argName: 'endDate', 'end date yyyy-MM-dd')
        cli.r(args: 1, longOpt: 'repo', argName: 'repo', 'local repository directory')
        cli.U(args: 1, longOpt: 'user', argName: 'user', 'URS user name')
        cli.P(args: 1, longOpt: 'pass', argName: 'pass', 'URS password')
        cli.n(longOpt: 'non-interactive', argName: 'non-interactive', 'Run in non-interactive mode')
        cli.p(args: 1, longOpt: 'productTypes', argName: 'productType', 'list of product types to process')

        def options = cli.parse(args)
        if (!options) {
            logger.error('Invalid input parameter(s)')
            result = false
        }

        if (options.t) {
            this.userProductType = options.t
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

        if (options.U) {
            this.user = options.U
        }

        if (options.P) {
            this.pass = options.P
        }

        if (options.h || !result) {
            cli.usage()
            result = false
        }


        if (!options.h) {
            if (!options.n) {
                // gov.nasa.gibs.tie.handlers.sftp.SFTP requires URS login
                if (!options.u || !options.p) {
                    Console con = System.console()
                    System.out.print("URS Username: ")
                    this.user = con.readLine()
                    System.out.print("URS Password: ")
                    this.pass = new String(con.readPassword())
                }
            }
        }

        if (options.p) {
            this.filteredProductTypes = options.p.split(",")
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
                    logger.debug("set user start date: ${pt.start}")
                }
                if (this.userEnd) {
                    pt.end = userEnd
                    logger.debug("set user end date: ${pt.end}")
                    pt.batch = true
                }
                // set flag to enable the product type to harvest data
                if (!this.userProductType || this.userProductType.equals(pt.name)) {
                    pt.ready = true
                }
                pt.setup()
                if (logger.debugEnabled) {
                    logger.info("${pt.name} -> ${pt}")
                }
            }
        }

        return result
    }
}
