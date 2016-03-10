package gov.nasa.horizon.inventory

import com.sun.org.apache.bcel.internal.generic.RETURN;
import gov.nasa.horizon.common.api.serviceprofile.*

import gov.nasa.gibs.inventory.*
import groovy.json.*

class PtmtService {

   def grailsApplication
   def productMetadataService
   
   /* BEGIN: PTMT DEVELOPMENT */
   
   // return JSON list of all objects with class (formName) specified in params
   def generateList = { params ->
       switch(params.formName) {
           case 'productType':
               def ptList = ProductType.list(params).collect { pt ->
                   [
                       "id": pt.id,
                       "identifier": pt.identifier
                   ]
               }
               return ptList
               break
           case 'provider':
               return Provider.list(params).collect { provider ->
                   [
                       "value": provider.id,
                       "text": provider.shortName
                   ]
               }
               break
           case 'projection':
               return Projection.list(params).collect { projection ->
                   [
                       "value": projection.id,
                       "text": projection.name
                   ]
               }
               break
           case 'element':
               return ElementDd.list(params).collect { element ->
                   [
                       "value": element.id,
                       "text": element.shortName
                   ]
               }
               break
           case 'collection':
               return Collection.list(params).collect { collection ->
                   [
                       "value": collection.id,
                       "text": collection.shortName
                   ]
               }
               break
           case 'ogc':
               return OgcTileMatrixSet.list(params)
               break
           case 'dataset':
               return Dataset.list(params).collect { dataset ->
                   [
                       "value": dataset.id,
                       "text": dataset.shortName
                   ]
               }
               break
           default:
               return []
               break
       }
   }

