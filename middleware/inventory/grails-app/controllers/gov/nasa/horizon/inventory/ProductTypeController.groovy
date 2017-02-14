package gov.nasa.horizon.inventory

import groovy.json.*
import java.net.URLDecoder;
import groovy.xml.MarkupBuilder
import grails.converters.XML;
import grails.converters.JSON;

/*import gov.nasa.podaac.common.api.metadatamanifest.MetadataManifest
 import gov.nasa.podaac.common.api.metadatamanifest.Constant.ActionType
 import gov.nasa.podaac.common.api.metadatamanifest.Constant.ObjectType
 */

class ProductTypeController {
   static scaffold = false;

   def inventoryService;
   def grailsApplication;

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

   def show = {
      ProductType pt = inventoryService.selectProductType(params);
      if(pt == null){
         log.debug("404, No dataset found or no identfier (id or name) specified.")
         response.status = 404 //Not Found
         render "No dataset found or no identfier (id or name) specified."
         return
      }
      renderProductType(pt)
      return;
   }


   def renderProductType = {ProductType pt ->
      //Replace producttype/product labels according to config.groovy
      def productTypeLabel = grailsApplication.config.constants.domainLabel.PRODUCT_TYPE;
      def productLabel = grailsApplication.config.constants.domainLabel.PRODUCT;
      def policyLabel = "policy";
      def ptPolicy = ProductTypePolicy.findByPt(pt);
      render(contentType:"text/xml"){
         "ProductType"(id:pt.id){
            "productTypeId"(pt.id)
            "identifier"(pt.identifier)
            "title"(pt.title)
            "purgable"(pt.purgable)
            "purgeRate"(pt.purgeRate)
            "description"(pt.description)
            "policy"{
               "dataClass"(ptPolicy.dataClass)
               "dataFrequency"(ptPolicy.dataFrequency)
               "dataVolume"(ptPolicy.dataVolume)
               "dataDuration"(ptPolicy.dataDuration)
               "dataLatency"(ptPolicy.dataLatency)
               "deliveryRate"(ptPolicy.deliveryRate)
               "multiDay"(ptPolicy.multiDay)
               "accessType"(ptPolicy.accessType)
               "basePathAppendType"(ptPolicy.basePathAppendType)
               "dataFormat"(ptPolicy.dataFormat)
               "compressType"(ptPolicy.compressType)
               "checksumType"(ptPolicy.checksumType)
               "spatialType"(ptPolicy.spatialType)
               "accessConstraint"(ptPolicy.accessConstraint)
               "useConstraint"(ptPolicy.useConstraint)
            }
            "locationPolicySet" {
               pt.locationPolicies.each { lp ->
                  "locationPolicy"{
                     "type"(lp.type)
                     "basePath"(lp.basePath)
                  }
               }
            }
            "resourceSet" {
               pt.resources.each { r ->
                  "resource" {
                     "name"(r.name)
                     "path"(r.path)
                     "type"(r.type)
                     "description"(r.description)
                  }
               }
            }
            "coverages" {
               //Finish the marshalling
            }
         }
      }
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
                  "stopTime"(productObj.stopTime as Long)
                  "name"(productObj.name as String)
                }
            }
         }
      }
   }

   def listProductsByDataDay = {
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
      def pattern = (params.pattern) ? params.pattern : "%"
      pattern = pattern.replaceAll("#", "%")
      def onlineOnly = false
      if(params.onlineOnly)
         onlineOnly = params.onlineOnly as Boolean

      def productList = null
      def productCount = null
      def dataDayList = null

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
            dataDayList = ProductDataDay.findAllByDataDayBetween(start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"dataDay", order:"desc"]);
            productList = dataDayList.collect {it.product}
            productList = productList.unique()
            productCount = productList.size
         }
         else {
            def status = "ONLINE"
            dataDayList = ProductDataDay.findAllByDataDayBetween(start, stop, [max:productPageSize, offset:productPageSize * (page-1), sort:"dataDay", order:"desc"]);
            productList = dataDayList.findResults {(it.status == status) ? it : null}
            productCount = productList.size
         }
      }
      else {
         log.warn("404, No startTime and stopTime params found: "+params)
         response.status = 404 //Not Found
         render "Please specify startTime and stopTime parameters"
         return
      }

      render(contentType:"text/xml"){
         "response"(page:"$page",numPages: Math.ceil(productCount/productPageSize) as Integer){
            productList.each {productObj ->
               "product" {
                  "id"(productObj.id)
                  "archiveTime"(productObj.archiveTime as Long)
                  "startTime"(productObj.startTime as Long)
                  "stopTime"(productObj.stopTime as Long)
                  //productObj.dataDay.each { dd ->
                  //   "dataDay"(dd.dataDay)
                  //}
                  "name"(productObj.name as String)
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
      def pattern = (params.pattern) ? params.pattern : "%"
      pattern = pattern.replaceAll("#", "%")
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
            productCount = Product.countByPtAndStartTimeBetweenAndNameIlike(pt, start, stop, pattern);
            productList = Product.findAllByPtAndStartTimeBetweenAndNameIlike(pt, start, stop, , pattern, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatusAndStartTimeBetweenAndNameIlike(pt, status, start, stop, pattern);
            productList = Product.findAllByPtAndStatusAndStartTimeBetweenAndNameIlike(pt, status, start, stop, pattern, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }
      else {
         if(onlineOnly == false) {
            productCount = Product.countByPtAndNameIlike(pt, pattern);
            productList = Product.findAllByPtAndNameIlike(pt, pattern, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"])
         }
         else {
            def status = "ONLINE"
            productCount = Product.countByPtAndStatusAndNameIlike(pt, status, pattern);
            productList = Product.findAllByPtAndStatusAndNameIlike(pt, status, pattern, [max:productPageSize, offset:productPageSize * (page-1), sort:"createTime", order:"desc"]);
         }
      }

      render(contentType:"text/xml"){
         "response"(page:"$page",numPages: Math.ceil(productCount/productPageSize) as Integer){
            productList.each {productObj ->
               "product" { 
                  "id"(productObj.id) 
                  "archiveTime"(productObj.archiveTime as Long)
                  "startTime"(productObj.startTime as Long)
                  "stopTime"(productObj.stopTime as Long)
                  "name"(productObj.name as String)
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
        def start, stop
        if(params.startTime != null || params.stopTime != null) {
            if(params.startTime != null){
                log.debug("ValueOf [params.startTime]:"+params.startTime)
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
                stop = new Date().getTime()
            }
        }
        else {
            start = 0
            stop = new Date().getTime()
        }
        
        def ptList
        if(params.search != null) {
            ptList = ProductType.findAllByLastUpdatedBetweenAndIdentifierIlike(start, stop, "%"+params.search+"%" , [sort:"lastUpdated", order:"desc"]);
        }
        else {
            ptList = ProductType.findAllByLastUpdatedBetween(start, stop, [sort:"lastUpdated", order:"desc"]);
        }
        
        if(params.useJson.equals("true") || params.json.equals("1")) {
            render(contentType:"text/json") {[
                "items": ptList.collect { pt -> [
                        "id":pt.id,
                        "identifier":pt.identifier,
                        "title":pt.title,
                        "lastUpdated":pt.lastUpdated,
                        "dataFormat":pt.policy.dataFormat[0]
                ]},
            ]}
        }
        else {
            render(contentType:"text/xml"){
                "items"{
                    for(pt in ptList){
                        "ProductType"{
                            "id"(pt.id)
                            "identifier"(pt.identifier)
                            "title"(pt.title)
                            "lastUpdated"(pt.lastUpdated)
                        }
                    }
                }
            }
        }
    }
   
}
