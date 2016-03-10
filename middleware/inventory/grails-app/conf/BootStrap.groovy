
import gov.nasa.horizon.inventory.ProductType;

import gov.nasa.gibs.inventory.*
import gov.nasa.horizon.inventory.*
import grails.util.Environment

class BootStrap {
   def grailsApplication
   def quartzScheduler

   def init = { servletContext ->
      log.debug "*** BootStrap, Environment.current: ${Environment.current}"
      
      environments {
         development {
            log.debug '*** detected development'
            this.config('inventory.bootstrap.dev.xml')
         }
         test {
            log.debug '*** detected test'
            this.config('inventory.bootstrap.test.xml')
         }
         production {
            log.debug '*** detected production'
            this.config('inventory.bootstrap.pro.xml')
         }
         smap_cal_val {
            log.debug '*** detected SMAP Cal/Val'
            this.config('inventory.bootstrap.smap.xml')
         }
      }
      quartzScheduler.start()
      
   }
   
   def convertUnits = { String orig, String unit, Integer multiplier ->
      Integer converted = null
      
      def regex = /([0-9]+\.*[0-9]*)(${unit})/
      def matcher = (orig =~ regex)
      
      if(matcher.size() > 0) {
         
         def decimal = matcher[0][1] as Float
         decimal = decimal * multiplier
         converted = decimal as Integer
      }
      return converted
   }
   
   def createProjection = { name ->
      Projection projObj = new Projection()
      projObj.name = name
      if(projObj.name.equals("Geographic")) {
         projObj.epsgCode = "EPSG:4326"
         projObj.wg84Bounds = "-180.0000, -90.0000, 180.0000, 90.0000"
         projObj.nativeBounds = "-180.0000, -90.0000, 180.0000, 90.0000"
         projObj.ogc_crs = "OGC:1.3:CRS84"
         projObj.description = "WGS 84 / Latlong"
      }
      else if (projObj.name.equals("Arctic")) {
         projObj.epsgCode = "EPSG:3413"
         projObj.wg84Bounds = "-180.0000, 38.807151, 180.0000, 90.0000"
         projObj.nativeBounds = "-4194304, -4194304, 4194304, 4194304"
         projObj.ogc_crs = "EPSG::3413"
         projObj.description = "WGS 84 / NSIDC Polar Stereographic North"
      }
      else if (projObj.name.equals("Antarctic")) {
         projObj.epsgCode = "EPSG:3031"
         projObj.wg84Bounds = "-180.0000, -90.0000, 180.0000, -38.941373"
         projObj.nativeBounds = "-4194304, -4194304, 4194304, 4194304"
         projObj.ogc_crs = "EPSG::3031"
         projObj.description = "WGS 84 / Antarctic Polar Stereographic"
      }
      else if (projObj.name.equals("Web Mercator")) {
         projObj.epsgCode = "EPSG:3857"
         projObj.wg84Bounds = "-180.0000, -85.0000, 180.0000, 85.0000"
         projObj.nativeBounds = "-20037508.34278925, -20037508.34278925, 20037508.34278925, 20037508.34278925"
         projObj.ogc_crs = "EPSG::3857"
         projObj.description = "WGS 84 / Web Mercator"
      }
	  else if (projObj.name.equals("EASE Geographic")) {
         projObj.epsgCode = "EPSG:3975"
         projObj.wg84Bounds = "-180.0000, -85.0000, 180.0000, 85.0000"
         projObj.nativeBounds = "-17367530.445, -11133956.239, 19529463.696, 7314540.831"
         projObj.ogc_crs = "EPSG::3975"
         projObj.description = "WGS 84 / NSIDC EASE-Grid Global"
      }
      else if (projObj.name.equals("EASE North")) {
         projObj.epsgCode = "EPSG:3973"
         projObj.wg84Bounds = "-135.0000, -135.0000, 135.0000, 135.0000"
         projObj.nativeBounds = "-9216000.000, -9216000.000, 9216000.000, 9216000.000"
         projObj.ogc_crs = "EPSG::3973"
         projObj.description = "WGS 84 / NSIDC EASE-Grid North"
      }
      else {
         projObj.epsgCode = "UNKNOWN"
         projObj.wg84Bounds = "UNKNOWN"
         projObj.nativeBounds = "UNKNOWN"
         projObj.ogc_crs = "UNKNOWN"
         projObj.description = "UNKNOWN"
         if(projObj.name.equals("")) {
            projObj.name = "UNKNOWN"
         }
      }
      return projObj
   }
   