   // return JSON schema for form specified in params
   def generateSchema = { params ->
       switch(params.formName) {
           case 'productType':
               return '{"type":"object","title":"","properties":{"formType":{"title":"FormType","type":"string"},"id":{"title":"id","type":"number"},"json":{"title":"JSONData","type":"string"},"productType":{"title":"ProductType","type":"object","properties":{"identifier":{"title":"Identifier","type":"string","maxLength":160,"pattern":"^[0-9a-zA-Z]+[0-9a-zA-Z-_.]*$","required":true},"title":{"title":"Title","type":"string","maxLength":255,"required":true},"providerId":{"title":"ProviderID","type":"string","required":true},"purgable":{"title":"Purgable","type":"boolean"},"purgeRate":{"title":"PurgeRate","type":"integer"},"description":{"title":"Description","type":"string"}}},"productTypePolicy":{"title":"ProductTypePolicy","type":"array","items":{"title":"Policy","type":"object","properties":{"dataClass":{"title":"DataClass","type":"string","maxLength":20,"enum":["ARCHIVE-DIST","REMOTE-DIST"]},"dataFrequency":{"title":"DataFrequency(hours)","type":"integer"},"dataVolume":{"title":"DataVolume(files)","type":"integer"},"dataDuration":{"title":"DataDuration(days)","type":"integer"},"dataLatency":{"title":"DataLatency(hours)","type":"integer"},"deliveryRate":{"title":"DeliveryRate","type":"integer"},"multiDay":{"title":"Multiday","type":"integer"},"multiDayLink":{"title":"MultidayLink","type":"boolean"},"accessType":{"title":"AccessType","type":"string","maxLength":20,"enum":["OPEN","PREVIEW","CONTROLLED"],"required":true},"basePathAppendType":{"title":"BasePathAppendType","type":"string","maxLength":20,"enum":["CYCLE","YEAR","YEAR-DOY","BATCH","YEAR-FLAT","YEAR-MONTH-DAY","YEAR-WEEK"],"required":true},"dataFormat":{"title":"DataFormat","type":"string","maxLength":20,"enum":["MRF","MRF-PPNG","MRF-JPG","JPG","PNG","PPNG","TIFF","HDF","NETCDF","RAW"],"required":true},"compressType":{"title":"CompressType","type":"string","maxLength":20,"enum":["GZIP","BZIP2","ZIP","NONE"],"required":true},"checksumType":{"title":"ChecksumType","type":"string","maxLength":20,"enum":["MD5","NONE"],"required":true},"spatialType":{"title":"SpatialType","type":"string","maxLength":20,"enum":["ORACLE","POSTGRES","BACKTRACK","NONE"],"required":true},"accessConstraint":{"title":"AccessConstraint","type":"string","maxLength":1024,"required":true},"useConstraint":{"title":"UseConstraint","type":"string","maxLength":1024,"required":true}}}},"locationPolicy":{"title":"ProductTypeLocationPolicy","type":"array","items":{"title":"LocationPolicy","type":"object","properties":{"basePath":{"title":"BasePath","type":"string","maxLength":1024,"required":true},"type":{"title":"Type","type":"string","maxLength":30,"enum":["LOCAL-FTP","LOCAL-HTTP","LOCAL-LINK","REMOTE-FTP","REMOTE-HTTP"],"required":true}}}},"productTypeResource":{"title":"ProductTypeResource","type":"array","items":{"title":"Resource","type":"object","properties":{"name":{"title":"Name","type":"string","maxLength":160,"required":true},"path":{"title":"Path","type":"string","maxLength":1024,"required":true},"type":{"title":"Type","type":"string","maxLength":255,"required":true},"description":{"title":"Description","type":"string","maxLength":255}}}},"productTypeMetadata":{"title":"Metadata","type":"object","properties":{"project":{"title":"Project","type":"string","maxLength":255,"required":true},"instrument":{"title":"Instrument","type":"string","maxLength":255,"required":true},"platform":{"title":"Platform","type":"string","maxLength":255,"required":true},"processingLevel":{"title":"ProcessingLevel","type":"string","maxLength":255},"dataVersion":{"title":"DataVersion","type":"string","maxLength":255},"regionCoverage":{"title":"RegionCoverage","type":"string","maxLength":255},"dayNight":{"title":"DayNight","type":"string","maxLength":255},"ascDesc":{"title":"AscendingDescending","type":"string","maxLength":255},"nativeResolution":{"title":"NativeResolution","type":"integer"},"displayResolution":{"title":"DisplayResolution","type":"integer"},"sourceProjectionId":{"title":"SourceProjectionID","type":"string"},"targetProjectionId":{"title":"TargetProjectionID","type":"string"}}},"productTypeElement":{"title":"ProductTypeElement","type":"array","items":{"title":"Element","type":"object","properties":{"elementId":{"title":"ElementID","type":"string","required":true},"obligationFlag":{"title":"ObligationFlag","type":"boolean","required":true},"scope":{"title":"Scope","type":"string","maxLength":20,"enum":["Product","ProductType","Both"],"required":true},"type":{"title":"Type","type":"string","enum":["Character","Datetime","Integer","Real"],"required":true,"dependencies":"scope"},"valueCharacter":{"title":"Value","type":"string","required":true,"dependencies":"type"},"valueDatetime":{"title":"Value","format":"datetime","required":true,"dependencies":"type"},"valueInteger":{"title":"Value","type":"integer","required":true,"dependencies":"type"},"valueReal":{"title":"Value","type":"number","required":true,"dependencies":"type"}}}},"productTypeCoverage":{"title":"ProductTypeCoverage","type":"object","properties":{"northLatitude":{"title":"NorthLatitude","type":"number"},"southLatitude":{"title":"SouthLatitude","type":"number"},"eastLongitude":{"title":"EastLongitude","type":"number"},"westLongitude":{"title":"WestLongitude","type":"number"},"startTime":{"title":"StartTime","format":"datetime"},"stopTime":{"title":"StopTime","format":"datetime"}}},"productTypeGeneration":{"title":"ProductTypeGeneration","type":"object","properties":{"outputSizeX":{"title":"OutputSizeX","type":"integer"},"outputSizeY":{"title":"OutputSizeY","type":"integer"},"overviewScale":{"title":"OverviewScale","type":"integer"},"overviewLevels":{"title":"OverviewLevels","type":"integer"},"overviewResample":{"title":"OverviewResample","type":"string","enum":["nearest","average","gauss","cubic","average_mp","average_magphase","mode","avg"]},"resizeResample":{"title":"ResizeResample","type":"string","enum":["near","bilinear","cubic","cubicspline","lanczos","average","mode","none"]},"reprojectionResample":{"title":"ReprojectionResample","type":"string","enum":["near","bilinear","cubic","cubicspline","lanczos","average","mode"]},"vrtNodata":{"title":"vrtNodata","type":"string"},"mrfBlockSize":{"title":"mrfBlockSize","type":"integer"}}},"collection":{"title":"Collection","type":"array","items":{"title":"Collection","type":"object","properties":{"collectionId":{"title":"CollectionID","type":"string","required":true}}}},"ogcLayer":{"title":"OGCLayer","type":"object","properties":{"title":{"title":"Title","type":"string"},"format":{"title":"Format","type":"string"},"ogcLayerDimension":{"title":"OGCLayerDimension","type":"object","properties":{"identifier":{"title":"Identifier","type":"string","required":true},"title":{"title":"Title","type":"string"},"uom":{"title":"UOM","type":"string"},"defaultString":{"title":"DefaultString","type":"string"},"current":{"title":"Current","type":"boolean"},"value":{"title":"Value","type":"string"},"abstractString":{"title":"AbstractString","type":"string"},"keywords":{"title":"Keywords","type":"string"},"unitSymbol":{"title":"UnitSymbol","type":"string"}}},"ogcLayerBbox":{"title":"OGCLayerBbox","type":"array","items":{"title":"OGCLayerBbox","type":"object","properties":{"projectionId":{"title":"ProjectionID","type":"string","required":true},"lowerCorner":{"title":"LowerCorner","type":"string","required":true},"upperCorner":{"title":"UpperCorner","type":"string","required":true}}}},"ogcLayerMatrix":{"title":"OGCLayerMatrix","type":"object","properties":{"ogcTileMatrixSetId":{"title":"OGCTileMatrixSetID","type":"string","required":false}}}}},"dataset":{"title":"Dataset","type":"object","properties":{"datasetId":{"title":"DatasetID","type":"string","required":true}}}}}'
               break
           case 'provider':
               return '{"type":"object","title":"Provider","properties":{"formType":{"title":"FormType","type":"string"},"shortName":{"title":"ShortName","type":"string","required":true},"longName":{"title":"LongName","type":"string","required":true},"type":{"title":"Type","type":"string","enum":["DATA-PROVIDER","DATA-CENTER","SCIENCE-TEAM"],"required":true}}}'
               break
           case 'projection':
               return '{"type":"object","title":"Projection","properties":{"formType":{"title":"FormType","type":"string"},"name":{"title":"Name","type":"string","required":true},"epsgCode":{"title":"EPSGCode","type":"string","required":true},"wg84Bounds":{"title":"WG84Bounds","type":"string","required":true},"nativeBounds":{"title":"NativeBounds","type":"string","required":true},"ogc_crs":{"title":"OGCCRS","type":"string","required":true},"description":{"title":"Description","type":"string"}}}'
               break
           case 'element':
               return '{"title":"Element","type":"object","properties":{"formType":{"title":"FormType","type":"string"},"shortName":{"title":"ShortName","type":"string","required":true},"longName":{"title":"LongName","type":"string"},"type":{"title":"Type","type":"string","required":true},"description":{"title":"Description","type":"string"},"maxLength":{"title":"MaxLength","type":"integer"},"scope":{"title":"Scope","type":"string"}}}'
               break
           case 'collection':
               return '{"type":"object","title":"Collection","properties":{"formType":{"title":"FormType","type":"string"},"shortName":{"title":"ShortName","type":"string","required":true},"longName":{"title":"LongName","type":"string","required":true},"aggregate":{"title":"Aggregate","type":"boolean","required":true},"description":{"title":"Description","type":"string","required":true},"fullDescription":{"title":"FullDescription","type":"string"}}}'
               break
           case 'ogc':
               return '{"title":"OGCTileMatrixSet","type":"object","properties":{"formType":{"title":"FormType","type":"string"},"identifier":{"title":"Identifier","type":"string","required":true},"tileMatrixSet":{"title":"TileMatrixSet","type":"array","items":{"title":"TileMatrix","type":"object","properties":{"identifier":{"title":"Identifier","type":"string","required":true},"scaleDenominator":{"title":"ScaleDenominator","type":"number"},"topLeftCornerX":{"title":"TopLeftCornerX","type":"integer"},"topLeftCornerY":{"title":"TopLeftCornerY","type":"integer"},"tileWidth":{"title":"TileWidth","type":"integer"},"tileHeight":{"title":"TileHeight","type":"integer"},"matrixWidth":{"title":"MatrixWidth","type":"integer"},"matrixHeight":{"title":"MatrixHeight","type":"integer"}}}}}}'
               break
           case 'dataset':
               return '{"type":"object","title":"Dataset","properties":{"formType":{"title":"FormType","type":"string"},"remoteDatasetId":{"title":"RemoteDatasetID","type":"string","required":true},"shortName":{"title":"ShortName","type":"string","required":true},"longName":{"title":"LongName","type":"string","required":true},"versionLabel":{"title":"VersionLabel","type":"string","required":true},"metadataRegistry":{"title":"MetadataRegistry","type":"string","enum":["ECHO-REST","PO.DAAC-OPENSEARCH"]},"metadataEndpoint":{"title":"MetadataEndpoint","type":"string"},"description":{"title":"Description","type":"string"},"providerId":{"title":"ProviderID","type":"string"}}}'
               break
           default:
               return '{}'
               break
       }
   }
   
