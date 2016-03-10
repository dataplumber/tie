package gov.nasa.horizon.inventory

import com.sun.org.apache.bcel.internal.generic.RETURN;

import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.gibs.inventory.*

class InventoryService {

   def grailsApplication
   def productMetadataService
   
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
      /*if(params.name) {
         params.name = params.name.replace("#", "&")
      }*/
      
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
   
/*   def addOperation = { Product p, ServiceProfile sip, SPAgent agent   ->
      //TODO implement
   }*/

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
   
   /*
    * Product API
    */
   
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
}