   def config = {bootstrapFile ->

      def filePath = "resources/${bootstrapFile}"
      try {
         def bootstrapXML = new XmlSlurper().parseText(grailsApplication.getParentContext().getResource("classpath:$filePath").inputStream.text)
         bootstrapXML.productType.each { pt ->
         
            def ptObj = ProductType.findByIdentifierAndTitle(pt.identifier as String, pt.title as String)
            if(!ptObj) {
               ptObj = new ProductType()
               ptObj.identifier = pt.identifier
               ptObj.title = pt.title
               ptObj.lastUpdated = new Date().time
               
               // First thing is first, find out what kind of layer this is
               //def type = pt.policy.dataFormat as String

               Provider providerObj
               pt.provider.each { provider ->
                  providerObj = Provider.findByShortName(provider.shortName as String)
                  if(!providerObj) {
                     providerObj = new Provider()
                     providerObj.shortName = provider.shortName
                     providerObj.longName = provider.longName
                     providerObj.type = provider.type
                     
                     if(!providerObj.save(flush:true)) {
                        log.warn "Provider object could not be saved"
                     }
                  }
               }
               if (providerObj) {
                  ptObj.provider = providerObj
               } else {
                  log.warn "ProductType ${ptObj.title} is missing provider."
               }
               if(!ptObj.save()) {
                  log.warn "ProductType object could not be saved in bootstrap"
               }
            
            
               //Set up pt_metadata
               ProductTypeMetadata metadataObj = new ProductTypeMetadata()
               metadataObj.project = (pt.metadata.project == "") ? null : pt.metadata.project
               metadataObj.instrument = (pt.metadata.instrument == "") ? null : pt.metadata.instrument
               metadataObj.platform = (pt.metadata.platform == "") ? null : pt.metadata.platform
               metadataObj.processingLevel = (pt.metadata.processingLevel == "") ? null : pt.metadata.processingLevel
               metadataObj.dataVersion = (pt.metadata.dataVersion == "") ? null : pt.metadata.dataVersion
               metadataObj.regionCoverage = (pt.metadata.regionCoverage == "") ? null : pt.metadata.regionCoverage
               metadataObj.dayNight = (pt.metadata.dayNight == "") ? null : pt.metadata.dayNight
               metadataObj.ascDesc = (pt.metadata.ascDesc == "") ? null : pt.metadata.ascDesc
               metadataObj.scienceParameter = (pt.metadata.scienceParameter == "") ? null : pt.metadata.scienceParameter 
               metadataObj.nativeResolution = convertUnits(pt.metadata.nativeResoution as String, "km", 1000)
               metadataObj.displayResolution = convertUnits(pt.metadata.displayResolution as String, "km", 1000)
               def sourceProjObj = Projection.findByName(pt.metadata.sourceProjection as String)
               if(!sourceProjObj && pt.metadata.sourceProjection != "") {
                  sourceProjObj = createProjection(pt.metadata.sourceProjection as String)
                  if(!sourceProjObj.save(flush:true)) {
                     log.warn "Source Projection object could not be saved"
                  }
               }
               def targetProjObj = Projection.findByName(pt.metadata.targetProjection as String)
               if(!targetProjObj && pt.metadata.targetProjection != "") {
                  targetProjObj = createProjection(pt.metadata.targetProjection as String)
                  
                  if(!targetProjObj.save(flush:true)) {
                     log.warn "Target Projection object could not be saved"
                  }
               }
               metadataObj.sourceProjection = sourceProjObj
               metadataObj.targetProjection = targetProjObj
               metadataObj.pt = ptObj
               if(!metadataObj.save()) {
                  log.warn "PT Metadata object could not be saved"
               }

               ProductTypePolicy policyObj = new ProductTypePolicy()
               policyObj.dataFormat = pt.policy.dataFormat
               policyObj.accessType = pt.policy.accessType
               policyObj.dataClass = pt.policy.dataClass
               policyObj.useConstraint = pt.policy.useConstraint
               policyObj.accessConstraint = pt.policy.accessConstraint
               policyObj.spatialType = pt.policy.spatialType
               policyObj.compressType = pt.policy.compressType
               policyObj.checksumType = pt.policy.checksumType
               policyObj.basePathAppendType = pt.policy.basePathAppendType
               policyObj.pt = ptObj
               if(!policyObj.save()) {
                  log.warn "PT Policy object could not be saved"
               }

               ProductTypeCoverage coverageObj = new ProductTypeCoverage()

               coverageObj.startTime = (pt.coverage.startTime && ! pt.coverage.startTime.equals("")) ? Date.parse("dd/MM/yyyy", pt.coverage.startTime as String).getTime() : null
               coverageObj.stopTime = (pt.coverage.stopTime && ! pt.coverage.stopTime.equals("")) ?Date.parse("dd/MM/yyyy", pt.coverage.stopTime as String).getTime() : null
               coverageObj.northLatitude = (pt.coverage.northLatitude.size() == 0 || pt.coverage.northLatitude.equals("")) ? null : pt.coverage.northLatitude.text() as Float
               coverageObj.southLatitude = (pt.coverage.southLatitude.size() == 0 || pt.coverage.southLatitude.equals("")) ? null : pt.coverage.southLatitude.text() as Float
               coverageObj.westLongitude = (pt.coverage.westLongitude.size() == 0 || pt.coverage.westLongitude.equals("")) ? null : pt.coverage.westLongitude.text() as Float
               coverageObj.eastLongitude = (pt.coverage.eastLongitude.size() == 0 || pt.coverage.eastLongitude.equals("")) ? null : pt.coverage.eastLongitude.text() as Float
               coverageObj.pt = ptObj
               if(!coverageObj.save()) {
                  log.warn "PT Coverage object could not be saved"
               }
               
               pt.locationPolicies.locationPolicy.each { policy ->
                  ProductTypeLocationPolicy locationPolicyObj = new ProductTypeLocationPolicy()
                  locationPolicyObj.type = policy.type
                  locationPolicyObj.basePath = policy.basePath
                  
                  locationPolicyObj.pt = ptObj
                  if(!locationPolicyObj.save()) {
                     log.warn "PT Location Policy object could not be saved"
                  }
               }
               
               pt.resources.resource.each { resource ->
                  ProductTypeResource resourceObj = new ProductTypeResource()
                  resourceObj.name = resource.name
                  resourceObj.path = resource.path
                  resourceObj.type = resource.type
                  resourceObj.description = resource.description
                  resourceObj.pt = ptObj
                  if(!resourceObj.save()) {
                     log.warn "PT Resource object could not be saved"
                  }
               }
               
               //GIBS generation table
               if(pt.generation.size() > 0) {
                  ProductTypeGeneration generationObj = new ProductTypeGeneration()
                  generationObj.pt = ptObj
                  generationObj.outputSizeX = (pt.generation.outputSizeX.size() == 0 || pt.generation.outputSizeX  == '') ? null: pt.generation.outputSizeX.text() as Integer
                  generationObj.outputSizeY = (pt.generation.outputSizeY.size() == 0 || pt.generation.outputSizeY  == '') ? null: pt.generation.outputSizeY.text() as Integer
                  generationObj.overviewScale = (pt.generation.overviewScale.size() == 0 || pt.generation.overviewScale  == "") ? null : pt.generation.overviewScale.text() as Integer
                  generationObj.overviewLevels = (pt.generation.overviewLevels.size() == 0 || pt.generation.overviewLevels  == "") ? null : pt.generation.overviewLevels.text() as Integer
                  generationObj.overviewResample = (pt.generation.overviewResample.size() == 0 || pt.generation.overviewResample  == "") ? null : pt.generation.overviewResample
                  generationObj.resizeResample = (pt.generation.resizeResample.size() == 0 || pt.generation.resizeResample  == "") ? null : pt.generation.resizeResample
                  generationObj.reprojectionResample = (pt.generation.reprojectionResample.size() == 0 || pt.generation.reprojectionResample  == "") ? null : pt.generation.reprojectionResample
                  generationObj.vrtNodata = (pt.generation.vrtNodata.size() == 0 || pt.generation.vrtNodata  == "") ? null: pt.generation.vrtNodata
                  generationObj.mrfBlockSize = (pt.generation.mrfBlockSize.size() == 0 || pt.generation.mrfBlockSize  == "") ? null : pt.generation.mrfBlockSize.text() as Integer
                  
                  if(!generationObj.save()) {
                     log.warn "PT Generation object could not be saved"
                  }
               }
            }
         }
      }
      catch(IOException e) {
         log.error "Unable to process bootstrap file ${filePath}", e
      }

   }
   
   def destroy = {
   }
}