   // return ProductType object by id/name params
   def getProductType = { params ->
      if(params.id)
         return ProductType.get(params.id as Long);
      else if(params.name)
         return ProductType.findByIdentifier(params.name);
      else return null;
   }
   
   // return Provider object by id/name params
   def getProvider = { params ->
      if(params.id)
         return Provider.get(params.id as Long);
      else if(params.name)
         return Provider.findByShortName(params.name);
      else return null;
   }
   
   // return Projection object by id/name params
   def getProjection = { params ->
      if(params.id)
         return Projection.get(params.id as Long);
      else if(params.name)
         return Projection.findByName(params.name);
      else return null;
   }
   
   // return Element by params (id or name)
   def getElement = { params ->
      if(params.id)
         return ElementDd.get(params.id as Long);
      else if(params.name)
         return ElementDd.findByShortName(params.name);
      else return null;
   }
   
   // return Collection by params (id or name)
   def getCollection = { params ->
      if(params.id)
         return Collection.get(params.id as Long);
      else if(params.name)
         return Collection.findByShortName(params.name);
      else return null;
   }
   
   /*
   // return ogc object by id/name params
   def getOgc = { params ->
      if(params.id)
         return ProductType.get(params.id as Long);
      else if(params.name)
         return ProductType.findByIdentifier(params.name);
      else return null;
   }
   */
   
