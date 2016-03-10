import grails.util.Holders

// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    xml:           ['text/xml', 'application/xml']
]

grails.gorm.failOnError = true

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

//Security Service

gov.nasa.horizon.security.host="https://localhost"
gov.nasa.horizon.security.port=9197
gov.nasa.horizon.security.realm="PODAAC-INVENTORY"
//no role information needed. Authentication is enough.

//If you want to enable roles management, uncomment the line
//below and fill in the ROLES that are authorized to update the inventory system
//gov.nasa.podaac.security.roles=['DE','ADMIN','WRITE_ALL']

//cache user logins for faster access. Recomended to keep this as true,
//other wise every request needing auth will query the security service.
gov.nasa.podaac.auth.cache.useCache = true;

//hours to cache a user
//(once successfully cached, requests won't ping the security server until the time limit is up). 0 = infinite cache.
gov.nasa.podaac.auth.cache.timeLimit = 2

gov.nasa.horizon.log.root = ""


environments {
    development {
        grails.logging.jul.usebridge = true
        
        gov.nasa.horizon.sigevent.url = "http://localhost.jpl.nasa.gov:8100/sigevent"
        gov.nasa.horizon.security.host = "https://localhost"
        gov.nasa.horizon.security.port = 9197
        gov.nasa.horizon.security.realm = "HORIZON-MANAGER"
        gov.nasa.horizon.log.root = System.getProperty('user.dir')
    }
    production {
        grails.logging.jul.usebridge = false
        
        gov.nasa.horizon.sigevent.url = "http://localhost.jpl.nasa.gov:8100/sigevent"
        gov.nasa.horizon.security.host = "https://localhost"
        gov.nasa.horizon.security.port = 9197
        gov.nasa.horizon.security.realm = "HORIZON-MANAGER"
        gov.nasa.horizon.log.root = System.getProperty('user.dir')
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
    smap_cal_val {
        grails.logging.jul.usebridge = true

        gov.nasa.horizon.sigevent.url = "http://tie-2-aws.jpl.nasa.gov:8100/sigevent"
        gov.nasa.horizon.security.host = "https://tie-2-aws.jpl.nasa.gov"
        gov.nasa.horizon.security.port = 9197
        gov.nasa.horizon.security.realm = "HORIZON-MANAGER"
        gov.nasa.horizon.log.root = "/data/tie/logs/inventory/"
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
//    appenders {
//        console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
//    }
    
   appenders {
      console name: 'stdoutLogger',
            layout: pattern(
                  conversionPattern: '%d{ABSOLUTE} %-5p [%c{1}:%L] {%t} %m%n')

      appender new org.apache.log4j.DailyRollingFileAppender(
            name: 'fileLogger',
            file: "${Holders.config.gov.nasa.horizon.log.root}/inventory.log",
            layout: pattern(
                  conversionPattern: '%d %-5p [%c{1}:%L] {%t} %m%n'),
            datePattern: "'.'yyyy-MM-dd"
      )

      appender new org.apache.log4j.DailyRollingFileAppender(
            name: 'stackTraceLogger',
            fileName: "${Holders.config.gov.nasa.horizon.log.root}/inventory.stacktrace",
            layout: pattern(
                  conversionPattern: '%d %-5p [%c{1}:%L] {%t} %m%n'),
            datePattern: "'.'yyyy-MM-dd"
      )
   }

   root {
      error 'stdoutLogger', 'fileLogger'
      warn 'stdoutLogger', 'fileLogger'
      info 'stdoutLogger', 'fileLogger'
      debug 'stdoutLogger', 'fileLogger'
      
      additivity = true
   }

   error stackTraceLogger: "StackTrace"

   debug 'grails.app',
         'org.quartz',
         'gov.nasa.horizon.inventory.api.InventoryApi',
         'gov.nasa.horizon.sigevent.api'
         'gov.nasa.horizon.security.client'
   
    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate',
           'org.apache.tomcat.util.digester',
           'org.apache.tomcat.util',
           'org.apache.catalina.util',
           'org.apache',
           'org.grails.plugin.resource',
           'org.codehaus.groovy.grails.context.support',
           'com.mchange.v2'
           
}

pagination {
   PRODUCT_PAGE_SIZE = 10000
   PRODUCT_REF_PAGE_SIZE = 10000
}

constants {
   domainLabel {
      PRODUCT_TYPE = "ImagerySet"
      PRODUCT = "Imagery"
   }
   appendType {
      YEAR_DOY = "YEAR-DOY"
      YEAR_MONTH_DAY = "YEAR-MONTH-DAY"
      YEAR = "YEAR"
      YEAR_FLAT = "YEAR-FLAT"
      BATCH = "BATCH"
      YEAR_WEEK = "YEAR-WEEK"
      YEAR_MONTH = "YEAR-MONTH"
      CYCLE = "CYCLE"
   }
   fileClass {
      DATA = "DATA"
      METADATA = "METADATA" 
      CHECKSUM = "CHECKSUM"
      THUMBNAIL = "THUMBNAIL"
      IMAGE = "IMAGE"
   }
   locationPolicy {
      ARCHIVE = "ARCHIVE-OPEN"
      LOCAL_FTP = "LOCAL-FTP"
      LOCAL_HTTP = "LOCAL-HTTP"
      LOCAL_LINK = "LOCAL-LINK"
      REMOTE_FTP = "REMOTE-FTP"
      REMOTE_HTTP = "REMOTE-HTTP"
   }
   productStatus {
      ONLINE = "ONLINE"
      OFFLINE = "OFFLINE"
   }
}
