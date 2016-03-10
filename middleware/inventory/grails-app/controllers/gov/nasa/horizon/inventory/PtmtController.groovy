package gov.nasa.horizon.inventory

import java.net.URLDecoder;
import java.text.SimpleDateFormat
import java.util.Formatter.DateTime

import groovy.xml.MarkupBuilder
import grails.converters.XML;
import grails.converters.JSON;
import gov.nasa.gibs.inventory.*
import groovy.json.*

/*import gov.nasa.podaac.common.api.metadatamanifest.MetadataManifest
 import gov.nasa.podaac.common.api.metadatamanifest.Constant.ActionType
 import gov.nasa.podaac.common.api.metadatamanifest.Constant.ObjectType
 */

class PtmtController {
   static scaffold = false;

   def inventoryService;
   def grailsApplication;

   /* BEGIN: PTMT DEVELOPMENT */
   
   def ptmtService;
   
   // SUBMIT OBJECT TO SPECIFIED DOMAIN
   def submit = {
       // NOTE: params.json is NOT from the URL endpoint; it's POST data with "json" as the key
       if(params.json) {
           // use Groovy's built-in JSON converter
           def jsonObj = null
           try {
               jsonObj = JSON.parse(params.json)
           }
           catch(Exception e) {
               response.status=500
               render "Error parsing submitted JSON"
               throw e
           }
           
           // for converting DateTime picker values to epoch time
           SimpleDateFormat dtFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
           log.info("hmmm")
           // submit objects differently based on form type
           if(jsonObj != null && jsonObj.formType) {
               
               render "You are submitting a form of type '" + jsonObj.formType + "'\n"
               
               switch(jsonObj.formType) {
                   case "productType":
                   
                       // if existing product type, reference it by id, makes changes, and save it
                       // otherwise, make a new one
                       def pt

                       if(!jsonObj.isNull("id")) {
                           pt = ProductType.get(jsonObj.id)
                       }
                       else {
                           pt = ProductType.findByIdentifier(jsonObj.productType.identifier)
                           if(pt) {
                               response.status = 404
                               render "Product Type with identfier "+jsonObj.productType.identifier+" already exists"
                           }
                               
                       }
                       if(!pt) {
                           pt = new ProductType()
                       }
                       log.info(jsonObj)
                       log.info("LOOK HERE")
                       log.info(jsonObj.productType)
                       log.info(jsonObj.productType.identifier)
                       
                       pt.identifier = jsonObj.productType.identifier
                       pt.title = jsonObj.productType.title
                       pt.purgable = jsonObj.productType.purgable
                       pt.purgeRate = (jsonObj.productType.purgeRate) ? Integer.valueOf(jsonObj.productType.purgeRate as String): null
                       pt.description = jsonObj.productType.description
                       pt.provider = (jsonObj.productType.providerId ? Provider.get(jsonObj.productType.providerId) : null)
                       pt.lastUpdated = new Date().getTime()
                       pt.save(flush:true)
                       // policy
                       def policy = ProductTypePolicy.findByPt(pt)
                       if(!policy) {
                           policy = new ProductTypePolicy()
                       }
                       policy.pt = pt
                       policy.dataClass = jsonObj.productTypePolicy.dataClass
                       policy.dataFrequency = (jsonObj.productTypePolicy.dataFrequency) ? jsonObj.productTypePolicy.dataFrequency as Integer : null
                       policy.dataVolume = (jsonObj.productTypePolicy.dataVolume) ? jsonObj.productTypePolicy.dataVolume as Integer : null
                       policy.dataDuration = (jsonObj.productTypePolicy.dataDuration) ? jsonObj.productTypePolicy.dataDuration as Integer : null
                       policy.dataLatency = (jsonObj.productTypePolicy.dataLatency) ? jsonObj.productTypePolicy.dataLatency as Integer : null
                       policy.deliveryRate = (jsonObj.productTypePolicy.deliveryRate) ? jsonObj.productTypePolicy.deliveryRate as Integer : null
                       policy.multiDay = (jsonObj.productTypePolicy.multiDay) ? jsonObj.productTypePolicy.multiDay as Integer : null
                       policy.multiDayLink = jsonObj.productTypePolicy.multiDayLink
                       policy.accessType = jsonObj.productTypePolicy.accessType
                       policy.basePathAppendType = jsonObj.productTypePolicy.basePathAppendType
                       policy.dataFormat = jsonObj.productTypePolicy.dataFormat
                       policy.compressType = jsonObj.productTypePolicy.compressType
                       policy.checksumType = jsonObj.productTypePolicy.checksumType
                       policy.spatialType = jsonObj.productTypePolicy.spatialType
                       policy.accessConstraint = jsonObj.productTypePolicy.accessConstraint
                       policy.useConstraint = jsonObj.productTypePolicy.useConstraint
                       policy.save(flush:true)
                       
                       // location policy
                       def oldLocationPolicies = ProductTypeLocationPolicy.findAllByPt(pt);
                       def newLocationPolicies = [];
                       for(lp in jsonObj.locationPolicy) {
                           def locationPolicy;
                           if(lp.id) {
                               locationPolicy = ProductTypeLocationPolicy.get(lp.id)
                           }
                           if(!locationPolicy) {
                               locationPolicy = new ProductTypeLocationPolicy()
                           }
                           locationPolicy.pt = pt
                           locationPolicy.basePath = lp.basePath
                           locationPolicy.type = lp.type
                           locationPolicy.save(flush:true)
                           newLocationPolicies.add(locationPolicy);
                       }
                       def removedLocationPolicies = oldLocationPolicies - newLocationPolicies;
                       removedLocationPolicies.collect { x -> x.delete() };
                       
                       render removedLocationPolicies
                       
                       // resource
                       def oldResources = ProductTypeResource.findAllByPt(pt);
                       def newResources = [];
                       for(r in jsonObj.productTypeResource) {
                           def resource
                           if(r.id && !jsonObj.isNull("id")) {
                               resource = ProductTypeResource.get(r.id)
                           }
                           if(!resource) {
                               resource = new ProductTypeResource()
                           }
                           resource.pt = pt
                           resource.name = r.name
                           resource.path = r.path
                           resource.type = r.type
                           resource.description = r.description
                           resource.save(flush:true)
                           newResources.add(resource);
                       }
                       def removedResources = oldResources - newResources;
                       removedResources.collect { x -> x.delete() };
                       
                       // metadata
                       def metadata = ProductTypeMetadata.findByPt(pt)
                       if(!metadata) {
                           metadata = new ProductTypeMetadata()
                       }
                       metadata.pt = pt
                       metadata.project = jsonObj.productTypeMetadata.project
                       metadata.instrument = jsonObj.productTypeMetadata.instrument
                       metadata.platform = jsonObj.productTypeMetadata.platform
                       metadata.processingLevel = jsonObj.productTypeMetadata.processingLevel
                       metadata.dataVersion = jsonObj.productTypeMetadata.dataVersion
                       metadata.regionCoverage = jsonObj.productTypeMetadata.regionCoverage
                       metadata.dayNight = jsonObj.productTypeMetadata.dayNight
                       metadata.ascDesc = jsonObj.productTypeMetadata.ascDesc
                       metadata.scienceParameter = jsonObj.productTypeMetadata.scienceParameter
                       metadata.nativeResolution = (jsonObj.productTypeMetadata.nativeResolution) ? jsonObj.productTypeMetadata.nativeResolution : null
                       metadata.displayResolution = (jsonObj.productTypeMetadata.displayResolution) ? jsonObj.productTypeMetadata.displayResolution : null
                       metadata.sourceProjection = (jsonObj.productTypeMetadata.sourceProjectionId ? Projection.get(jsonObj.productTypeMetadata.sourceProjectionId) : null)
                       metadata.targetProjection = (jsonObj.productTypeMetadata.targetProjectionId ? Projection.get(jsonObj.productTypeMetadata.targetProjectionId) : null)
                       metadata.save(flush:true)
                       
                       // element
                       def oldElements = ProductTypeElement.findAllByPt(pt);
                       def newElements = [];
                       for(e in jsonObj.productTypeElement) {
                           def element
                           if(e.id) {
                               element = ProductTypeElement.get(e.id)
                           }
                           if(!element) {
                               element = new ProductTypeElement()
                           }
                           element.pt = pt
                           element.element = ElementDd.get(e.elementId)
                           element.obligationFlag = e.obligationFlag
                           element.scope = e.scope
                           element.save(flush:true)
                           newElements.add(element);
                           switch(e.type) {
                               case "Character":
                                   ProductTypeCharacter ptc = new ProductTypeCharacter()
                                   ptc.value = e.valueCharacter
                                   ptc.pt = pt
                                   ptc.pte = element
                                   ptc.save(flush:true)
                                   break
                               case "Datetime":
                                   ProductTypeDatetime ptd = new ProductTypeDatetime()
                                   Date valueAsDate = (e.valueDatetime ? dtFormat.parse(e.valueDatetime) : null);
                                   ptd.valueLong = (valueAsDate ? valueAsDate.getTime() : null);
                                   ptd.pt = pt
                                   ptd.pte = element
                                   ptd.save(flush:true)
                                   break
                               case "Integer":
                                   ProductTypeInteger pti = new ProductTypeInteger()
                                   pti.value = e.valueInteger
                                   pti.units = e.unitsInteger
                                   pti.pt = pt
                                   pti.pte = element
                                   pti.save(flush:true)
                                   break
                               case "Real":
                                   ProductTypeReal ptr = new ProductTypeReal()
                                   ptr.value = e.valueReal
                                   ptr.units = e.unitsReal
                                   ptr.pt = pt
                                   ptr.pte = element
                                   ptr.save(flush:true)
                                   break
                           }
                       }
                       def removedElements = oldElements - newElements;
                       // remove all product type values (characters, reals, ints, dates, etc.) of these product type element joins
                       // not sure if this code below works, so uncomment it for now
                       /*
                       for(ProductTypeElement x : removedElements) {
                           (ProductTypeCharacter.findByPtAndPte(pt,x) ? ProductTypeCharacter.findByPtAndPte(pt,x).delete() : null);
                           (ProductTypeDatetime.findByPtAndPte(pt,x) ? ProductTypeDatetime.findByPtAndPte(pt,x).delete() : null);
                           (ProductTypeInteger.findByPtAndPte(pt,x) ? ProductTypeInteger.findByPtAndPte(pt,x).delete() : null);
                           (ProductTypeReal.findByPtAndPte(pt,x) ? ProductTypeReal.findByPtAndPte(pt,x).delete() : null);
                       }
                       */
                       removedElements.collect { x -> x.delete() };
                       
                       // coverage
                       def coverage = ProductTypeCoverage.findByPt(pt)
                       if(!coverage) {
                           coverage = new ProductTypeCoverage()
                       }
                       coverage.pt = pt
                       // latitude/longitude values must be protected, since they're expected as floats
                       coverage.northLatitude = (jsonObj.productTypeCoverage.northLatitude ? jsonObj.productTypeCoverage.northLatitude : null)
                       coverage.southLatitude = (jsonObj.productTypeCoverage.southLatitude ? jsonObj.productTypeCoverage.southLatitude : null)
                       coverage.eastLongitude = (jsonObj.productTypeCoverage.eastLongitude ? jsonObj.productTypeCoverage.eastLongitude : null)
                       coverage.westLongitude = (jsonObj.productTypeCoverage.westLongitude ? jsonObj.productTypeCoverage.westLongitude : null)
                       Date start = (jsonObj.productTypeCoverage.startTime ? dtFormat.parse(jsonObj.productTypeCoverage.startTime) : null);
                       Date stop = (jsonObj.productTypeCoverage.stopTime ? dtFormat.parse(jsonObj.productTypeCoverage.stopTime) : null);
                       coverage.startTime = (start ? start.getTime() : null);
                       coverage.stopTime = (stop ? stop.getTime() : null);
                       coverage.save(flush:true)
                       
                       // generation
                       if(jsonObj.productTypeGeneration) {
                           def generation = ProductTypeGeneration.findByPt(pt)
                           if(!generation) {
                               generation = new ProductTypeGeneration()
                           }
                           generation.pt = pt
                           generation.outputSizeX = (jsonObj.productTypeGeneration.outputSizeX) ? jsonObj.productTypeGeneration.outputSizeX as Integer: None
                           generation.outputSizeY = jsonObj.productTypeGeneration.outputSizeY
                           generation.overviewScale = jsonObj.productTypeGeneration.overviewScale
                           generation.overviewLevels = jsonObj.productTypeGeneration.overviewLevels
                           generation.overviewResample = jsonObj.productTypeGeneration.overviewResample
                           generation.resizeResample = jsonObj.productTypeGeneration.resizeResample
                           generation.reprojectionResample = jsonObj.productTypeGeneration.reprojectionResample
                           generation.vrtNodata = jsonObj.productTypeGeneration.vrtNodata
                           generation.mrfBlockSize = jsonObj.productTypeGeneration.mrfBlockSize
                           generation.save(flush:true)
                           
                       }
                       // collection
                       def oldCollections = CollectionImagerySet.findAllByPt(pt);
                       def newCollections = [];
                       for(c in jsonObj.collection) {
                           def collection
                           if(c.id) {
                               collection = CollectionImagerySet.get(c.id)
                           }
                           if(!collection) {
                               collection = new CollectionImagerySet()
                           }
                           collection.pt = pt
                           collection.collection = Collection.get(c.collectionId)
                           collection.save(flush:true)
                       }
                       def removedCollections = oldCollections - newCollections;
                       // remove all product type values (characters, reals, ints, dates, etc.) of these product type element joins
                       removedCollections.collect { x -> x.delete() };
                       
                       // dataset
                       if (jsonObj.dataset) {
                           def dataset = DatasetImagery.findByPt(pt)
                           if(!dataset) {
                               dataset = new DatasetImagery()
                           }
                           dataset.pt = pt
                           dataset.dataset = Dataset.get(jsonObj.dataset.datasetId)
                           dataset.save(flush:true)
                       }
                       
                       break
                   case "collection":
                       def collection;
                       if(!jsonObj.isNull("id")) {
                           collection = Collection.get(jsonObj.id)
                       }
                       else {
                           collection = new Collection()
                       }
                       collection.shortName = jsonObj.shortName
                       collection.longName = jsonObj.longName
                       collection.aggregate = jsonObj.aggregate
                       collection.description = jsonObj.description
                       collection.fullDescription = jsonObj.fullDescription
                       collection.save(flush:true)
                       newCollections.add(collection);
                       break
                   case "dataset":
                       def dataset;
                       if(!jsonObj.isNull("id")) {
                           dataset = Dataset.get(jsonObj.id)
                       }
                       else {
                           dataset = new Dataset()
                       }
                       dataset.remoteDatasetId = jsonObj.remoteDatasetId
                       dataset.shortName = jsonObj.shortName
                       dataset.longName = jsonObj.longName
                       dataset.versionLabel = jsonObj.versionLabel
                       dataset.metadataRegistry = jsonObj.metadataRegistry
                       dataset.metadataEndpoint = jsonObj.metadataEndpoint
                       dataset.description = jsonObj.description
                       dataset.provider = (jsonObj.providerId ? Provider.get(jsonObj.providerId) : null)
                       dataset.save(flush:true)
                       break
                   case "element":
                       def element;
                       if(!jsonObj.isNull("id")) {
                           element = ElementDd.get(jsonObj.id)
                       }
                       else {
                           element = new ElementDd()
                       }
                       element.shortName = jsonObj.shortName
                       element.longName = jsonObj.longName
                       element.type = jsonObj.type
                       element.description = jsonObj.description
                       element.maxLength = jsonObj.maxLength
                       element.scope = jsonObj.scope
                       element.save(flush:true)
                       break
                   case "ogc":
                       break
                   case "projection":
                       def projection;
                       if(!jsonObj.isNull("id")) {
                           projection = Projection.get(jsonObj.id)
                       }
                       else {
                           projection = new Projection()
                       }
                       projection.name = jsonObj.name
                       projection.epsgCode = jsonObj.epsgCode
                       projection.wg84Bounds = jsonObj.wg84Bounds
                       projection.nativeBounds = jsonObj.nativeBounds
                       projection.ogc_crs = jsonObj.ogc_crs
                       projection.description = jsonObj.description
                       projection.save(flush:true)
                       break
                   case "provider":
                       def provider;
                       if(!jsonObj.isNull("id")) {
                           provider = Provider.get(jsonObj.id)
                       }
                       else {
                           provider = new Provider()
                       }
                       provider.shortName = jsonObj.shortName
                       provider.longName = jsonObj.longName
                       provider.type = jsonObj.type
                       provider.save(flush:true)
                       break
                   default:
                       break
               }
           }
       }
       else {
           render "No JSON data submitted via POST."
       }
   }
   