   // return Dataset object by id/name params
   def getDataset = { params ->
      if(params.id)
         return Dataset.get(params.id as Long);
      else if(params.name)
         return Dataset.findByShortName(params.name);
      else return null;
   }
   
   /* END: PTMT DEVELOPMENT */

/*
      
   //Return producttype based on param input - either id or name
   def selectProductType = { params ->

      if(params.id)
         return ProductType.get(params.id as Long);
      else if (params.ptId)
         return ProductType.get(params.ptId as Long)
      else if(params.name || params.ptName)
         return ProductType.findByIdentifier(params.name);
      else return null;
   }

   def selectProduct = { params ->
//      if(params.name) {
//         params.name = params.name.replace("#", "&")
//      }
      
      if(params.id) {
         return Product.get(params.id as Long);
      }
      else if(params.productName && params.ptId) {
         ProductType pt = ProductType.get(params.ptId);
         return Product.findByNameAndPt(params.productName, pt);
      }
      else if(params.productName && params.ptName) {
         ProductType pt = ProductType.findByIdentifier(params.ptName);
         return Product.findByNameAndPt(params.productName, pt);
      }
      else return null;
   }

   def ingestSip = { ServiceProfile sip ->
      
      Product product
      
      if(sip.getSubmission().getArchive()) {
         log.debug("Archive SIP detected, processing products and marking them ONLINE")
         processArchive(sip);
      }
      else {
         
         log.debug("Store ServiceProfile as no archive element was found")
         //Map with [sip, product]
         Map sipProductMap = productMetadataService.storeSip(sip)
   
         sip = sipProductMap.sip
         product = sipProductMap.product
      }
      //Add operations to db and add id to said operations in sip
      sip = processOperations(sip)
      
      sip = flagSuccess(sip)
      
      log.debug("finished storing profile")
      return sip
   }
   
   def checkSip = { ServiceProfile sip ->
      def header = sip.getSubmission().getHeader()
      def result = true
      
      def ptName = header.getProductType()
      def createTime = header.getCreateTime().getTime()
      
      def pt = ProductType.findByIdentifier(ptName)
      if(pt) {
         def policy = ProductTypePolicy.findByPt(pt)
         //Check if product type is of type MRF, otherwise, no check needed and just accept
         if (policy.dataFormat == "MRF") {
            //It's an MRF ProductType! Time to check most recent product's created time vs this sip
            List<Product> products = Product.findAllByPt(pt, [max:1, sort:"createTime", order:"desc"])
            if(products != null && products.size() == 1 && products.get(0).createTime > createTime) {
               // Created time for latest product greater than created time for current sip, reject!
               log.info("Latest MRF product created after current SIP. Rejecting.")
               result = false
            }
         }
      }
      else {
         //No ProductType found, return failure
         result = false
      }
      return result
   }
   
   def processArchive = {ServiceProfile sip ->
      def archive = sip.getSubmission().getArchive()
      def header = sip.getSubmission().getHeader()
      
      def ptId = header.getInventoryProductTypeInventoryId()
      def productId = header.getInventoryProductInventoryId()
      
      if(!ptId || !productId) {
         log.error("No Product Type ID or Product ID found in Archive SIP")
         throw new Exception("No Product Type ID or Product ID found in Archive SIP")
         return
      }
      
      def pt = ProductType.get(ptId)
      def product = Product.get(productId)
      
      if(!pt || !product) {
         log.error("No Product Type or Product found from IDs in Archive SIP")
         throw new Exception("No Product Type or Product found from IDs in Archive SIP")
         return
      }
      
      log.info("Processing archive for Product: "+product.name+" of Product Type: "+pt.identifier)
      
      def ingestProductFiles = archive.getIngestProductFiles()
      
      ingestProductFiles.each { ingestProductFile ->
         def fileName = ingestProductFile.getProductFile().getFile().getName()
         def productFile = ProductArchive.findByProductAndName(product, fileName)
         if(!productFile) {
            throw new Exception("No ProductArchive Record found for: "+fileName)
            return
         }
         productFile.status = "ONLINE"
         productFile.save(flush:true)
      }
      
      //Update product with ONLINE flag and set archive time
      product.status = "ONLINE"
      product.archiveTime = new Date().getTime()
      
      product.save(flush:true)
      
      return sip
   }
   
   def updateProductRootPath = {Product p, String rootPath ->
      p.rootPath = rootPath
      p.save(flush:true)
   }
   
   def updateProductTypeAssociation = {Product p, ProductType pt ->
      p.pt = pt
      p.save(flush:true)
   }
   
   def updateProductStatus = {Product product, String status ->
      def productFiles = ProductArchive.findAllByProduct(product)
      productFiles.each { productFile ->
         productFile.status = status
         productFile.save(flush:true)
         def refs = ProductArchiveReference.findAllByProductArchive(productFile)
         refs.each{ ref ->
            ref.status = status
            ref.save(flush:true)
         }
      }
      product.status = status
      if(status.equals("ONLINE"))
         product.archiveTime = new Date().getTime()
      product.save(flush:true)
   }
   
   def processOperations = {ServiceProfile sip ->
      def header = sip.getSubmission().getHeader()
      Product product = Product.get(header.getProductInventoryId())
      if(!product) {
         throw new Exception("No product with id "+header.getProductInventoryId()+" found in sip while processing operations")
      }
      def operations = header.getOperations()
      
      operations.each { operation ->
         if(operation.getOperationId() == null) {
            def agent = operation.getAgent()
            def operationLabel = operation.getOperation()
            def startTime = (operation.getOperationStartTime() != null) ? operation.getOperationStartTime().getTime() : null
            def stopTime = (operation.getOperationStopTime() != null) ? operation.getOperationStopTime().getTime() : null
            def command = operation.getCommand()
            def args = operation.getArguments()
            
            ProductOperation operationObj = new ProductOperation(product:product, agent:agent, operation:operationLabel, startTime:startTime, stopTime:stopTime, command:command, arguments:args)
            operationObj.save(flush:true)
            operation.setOperationId(operationObj.id)
         }
      }
      return sip
   }
   
//     def addOperation = { Product p, ServiceProfile sip, SPAgent agent -> }

   def flagSuccess = { ServiceProfile sip ->
      
      sip.getSubmission().getArchive().setOperationSuccess(true)
      return sip
   }
   
   //TODO Finish implementation
   def sizeOfProduct = {ProductType pt ->
      Integer productCount = 0
      productCount = Product.countByPt(pt)
      return productCount
   }

   //Filter product ids to ones that exist in db
   def findProductList = { idList ->
      def results = Product.findAllByIdInList(idList);
      def foundList = [];
      results.each() {
         foundList.add(it.id);
      }
      return foundList;
   }

   def updateGranulereferencePath = {Product p, String path, String newPath ->
      def refList = ProductReference.findAllByProductAndPath(p, path);
      refList.each() { ref ->
         ref.path = newPath;
         ref.save();
      }
   }

   
   def getProductFullPath = {pId ->
      def p = Product.get(pId as Long);
      def pt = p.pt;
      def basePath = ProductTypePolicy.findByPtAndType(pt, grailsApplication.config.constants.locationPolicy.ARCHIVE).basePath
      
      //TODO implement
   }
   
    // Product API
   
   def deleteProduct = {Product p ->
      //TODO GIBS specific code
      def granuleImageryRows = GranuleImagery.findAllByProduct(p)
      granuleImageryRows.each { row ->
         row.delete(flush:true)
      }
      p.delete(flush:true)

      return true
   }
   
   def updateProductArchiveChecksum = {Product p, String name, String csum ->
      List<ProductArchive> archives = ProductArchive.findAllByProductAndName(p, name)
      if(archives.size() > 0) {
         archives.each { archive ->
            archive.checksum = csum
            if(!archive.save(flush:true)) {
               log.error("Could not update archive status for product "+p.getName())
               throw new Exception("Could not update archive status for product "+p.getName())
               
            }
         }
      }
      return true
   }
   
   
   def updateProductArchiveStatus = {Product p,  String status ->
      List<ProductArchive> archives = ProductArchive.findAllByProduct(p)
      if(archives.size() > 0) {
         archives.each { archive ->
            archive.status = status
            if(!archive.save(flush:true)) {
               log.error("Could not update archive status for product "+p.getName())
               throw new Exception("Could not update archive status for product "+p.getName())
               
            }
         }
      }
      return true
   }
   
   def updateProductArchiveSize = {Product p, String, name, Long fsize ->
      List<ProductArchive> archives = ProductArchive.findAllByProductAndName(p, name)
      if(archives.size() > 0) {
         archives.each { archive ->
            archive.fileSize = fsize
            if(!archive.save(flush:true)) {
               log.error("Could not update archive status for product "+p.getName())
               throw new Exception("Could not update archive status for product "+p.getName())
               
            }
         }
      }
      return true
   }
   
*/
   
}
