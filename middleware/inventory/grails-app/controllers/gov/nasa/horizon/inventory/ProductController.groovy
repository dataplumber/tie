package gov.nasa.horizon.inventory

//import grails.util.JSonBuilder
import groovy.xml.MarkupBuilder
import grails.converters.deep.XML;
import grails.converters.deep.JSON;

import gov.nasa.horizon.common.api.util.StringUtility;

class ProductController {

   def inventoryService;
   def authenticationService;

   def index() {
   }

   def show = {

      log.debug("params: $params")

      Product p =  inventoryService.selectProduct(params);
      if(p==null)
      {
         response.status = 404
         render "Product not found."
         return;
      }

      renderProduct(p)
      return;
   }

   def renderProduct = {Product p ->
      render(contentType:"text/xml") {
         "product" {
            "productId"(p.id)
            "productTypeId"(p.pt.id)
            "name"(p.name)
            "startTime"(p.startTime)
            "stopTime"(p.stopTime)
            "createTime"(p.createTime)
            "archiveTime"(p.archiveTime)
            "version"(p.versionNum)
            "status"(p.status)
            "rootPath"(p.rootPath)
            "relPath"(p.relPath)
            "archiveSet" {
               p.archive.each() { archiveFile ->
                  "archive" {
                     "type"(archiveFile.type)
                     "fileSize"(archiveFile.fileSize)
                     "compressFlag"(archiveFile.compressFlag)
                     "checksum"(archiveFile.checksum)
                     "name"(archiveFile.name)
                     "status"(archiveFile.status)
                  }
               }
            }
         }
      }
   }

   //lists products matching the criteria
   def list = {
      //params //start, //stop, // compareField (ingest or archive) //dsId
      def lst = null
      try{
         def startLong = params.start as long;
         def stopLong = params.stop as long;
         def pt = ProductType.get(params.ptId as Integer)
         def start = Calendar.getInstance();
         def stop = Calendar.getInstance();
         if(startLong != null) {
            start.setTimeInMillis(startLong);
         }
         else start.add(Calendar.DATE, -1);
         if(stopLong != null) {
            stop.setTimeInMillis(stopLong);
         }
         products = Product.findAllByPtAndCreateTimeBetween(pt, start.getTime(), stop.getTime())

      }catch(Exception e){
         response.status = 400
         render "Error processing request: " + e.getMessage()
         return
      }


      response.status = 200;
      render(contentType:"text/xml"){
         "response"{
            products.each() {
               "product"(id:it.id)
            }
         }
      }
   }