   // SHOW FORM SCHEMA OF SPECIFIED DOMAIN
   def getSchema = {
       render ptmtService.generateSchema(params);
   }
   
   // LISTS ALL OBJECTS IN SPECIFIED DOMAIN AS JSON
   def getList = {
       render ptmtService.generateList(params) as JSON;
   }
   
   // SHOW SPECIFIED PRODUCT TYPE (BY ID OR NAME) AS JSON OBJECT
   def show = {
       
       switch(params.formName) {
           case 'productType':
               ProductType pt = ptmtService.getProductType(params);
               if(pt == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderProductType(pt)
               return;
               break
           case 'provider':
               Provider provider = ptmtService.getProvider(params);
               if(provider == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderProvider(provider)
               return;
               break
           case 'projection':
               Projection projection = ptmtService.getProjection(params);
               if(projection == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderProjection(projection)
               return;
               break
           case 'element':
               ElementDd element = ptmtService.getElement(params);
               if(element == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderElement(element)
               return;
               break
           case 'collection':
               Collection collection = ptmtService.getCollection(params);
               if(collection == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderCollection(collection)
               return;
               break
           case 'ogc':
               render "No data found or no identifier (id or name) specified."
               break
           case 'dataset':
               Dataset dataset = ptmtService.getDataset(params);
               if(dataset == null){
                  log.debug("404, No data found or no identifier (id or name) specified.")
                  response.status = 404 //Not Found
                  render "No data found or no identifier (id or name) specified."
                  return
               }
               renderDataset(dataset)
               return;
               break
           default:
               render "No data found or no identifier (id or name) specified."
               break
       }
       
      
   }

   // given a product type and product type element, determine type of the element
   def elementType(pt, pte) {
       if(ProductTypeCharacter.findByPtAndPte(pt,pte)) {
           return "Character";
       }
       else if(ProductTypeDatetime.findByPtAndPte(pt,pte)) {
           return "Datetime";
       }
       else if(ProductTypeInteger.findByPtAndPte(pt,pte)) {
           return "Integer";
       }
       else if(ProductTypeReal.findByPtAndPte(pt,pte)) {
           return "Real";
       }
       else {
           return null;
       }
   };
   
   // RENDER SPECIFIED PRODUCT TYPE AS JSON OBJECT
   def renderProductType = { ProductType pt ->
      //Replace producttype/product labels according to config.groovy
      def productTypeLabel = grailsApplication.config.constants.domainLabel.PRODUCT_TYPE;
      def productLabel = grailsApplication.config.constants.domainLabel.PRODUCT;
      def policyLabel = "policy";
      
      def ptPolicy = ProductTypePolicy.findByPt(pt);
      def ptMetadata = ProductTypeMetadata.findByPt(pt);
      def ptCoverage = ProductTypeCoverage.findByPt(pt);
      def ptElements = ProductTypeElement.findAllByPt(pt);
      def ptGeneration = ProductTypeGeneration.findByPt(pt);
      def ptCollection = CollectionImagerySet.findAllByPt(pt);
      def ptDataset = DatasetImagery.findByPt(pt);
      log.info(pt.locationPolicies)
      def locationPolicyList = pt.locationPolicies.collect { lp -> [
          "id": lp.id,
          "basePath": lp.basePath,
          "type": lp.type
      ]};
      
      def productTypeResourceList = pt.resources.collect { r -> [
          "id": r.id,
          "name": r.name,
          "path": r.path,
          "type": r.type,
          "description": r.description
      ]};
     
      def ptElementList = ptElements.collect { e -> [
           "id": e.id,
           "elementId": e.element.id,
           "obligationFlag": e.obligationFlag,
           "scope": e.scope,
           "type": elementType(pt, e),
            "valueCharacter": (ProductTypeCharacter.findByPtAndPte(pt,e) ? ProductTypeCharacter.findByPtAndPte(pt,e).value : null),
            "valueDatetime": (ProductTypeDatetime.findByPtAndPte(pt,e) ? ProductTypeDatetime.findByPtAndPte(pt,e).valueLong : null),
            "valueInteger": (ProductTypeInteger.findByPtAndPte(pt,e) ? ProductTypeInteger.findByPtAndPte(pt,e).value : null),
            "valueReal": (ProductTypeReal.findByPtAndPte(pt,e) ? ProductTypeReal.findByPtAndPte(pt,e).value : null),
            "unitsInteger": (ProductTypeInteger.findByPtAndPte(pt,e) ? ProductTypeInteger.findByPtAndPte(pt,e).units : null),
            "unitsReal": (ProductTypeReal.findByPtAndPte(pt,e) ? ProductTypeReal.findByPtAndPte(pt,e).units : null)
      ]};
    
      def ptCollectionList = ptCollection.collect { c -> [
          "id": c.id,
          "collectionId": c.collection.id
      ]};
          
      render(contentType:"text/json") {[
          "id": pt.id,
          "productType": [
              "identifier": pt.identifier,
              "title": pt.title,
              "providerId": pt.provider.id,
              "purgable": pt.purgable,
              "purgeRate": pt.purgeRate,
              "description": pt.description,
          ],
            "productTypePolicy": (ptPolicy ? [
                "dataClass": ptPolicy.dataClass,
                "dataFrequency": ptPolicy.dataFrequency,
                "dataVolume": ptPolicy.dataVolume,
                "dataDuration": ptPolicy.dataDuration,
                "dataLatency": ptPolicy.dataLatency,
                "deliveryRate": ptPolicy.deliveryRate,
                "multiDay": ptPolicy.multiDay,
                "accessType": ptPolicy.accessType,
                "basePathAppendType": ptPolicy.basePathAppendType,
                "dataFormat": ptPolicy.dataFormat,
                "compressType": ptPolicy.compressType,
                "checksumType": ptPolicy.checksumType,
                "spatialType": ptPolicy.spatialType,
                "accessConstraint": ptPolicy.accessConstraint,
                "useConstraint": ptPolicy.useConstraint
          ] : null),
          "locationPolicy": locationPolicyList,
            "productTypeResource": productTypeResourceList,
          "productTypeMetadata": (ptMetadata ? [
              "project": ptMetadata.project,
              "instrument": ptMetadata.instrument,
              "platform": ptMetadata.platform,
              "processingLevel": ptMetadata.processingLevel,
              "dataVersion": ptMetadata.dataVersion,
              "regionCoverage": ptMetadata.regionCoverage,
              "dayNight": ptMetadata.dayNight,
              "ascDesc": ptMetadata.ascDesc,
              "nativeResolution": ptMetadata.nativeResolution,
              "displayResolution": ptMetadata.displayResolution,
              "sourceProjectionId": (ptMetadata.sourceProjection ? ptMetadata.sourceProjection.id : null),
              "targetProjectionId": (ptMetadata.targetProjection ? ptMetadata.targetProjection.id : null)
          ] : null),
          "productTypeElement": ptElementList,
          "productTypeCoverage": (ptCoverage ? [
                "northLatitude": ptCoverage.northLatitude,
                "southLatitude": ptCoverage.southLatitude,
                "eastLongitude": ptCoverage.eastLongitude,
                "westLongitude": ptCoverage.westLongitude,
                "startTime": ptCoverage.startTime,
                "stopTime": ptCoverage.stopTime
          ] : null),
            "productTypeGeneration": (ptGeneration ? [
                "outputSizeX": ptGeneration.outputSizeX,
                "outputSizeY": ptGeneration.outputSizeY,
                "overviewScale": ptGeneration.overviewScale,
                "overviewLevels": ptGeneration.overviewLevels,
                "overviewResample": ptGeneration.overviewResample,
                "resizeResample": ptGeneration.resizeResample,
                "reprojectionResample": ptGeneration.reprojectionResample,
                "vrtNodata": ptGeneration.vrtNodata,
                "mrfBlockSize": ptGeneration.mrfBlockSize
            ] : null),
        "collection": ptCollectionList,
        "dataset": (ptDataset ? [
              "id": ptDataset.id,
              "datasetId": ptDataset.dataset.id
          ] : null)
          
      ]}
   }
   
   def renderProvider = { Provider provider ->
       render(contentType:"text/json") {[
           "id": provider.id,
           "shortName": provider.shortName,
           "longName": provider.longName,
           "type": provider.type
       ]}
    }
   
   def renderProjection = { Projection projection ->
       render(contentType:"text/json") {[
           "id": projection.id,
           "name": projection.name,
           "epsgCode": projection.epsgCode,
           "wg84Bounds": projection.wg84Bounds,
           "nativeBounds": projection.nativeBounds,
           "ogc_crs": projection.ogc_crs,
           "description": projection.description
       ]}
    }
   
   def renderElement = { ElementDd element ->
       render(contentType:"text/json") {[
           "id": element.id,
           "shortName": element.shortName,
           "longName": element.longName,
           "type": element.type,
           "description": element.description,
           "maxLength": element.maxLength,
           "scope": element.scope
       ]}
    }
   
   def renderCollection = { Collection collection ->
       render(contentType:"text/json") {[
           "id": collection.id,
           "shortName": collection.shortName,
           "longName": collection.longName,
           "aggregate": collection.aggregate,
           "description": collection.description,
           "fullDescription": collection.fullDescription
       ]}
    }
   
   def renderDataset = { Dataset dataset ->
       render(contentType:"text/json") {[
           "id": dataset.id,
           "remoteDatasetId": dataset.remoteDatasetId,
           "shortName": dataset.shortName,
           "longName": dataset.longName,
           "versionLabel": dataset.versionLabel,
           "metadataRegistry": dataset.metadataRegistry,
           "metadataEndpoint": dataset.metadataEndpoint,
           "description": dataset.description,
           "providerId": (dataset.provider ? dataset.provider.id : null)
       ]}
    }

   /* END: PTMT DEVELOPMENT */

/*
   
   //def index() {
   //   redirect(action: authenticate)
   // }
   
   def test = {
      println "testing console";
      def retty;
      retty = inventoryService.appendArchiveSubDir("YEAR", new Date(), null);
      println retty;
      render(contentType:"text/html"){
         div(retty);
      }
   }

   def heartbeat = {
      log.debug("Inventory Heartbeat")
      response.status = 200 //Not Found

      //      JsonBuilder json = new JsonBuilder()
      def content = { status("Inventory Service is Online") }
      render(contentType:"text/xml"){ response{content} }

      return

   }
   
   
   
   def renderResult = {Object theObject ->

      if(params.json.equals("1") || params.useJson.equals("true")) {

         render theObject as JSON
      }
      else
         render theObject as XML
   }

   def showPolicy = {
      ProductType pt = inventoryService.selectProductType(params);
      if(!pt) {
         log.debug("404, No Product Type found")
         response.status = 404 //Not Found
         render "ProductType: ${params.id} not found."
         return
      }
      ProductTypePolicy ptPolicy = ProductTypePolicy.findByPt(pt)
      renderResult(ptPolicy)
   }

   def findByProductId = {
      def productId = params.id;
      if(productId == null) {
         log.debug("404, I'm afraid i can't do that, Dave.")
         response.status = 404 //Not Found
         render "Product Id not specified."
         return
      }
      def product = Product.get(productId.toInteger().intValue());
      renderResult(product.pt);
   }
   
   def listProductsByArchiveTime = {
      
      def productPageSize = grailsApplication.config.pagination.PRODUCT_PAGE_SIZE;
      def pt = inventoryService.selectProductType(params);
      if(pt==null){
         log.warn("404, No Product Type found with with params: "+params)
         response.status = 404 //Not Found
         render "Product Type: Name or ID not found."
         return
      }
      def start = null;
      def stop = null;
      def pattern = (params.pattern) ? params.pattern : null
      def onlineOnly = false
      if(params.onlineOnly)
         onlineOnly = params.onlineOnly as Boolean

      def productList = null;
      def productCount = null;

      log.debug("pattern: $pattern");
      def page = null;
      if(params.page==null)
         page = 1;
      else{
         try{
            page = Integer.valueOf(params.page)
         }catch(Exception e){
            log.debug("Exception parsing page number: ${e.getMessage()}. Setting page=1")
            page=1;
         }
      }

      if(params.startTime != null || params.stopTime != null) {
         if(params.startTime != null){
            log.debug("ValueOf [params.startTime]:"+params.startTime);
            start = Long.valueOf(params.startTime)
         }
         else {
            start = 0;
         }
         if(params.stopTime != null){
            log.debug("ValueOf [params.stopTime]:"+params.stopTime);
            stop = Long.valueOf(params.stopTime)
         }
         else {
            stop = new Date().getTime();
         }
         
         if(onlineOnly == false) {
            productCount = Product.countByPtAndArchiveTimeBetween(pt, start, stop);
            productList = Product.findAllByPtAndArchiveTimeBetween(pt, start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatusAndArchiveTimeBetween(pt, status, start, stop);
            productList = Product.findAllByPtAndStatusAndArchiveTimeBetween(pt, status, start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }
      else {
         if(onlineOnly == false) {
            productCount = Product.countByPt(pt);
            productList = Product.findAllByPt(pt, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"])
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatus(pt, status);
            productList = Product.findAllByPtAndStatus(pt, status, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }

      render(contentType:"text/xml"){
         "response"(page:"$page",numPages: Math.ceil(productCount/productPageSize) as Integer){
            productList.each {productObj ->
               "product" {
                  "id"(productObj.id)
                  "archiveTime"(productObj.archiveTime as Long)
                  "startTime"(productObj.startTime as Long)
                }
            }
         }
      }
   }
   
   def listProducts = {
      def productPageSize = grailsApplication.config.pagination.PRODUCT_PAGE_SIZE;
      def pt = inventoryService.selectProductType(params);
      if(pt==null){
         log.warn("404, No Product Type found with with params: "+params)
         response.status = 404 //Not Found
         render "Product Type: Name or ID not found."
         return
      }
      def start = null;
      def stop = null;
      def pattern = (params.pattern) ? params.pattern : null
      def onlineOnly = false
      if(params.onlineOnly)
         onlineOnly = params.onlineOnly as Boolean

      def productList = null;
      def productCount = null;

      log.debug("pattern: $pattern");
      def page = null;
      if(params.page==null)
         page = 1;
      else{
         try{
            page = Integer.valueOf(params.page)
         }catch(Exception e){
            log.debug("Exception parsing page number: ${e.getMessage()}. Setting page=1")
            page=1;
         }
      }

      if(params.startTime != null || params.stopTime != null) {
         if(params.startTime != null){
            log.debug("ValueOf [params.startTime]:"+params.startTime);
            start = Long.valueOf(params.startTime)
         }
         else {
            start = 0;
         }
         if(params.stopTime != null){
            log.debug("ValueOf [params.stopTime]:"+params.stopTime);
            stop = Long.valueOf(params.stopTime)
         }
         else {
            stop = new Date().getTime();
         }
         
         if(onlineOnly == false) {
            productCount = Product.countByPtAndStartTimeBetween(pt, start, stop);
            productList = Product.findAllByPtAndStartTimeBetween(pt, start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatusAndStartTimeBetween(pt, status, start, stop);
            productList = Product.findAllByPtAndStatusAndStartTimeBetween(pt, status, start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }
      else {
         if(onlineOnly == false) {
            productCount = Product.countByPt(pt);
            productList = Product.findAllByPt(pt, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"])
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatus(pt, status);
            productList = Product.findAllByPtAndStatus(pt, status, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }

      render(contentType:"text/xml"){
         "response"(page:"$page",numPages: Math.ceil(productCount/productPageSize) as Integer){
            productList.each {productObj ->
               "product" {
                  "id"(productObj.id)
                  "archiveTime"(productObj.archiveTime as Long)
                  "startTime"(productObj.startTime as Long)
                }
            }
         }
      }
   }

   def listOfProductReferences = {
      def productRefPageSize = grailsApplication.config.pagination.PRODUCT_REF_PAGE_SIZE;
      def pt = inventoryService.selectProductType(params);
      if(pt==null)
      {
         log.debug("404, I'm afraid i can't do that, Dave.")
         response.status = 404 //Not Found
         render "ProductType:Name or ID not found."
         return
      }

      def page =null;
      if(params.page==null)
         page = 1;
      else{
         try{
            page = Integer.valueOf(params.page)
         }catch(Exception e){
            log.debug("Exception parsing page number: ${e.getMessage()}. Setting page=1")
            page=1;
         }
      }
      def start = (page-1)*productRefPageSize;
      def stop = (page*productRefPageSize);

      if(stop > refList.size())
         stop = refList.size();
      log.debug("page: $page, total pages: "+Math.ceil(refList.size()/productRefPageSize));
      log.debug("Reflist subset: "+refList.subList(start,stop).size());
      //    page++;
      def refList;
      try{
         refList= ProductReference.findAllByProductInList(Product.findAllByPt(pt), [max:productRefPageSize, offset:productRefPageSize * page, sort:"id", order:"desc"]);
      }
      catch(Exception e){
         log.debug("Error fetching producteRef List: " + e.getMessage());
         response.status = 500; //Not Found
         render "error: " + e.getMessage();
         return;

      }
      render(contentType:"text/xml"){
         "response"(page:"$page",numPages: ""+(Integer)Math.ceil(refList.size()/productRefPageSize)){
            for(productRef in refList)
               "productReference"{
                  "productId"(productRef.product.id)
                  "name"(productRef.name)
                  "description"(productRef.description)
                  "status"(productRef.status)
                  "path"(productRef.path)
                  "type"(productRef.type)
               }
         }
      }
      return;
   }
   
   def sizeOfProduct = {
      def pt = inventoryService.selectProductType(params);
      if(pt==null)
      {
         log.debug("404, I'm afraid i can't do that, Dave.")
         response.status = 404 //Not Found
         render "Dataset: ${params.id} not found."
         return
      }

      Integer size = inventoryService.sizeOfProduct(pt);
      if(size == null){
         response.status = 404 //Not Found
         render "No granule size information found for dataset ${params.id}"
      }
      render(contentType:"text/xml"){
         "response"{
            "int"(name:"sizeOfProduct",size)
         }
      }
      return

   }
   
   def showLatestProduct = {
      def pt = inventoryService.selectProductType(params);
      if(pt==null)
      {
         log.debug("404, I'm afraid i can't do that, Dave.")
         response.status = 404 //Not Found
         render "ProductType: Name or ID not found."
         return
      }
      List products = Product.findAllByPt(pt, [max:1, sort:"createTime", order:"desc"]);
      if(products == null || products.size() == 0)
      {
         response.status = 404 //Not Found
         render "No product found for product type ${params.id}"
         return
      }
      render(contentType:"text/xml"){
         "response"{
            "int"(name:"latestProduct",products[0].id)
         }
      }

   }
   
   def showCoverage = {
      log.debug("ProductType Coverage")
      def pt = inventoryService.selectProductType(params)
      if(pt==null)
      {
         log.debug("404, I'm afraid i can't do that, Dave.")
         response.status = 404 //Not Found
         render "ProductType: Name or ID not found."
         return
      }

      ProductTypeCoverage coverage = ProductTypeCoverage.findByPt(pt);
      renderResult(coverage);
      return;
   }

   def list = {
      def ptList = ProductType.list();
      if(params.useJson.equals("true") || params.json.equals("1")) {
         def pList = []
         for(pt in ptList)
         {
            def productType = [id:pt.id, shortname:pt.shortName]
            pList << productType
         }
         def items = [items: pList]
         render items as JSON
         return
      }

      render(contentType:"text/xml"){
         "items"{
            for(pt in ptList){
               "ProductType"{
                  "id"(pt.id)
                  "identifier"(pt.identifier)
                  "title"(pt.title)
               }
            }
         }
      }
   }
   
   */
   
}
