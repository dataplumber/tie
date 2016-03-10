package gov.nasa.gibs.inventory

import gov.nasa.horizon.inventory.*

class GibsController {

   def inventoryService

   def index() {
      redirect params
   }
   
   def showGen = {
      def pt = inventoryService.selectProductType(params)
      if(!pt) {
         response.status = 404
         render "Could not find specified Product Type"
         return
      }
      def gen = ProductTypeGeneration.findByPt(pt)
      if(gen) {
         render(contentType:"text/xml"){
            "generation" {
               "outputSizeX"(gen.outputSizeX)
               "outputSizeY"(gen.outputSizeY)
               "overviewScale"(gen.overviewScale)
               "overviewLevels"(gen.overviewLevels)
               "overviewResample"(gen.overviewResample)
               "resizeResample"(gen.resizeResample)
               "reprojectionResample"(gen.reprojectionResample)
               "vrtNodata"(gen.vrtNodata)
               "mrfBlockSize"(gen.mrfBlockSize) 
            }
         }
      }
      else {
         response.status = 500
         render "No Generation information found for specified ProductType"
         return
      }
   }

   def listImages = {
      def pt = inventoryService.selectProductType(params)
      if(!pt) {
         response.status = 404
         render "Could not find specified Product Type"
         return
      }
      def policy = ProductTypePolicy.findByPt(pt)
      def imageType = policy.dataFormat
      
      def start, stop
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

         def status = "ONLINE"
         def productList = Product.findAllByPtAndStatusAndStartTimeBetween(pt, status, start, stop);
         
         def imageList = [];
         def sourceList = [:];
         
         productList.each {product ->
            def productFile = ProductArchive.findByProductAndType(product, "IMAGE")
            def productGranuleList = GranuleImagery.findAllByProduct(product)
            productGranuleList.each { granuleJoin ->
               def granule = granuleJoin.granule;
               def dataset = granule.dataset
               def datasetName = dataset.shortName
               def granuleName = granule.remoteGranuleUr
               sourceList."$datasetName-$granuleName" = [productType:dataset.shortName, product:granule.remoteGranuleUr, repo: granule.metadataEndpoint]
            }
            def imagePath = product.rootPath + File.separator + product.relPath + File.separator + productFile.name
            imageList.add([path:imagePath, type:imageType])
         }
         
         println sourceList
         
         render(contentType:"text/xml"){
            "response" {
               "images" {
                  imageList.each {imageObj ->
                     "image" {
                        "path"(imageObj.path)
                        "type"(imageObj.type)
                     }
                  }
               }
               "sources" {
                  sourceList.each{ key, sourceObj ->
                     "source" {
                        "productType"(sourceObj.productType)
                        "product"(sourceObj.product)
                        "repo"(sourceObj.repo)
                     }
                  }
               }
            }
         }
      }
      else {
         response.status = 500
         render "Must specify product type id or name (id, name) and a start time and end time to query (startTime, endTime)"
         return
      }
   }
   
   def showProjection = {
      def pt = inventoryService.selectProductType(params)
      if(!pt) {
         response.status = 404
         render "Could not find specified Product Type"
         return
      }
      def metadata = ProductTypeMetadata.findByPt(pt)
      def proj = metadata.targetProjection
      if(proj) {
         render(contentType:"text/xml"){
            "projection" {
               "name"(proj.name)
               "epsgCode"(proj.epsgCode)
               "wg84Bounds"(proj.wg84Bounds)
               "nativeBounds"(proj.nativeBounds)
               "ogc_crs"(proj.ogc_crs)
               "description"(proj.description)
            }
         }
      }
      else {
         response.status = 500
         render "No Projection found for specified ProductType"
         return
      }
   }
   
   def showColormap = {
      def pt = inventoryService.selectProductType(params)
      if(!pt) {
         response.status = 404
         render "Could not find specified Product Type"
         return
      }
      def colormapResource = ProductTypeResource.findAllByPtAndType(pt, "colormap")
      if(colormapResource) {
         render(contentType:"text/xml"){
            "colormap" {
               "name"(colormapResource.name)
               "type"(colormapResource.type)
               "path"(colormapResource.path)
               "description"(colormapResource.description)
            }
         }
      }
      else {
         response.status = 500
         render "No Colormap found for specified ProductType"
         return
      }
   }
   
   def showEmptyTile  = {
      def pt = inventoryService.selectProductType(params)
      if(!pt) {
         response.status = 404
         render "Could not find specified Product Type"
         return
      }
      def emptyTileResource = ProductTypeResource.findAllByPtAndType(pt, "empty_tile")
      if(emptyTileResource) {
         render(contentType:"text/xml"){
            "empty_tile" {
               "name"(emptyTileResource.name)
               "type"(emptyTileResource.type)
               "path"(emptyTileResource.path)
               "description"(emptyTileResource.description)
            }
         }
      }
      else {
         response.status = 500
         render "No Empty Tile found for specified ProductType"
         return
      }
   }
}