   def delete = {
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to delete products"
         return
      }
      Product p =   inventoryService.selectProduct(params);
      if(p==null)
      {
         response.status = 404
         render "Product not found."
         return;
      }
      log.debug("delete product " + p.name);
      def dOnly = false;
      if(params.dataOnly == null || params.dataOnly.equals("false"))
      {
         dOnly=false;
         log.debug("DELETING ALL DATA");
      }
      else{
         dOnly=true;
         log.debug("DELETING DATA ONLY");
      }
      try{
         if(dOnly)
            inventoryService.deleteDataOnly(p);
         else
            inventoryService.deleteProduct(p);
      }catch(Exception e){
         render "Error Deleting Product."
         log.debug("StackTrace: ",e);
         response.status = 500
         return;
      }
      render "Successfully Deleted."
      response.status = 200;
      return;
   }

   def findProductList = {
      def ids = params.ids;
      def list = []

      ids.split(',').each() {
         list.add(it);
      }

      log.debug("size of list: " + list.size);
      log.debug("resolveId list: $ids");
      def pIdList = inventoryService.findProductList(list);

      renderResult(pIdList);

   }

   private void renderResult(Object theObject){
      if(theObject == null){
         theObject = []
      }

      //XML.use("deep")
      if(params.json.equals("1") || params.useJson.equals("true"))
         render theObject as JSON
      else
         render theObject as XML
   }

   def listOperations = {
      Product p = inventoryService.selectProduct(params)
      if(p==null){
         response.status = 404
         render "Product not found."
         return;
      }
      def operationList = ProductOperation.findAllByProduct(p)

      response.status = 200;
      render(contentType:"text/xml"){
         "operations"{
            operationList.each() { operationObj ->
               "operation"(product:product.id) {
                  "agent"(operationObj.agent)
                  "operation"(operationObj.operation)
                  "startTime"(operationObj.startTime)
                  "stopTime"(operationObj.stopTime)
                  "command"(operationObj.command)
                  "arguments"(operationObj.arguments)
               }
            }
         }
      }
   }

   def showOperation = {
      Product p = inventoryService.selectProduct(params)
      if(p==null){
         response.status = 404
         render "Product not found."
         return;
      }
      if(params.operation == null) {
         response.status = 404
         render "No operation specified"
         return;
      }
      def operation = ProductOperation.findByProductAndOperation(product, params.operation)
      if(operation == null) {
         if(params.operation == null) {
            response.status = 404
            render "Operation not found"
            return;
         }
      }
      
      response.status = 200;
      render(contentType:"text/xml"){
         "operation"(product:product.id) {
            "agent"(operationObj.agent)
            "operation"(operationObj.operation)
            "startTime"(operationObj.startTime)
            "stopTime"(operationObj.stopTime)
            "command"(operationObj.command)
            "arguments"(operationObj.arguments)
         }
      }
   }
   
   def updateProduct = {
      log.debug("updateProductStatus pId: ${params.id}, status:${params.status}")
      
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the product status"
         return
      }
      
      def pt = null
      if(params.ptId) {
         def ptId = params.int("ptId")
         if(ptId != null) {
            try {
               pt = ProductType.findById(ptId)
            }
            catch(Exception e) {
               response.status 400
               render "Product type specified does not exist"
               return
            }
         }
      }
      
      def rootPath = null
      if(params.rootPath != null && params.rootPath != "") {
         rootPath = params.rootPath
      }
      if(pt == null && rootPath == null)
      {
         response.status 400
         render "You must set the product type id or root path (ptId, rootPath)"
         return
      }

      Product p =  inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }
      
      if(pt) {
         inventoryService.updateProductTypeAssociation(p, pt)
      }
      if(rootPath) {
         inventoryService.updateProductRootPath(p, rootPath)
      }
      
      response.status=200
      render "Product updated"

   }

   def updateProductStatus = {
      log.debug("updateProductStatus pId: ${params.id}, status:${params.status}")
      
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the product status"
         return
      }

      if(params.status == null)
      {
         response.status 400
         render "You must set the product status"
         return
      }
      def status = params.status
      try{
         status= grailsApplication.config.constants.productStatus."${status}"
      }catch(IllegalArgumentException iae){
         response.status= 400
         render "You must set a valid product status"
         return
      }
      if(!status) {
         response.status= 400
         render "You must set a valid product status"
         return
      }

      Product p =  inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }
      
      inventoryService.updateProductStatus(p, status)
      
      response.status=200
      render "Product status changed to $status"

   }

   def fetchArchivePath = {
      def p = inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }

      def path = StringUtility.cleanPaths(p.rootPath, p.relPath, p.name);
      path = path.substring(0, path.lastIndexOf("/"))
      
      if(path == null)
      {
         response.status = 404
         render "Product path not found."
         return
      }

      render(contentType:"text/xml"){
         "response"{
            "str"(name:"path",path)
         }
      }
      return

   }

   def updateAIPRef = {
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}

      def type = params.type;
      def dest = params.dest;
      def fileName = params.fname;
      def status = params.status;

      log.debug("type: $type\ndest: $dest\nfileName:$fileName\nstatus:$status");
      if(type ==null || dest==null || fileName==null || status==null || params.id==null){
         render "Error Processing input."
         response.status =  405
         return
      }

      log.debug("updating...");

      Product p = inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }
      def name = params.fname;
      ProductArchiveReference pars = ProductArchiveReference.findAllByProductAndNameLike(p, "%$name%");
      pars.each() {par ->
         //Update product archive ref values
         par.status = params.status;
      }
      response.status=200;
      render "success";

   }

   def updateAIPArch = {
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}

      def type = params.type;
      def dest = params.dest;
      def fileName = params.fname;
      def status = params.status;

      log.debug("type: $type\ndest: $dest\nfileName:$fileName\nstatus:$status");
      if(type ==null || dest==null || fileName==null || status==null || params.id==null){
         render "Error Processing input."
         response.status =  405
         return
      }

      log.debug("updating...");

      Product p = inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }
      def name = params.fname;
      ProductArchive pas = ProductArchive.findAllByProductAndNameLike(p, "%$name%");
      pas.each() {pa ->
         //Update product archive ref values
         pa.status = params.status;
      }
      response.status=200;
      render "success";

   }

   def updateReassociateProductElement = {
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   log.debug("error authenticating.");
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}
      Product p = inventoryService.selectProduct(params);
      if(p==null)
      {
         log.debug("Product Not Found")
         response.status = 404
         render "Product not found"
         return;
      }

      def fromD = params.from;
      def toD = params.to;

      def from = ProductType.get(fromD);
      def to = ProductType.get(toD);

      if(from == null || to == null){
         render "From or To product types were incorrectly formed."
         response.status = 400
         return;
      }

      try{
         log.debug("calling reassoc GEs");
         //MUST IMPLEMENT
         //inventoryService.reassociateGranuleElement(p,to,from);
      }catch(Exception e){
         render "Error processing requiest: " + e.getMessage();
         response.status = 500
         return;
      }

      response.status=200;
      render "success";
      return;

   }

   def updateReassociateProduct = {
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}
      Product p = inventoryService.selectProduct(params);
      if(p==null)
      {
         render "Product not found."
         response.status = 404
         return;
      }
      def root = params.rootPath;
      def ptId = params.ptId;
      log.debug("to ptId: $ptId")
      def pt = ProductType.getAt(ptId);
      if(root==null || pt == null){
         render "Root path or ProductType ID not supplied."
         response.status = 404
         return;
      }

      p.pt = pt;
      p.rootPath = root;

      try{
         p.save();
      }catch(Exception e){
         render "Error processing requiest: " + e.getMessage();
         response.status = 500
         return;
      }

      response.status=200;
      render "success";
      return;

   }

   def updateProductRootPath = {
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}
      Product p = inventoryService.selectProduct(params);
      if(p==null)
      {
         render "Product not found."
         response.status = 404
         return;
      }
      def root = params.rootPath;
      if(root==null){
         render "Root path not supplied."
         response.status = 404
         return;
      }

      p.rootPath = root;

      try{
         p.save();
      }catch(Exception e){
         render "Error processing request: " + e.getMessage();
         response.status = 500
         return;
      }

      response.status=200;
      render "success";
      return;

   }

   //post
   //id,path,status, type,desc?
   def addNewProductReference = {
      //TODO finish this
      //if(!authenticationService.authenticate(params.userName, params.password)){
      //   response.status =  409
      //   render "You're not authorized to update the granule status"
      //   return
      //}
      Product p = inventoryService.selectProduct(params);

      if(p==null)
      {
         render "Product not found."
         response.status = 404
         return;
      }

      def type = params.type;
      def status = params.status;
      def path = params.path;
      def desc = params.desc;

      if(type == null || status == null || path == null){
         render "type Variable not supplied."
         response.status = 400
         return;
      }

      try{
         inventoryService.addGranuleReference(g.getGranuleId(), type,status,path,desc);

      }catch(Exception e){
         render "Error processing requiest: " + e.getMessage();
         response.status = 500
         return;
      }
      response.status=200;
      render "success";
      return;
   }

   def updateProductReferencePath = {
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the granule status"
         return
      }
      Product p = inventoryService.selectProduct(params);

      if(p==null)
      {
         render "Product not found."
         response.status = 404
         return;
      }

      def newpath = params.newpath;
      def path = params.path;

      if( newpath == null || path == null){
         render "Variable not supplied."
         response.status = 400
         return;
      }
      try{
         inventoryService.updateGranuleReferencePath(p, path,newpath);
      }catch(Exception e){
         render "Error processing requiest: " + e.getMessage();
         response.status = 500
         return;
      }
      response.status=200;
      render "success";
      return;
   }
   
   def updateProductArchiveStatus = {
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the product archive status"
         return
      }
      Product p = inventoryService.selectProduct(params)
      
      def status = params.status
      if( status == null){
         render "Variable not supplied. (status)"
         response.status = 400
         return;
      }

      try{
         inventoryService.updateProductArchiveStatus(p,status)
      }catch(Exception e){
         render "Error processing request: " + e.getMessage();
         response.status = 500
         return;
      }
      log.info("Updated archive status for product "+p.getName()+" to "+status)
      response.status=200;
      render "success";
      return;
   }
   
   def updateProductArchiveChecksum = {
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the product archive status"
         return
      }
      Product p = inventoryService.selectProduct(params)
      
      def checksum = params.checksum
      def name = params.name
      if( checksum == null || name == null){
         render "Variable not supplied. (name or status)"
         response.status = 400
         return;
      }
      
      try{
         inventoryService.updateProductArchiveChecksum(p, name ,checksum)
      }catch(Exception e){
         render "Error processing request: " + e.getMessage();
         response.status = 500
         return;
      }
      log.info("Updated archive checksum for product "+p.getName()+" and file "+name+" to "+checksum)
      response.status=200;
      render "success";
      return;
   }
   
   def updateProductArchiveSize = {
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  409
         render "You're not authorized to update the product archive status"
         return
      }
      Product p = inventoryService.selectProduct(params);
      
      def fsize = params.fileSize
      def name = params.name
      if( fsize == null || name == null){
         render "Variable not supplied. (name or status)"
         response.status = 400
         return;
      }
      
      try{
         inventoryService.updateProductArchiveSize(p, name,fsize)
      }catch(Exception e){
         render "Error processing request: " + e.getMessage();
         response.status = 500
         return;
      }
      log.info("Updated archive size for product "+p.getName()+" and file "+name+" to "+fsize)
      response.status=200;
      render "success";
      return;
   }
}
