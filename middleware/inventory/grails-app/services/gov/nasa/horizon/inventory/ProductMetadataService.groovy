package gov.nasa.horizon.inventory

import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.common.api.serviceprofile.jaxb.ProductFileJaxb;
import gov.nasa.gibs.inventory.*

class ProductMetadataService {

   static transactional = true

   def grailsApplication

   // Returns map [sip, product]
   def storeSip = { ServiceProfile sip ->
      //Extract info from sip
      def ptName = sip.getSubmission().getHeader().getProductType()
      def productName = sip.getSubmission().getHeader().getProductName()

      log.debug("Updating ingest SIP for product: $productName and product type: $ptName")

      //Empty Product creation
      Product product = null

      //Query DB for needed objects from extracted sip data
      def pt = ProductType.findByIdentifier(ptName)
      def policy = ProductTypePolicy.findByPt(pt)
      def locationPolicy = ProductTypeLocationPolicy.findByPtAndType(pt, grailsApplication.config.constants.locationPolicy.ARCHIVE)

      if(!pt || !policy || !locationPolicy) {
         throw new Exception("Could not find Product Type entry, policy, or archive location policy for: $ptName")
      }

      //Generate the archive tag in the sip and clone ingest tag contents
      sip = createArchiveSection(sip)

      //Invoke delete list creation in sip AND do actual deletion of existing product information
      //TODO do something about meta history (shouldn't delete those records AND should keep the same productID if a delete did happen
      sip = createDeletes(sip)

      //Lets create and save the new product and all associated tables, then return the newly saved product (now with an id)
      product = createProductFromSip(sip)

      //Determine archive path for the product then invoke destination creation
      def archiveBasePath = locationPolicy.basePath

      sip = createDestinations(sip, archiveBasePath, policy)

      sip.getSubmission().getHeader().setProductTypeInventoryId(pt.id)
      sip.getSubmission().getHeader().setProductInventoryId(product.id)

      return [sip:sip, product:product]
   }

   def createArchiveSection = {ServiceProfile sip ->
      SPIngest ingest = sip.getSubmission().getIngest()
      SPIngest archive = sip.getSubmission().createArchive()
      sip.getSubmission().setArchive(archive)

      def ingestProductFiles = ingest.getIngestProductFiles()
      ingestProductFiles.each { ingestProductFile ->
         //Create an archive product file
         SPIngestProductFile archiveProductFile = archive.createIngestProductFile()
         archive.addIngestProductFile(archiveProductFile)

         //Copy the product file from ingest over to archive
         archiveProductFile.setProductFile(ingestProductFile.getProductFile())
      }
      return sip
   }

   def createProductFromSip = { ServiceProfile sip ->
      def header = sip.getSubmission().getHeader()
      def metadata = sip.getSubmission().getMetadata()
      def ingestProductFiles = sip.getSubmission().getIngest().getIngestProductFiles()

      def pt = ProductType.findByIdentifier(header.getProductType())
      def policy = ProductTypePolicy.findByPt(pt)
      def locationPolicy = ProductTypeLocationPolicy.findByPtAndType(pt, grailsApplication.config.constants.locationPolicy.ARCHIVE)
      def archiveBasePathAppendType = policy.basePathAppendType


      //Define fields to populate new product

      //Header tag fields
      String name = header.getProductName()
      Date createTime = header.getCreateTime()
      Long createTimeLong = (createTime) ? createTime.getTime() : null
      Integer versionNum = header.getVersion()

      //Metadata tag fields
      Date startTime = metadata.getProductStartTime()
      Long startTimeLong = (startTime) ? startTime.getTime() : null
      Date stopTime = metadata.getProductStopTime()
      Long stopTimeLong = (stopTime) ? stopTime.getTime() : null
	  String partialId = metadata.getPartialId()

      //Date archiveTime = new Date()
      Long archiveTimeLong = null //archiveTime.getTime()

      //Inventory generate fields
      String status = "OFFLINE"
      String rootPath = locationPolicy.basePath
      String relPath = appendArchiveSubDir(archiveBasePathAppendType, startTime, name, null)

      log.debug "Creating new product $name with the following properties"
      log.debug "name: $name"
      log.debug "startTime: $startTime"
      log.debug "stopTime: $stopTime"
      log.debug "createTime: $createTime"
      //log.debug "archiveTime: $archiveTime"
      log.debug "versionNum: $versionNum"
      log.debug "status: $status"
      log.debug "rootPath: $rootPath"
      log.debug "relPath: $relPath"
      log.debug "pt: $pt"

      Product p = new Product(
            name: name,
            startTime: startTimeLong,
            stopTime: stopTimeLong,
            createTime: createTimeLong,
            archiveTime: archiveTimeLong,
            versionNum: versionNum,
            status: status,
			partialId: partialId,
            rootPath: rootPath,
            relPath: relPath,
            pt: pt
            )

      if(!p.save(flush:true))
         throw new Exception("Could not create new product row for: $name. Missing required fields in SIP")


      //Product archive generation
      log.debug "Creating product archive rows for product $name"
      ingestProductFiles.each { SPIngestProductFile ingestProductFile ->
         def productFile = ingestProductFile.getProductFile()

         //TODO add ingest-time start stop fields added to SIP

         //Fields to populate
         String fileType = productFile.getFileType().toString()
         Integer fileSize = productFile.getFile().getSize()
         Boolean compressFlag = (productFile.getFile().getCompressionType()) ? true : false
         String checksum = productFile.getFile().getChecksumValue()
         String fileName = productFile.getFile().getName()
         String fileStatus = "OFFLINE"

         def productArchive = new ProductArchive(
               type:fileType,
               fileSize:fileSize,
               compressFlag:false,
               checksum:checksum,
               name:fileName,
               status:fileStatus,
               product:p
               )

         if(!productArchive.save(flush:true))
            throw new Exception("Couldn't save product archive to DB: $fileName")

         if(policy.multiDayLink) {
            List<String> links = createLinks(name, startTime, rootPath, archiveBasePathAppendType, policy.multiDay)
            links.each {link ->
               def fullPath = "file://$link/$fileName"
               def productArchiveRef = new ProductArchiveReference(
                     name:name,
                     path:fullPath,
                     type:"LOCAL-LINK",
                     status:"OFFLINE",
                     description:"Link for product $name and file $fileName",
                     productArchive: productArchive
                     )
               if(!productArchiveRef.save(flush:true))
                  throw new Exception("Couldn't save product archive ref to DB: $fileName")

            }
         }
      }

      //Add meta history fields
      log.debug "Creating product meta history rows for product $name"
      def metaHistory = metadata.getProductHistory()

      //Fields to populate
      Integer versionId = metaHistory.getVersion()
      Date creationDate = metaHistory.getCreateDate()
      Long creationDateLong = (creationDate) ? creationDate.getTime() : null
      Date lastRevisionDate = metaHistory.getLastRevisionDate()
      Long lastRevisionDateLong = (lastRevisionDate) ? lastRevisionDate.getTime() : null
      String revisionHistory = metaHistory.getRevisionHistory()

      def productMetaHistory = new ProductMetaHistory(
            versionId:versionId,
            creationDate:creationDateLong,
            lastRevisionDate:lastRevisionDateLong,
            revisionHistory:revisionHistory,
            product:p
            )
      if(!productMetaHistory.save(flush:true))
         throw new Exception("Couldn't save meta history to DB: $fileName")

      
      // Source parsing, granule(not product) creation, and linkage between product and granule
      def sources = metaHistory.getSourceProducts()
      sources.each {SPSourceProduct source ->
         //Fields to populate
         String sourceProduct = source.getProduct()
         String sourceProductType = source.getProductType()
         String sourceRepo = source.getMetadataRepo()

         Dataset dataset = Dataset.findByShortName(sourceProductType)
         
         if(!dataset) {
            dataset = new Dataset(
                      remoteDatasetId: sourceProductType,
                      shortName: sourceProductType,
                      longName: sourceProductType,
                      metadataEndpoint: sourceRepo,
                      versionLabel: "N/A",
                      description: "Auto generated dataset from SIP for "+sourceProduct
                     )
            dataset.save(flush:true)
         }
         //Validate existance of dataset
         if(dataset){
            //Check existance of granule, create if not there
            Granule granule = Granule.findByRemoteGranuleUr(sourceProduct)
            if(!granule) {
               granule = new Granule(
                     remoteGranuleUr: sourceProduct,
                     metadataEndpoint: sourceRepo,
                     dataset: dataset
                     )
               granule.save(flush:true)
            }
            //Associate both product type and product to dataset and granule
            //First dataset to pt (check if already exists)
            DatasetImagery di = DatasetImagery.findByPtAndDataset(pt, dataset)
            if(!di) {
               di = new DatasetImagery(pt:pt, dataset:dataset)
               if(!di.save(flush:true))
                  throw new Exception("Could not save ProductType/Dataset association for ProductType $pt.identifier and Dataset $sourceProductType")
            }
            //Next granule to product
            GranuleImagery gi = new GranuleImagery(granule:granule, product:p)
            if(!gi.save(flush:true))
               throw new Exception("Could not save Product/Granule association for Product $name and Granule $sourceProduct")
         }
         else {
            //Maybe just log an error and move on instead of an exception?
            throw new Exception("Dataset $sourceProduct not found for product $name and error occured when generating a new one")
         }
      }
	  
	  def days = metadata.getDataDays()
	  if(days.size() > 0) {
		  days.each { day ->
			  ProductDataDay dayObj = new ProductDataDay(dataDay:day.getTime(), product:p)
			  if(!dayObj.save(flush:true))
		  		throw new Exception("Could not save Product Data Day for product $name")
		  }

	  }
	  
	  

      //TODO add extras (including most fields in the metadata tag inside the SIP)

      //TODO add logic for product reference and product archive references

      return p
   }

   def createDestinations = { ServiceProfile sip, String basePath, ProductTypePolicy policy ->
      //Figure out the base path
      def createTime = sip.getSubmission().getHeader().getCreateTime()
      def startTime = sip.getSubmission().getMetadata().getProductStartTime()

      def archiveBasePathAppendType = policy.basePathAppendType
      def productName = sip.getSubmission().getHeader().getProductName()
      def archiveSubDir = appendArchiveSubDir(archiveBasePathAppendType, startTime, productName, null)
      def archivePath = [basePath, archiveSubDir].join(File.separator)

      //Getting archive portion of sip
      SPIngest ingest = sip.getSubmission().getIngest()
      SPIngest archive = sip.getSubmission().getArchive()

      def archiveProductFiles = archive.getIngestProductFiles()
      archiveProductFiles.each { archiveProductFile ->

         def fileName = archiveProductFile.getProductFile().getFile().getName()
         SPFileDestination destination = archiveProductFile.createFileDestination()
         destination.setLocation("file://$archivePath/$fileName")

         if(policy.multiDayLink) {
            List<String> links = createLinks(productName, startTime, basePath, archiveBasePathAppendType, policy.multiDay)
            links.each {link ->
               destination.addLink("file://$link/$fileName")
            }
         }
         //Finally insert destination with the product file object
         archiveProductFile.setFileDestination(destination)
      }

      sip.getSubmission().setArchive(archive)
      return sip
   }

   def createLinks = {String productName, Date startTime, String basePath, String basePathAppendType, Integer numDays ->
      List<String> links = []

      TimeZone gmt = TimeZone.getTimeZone("GMT");
      GregorianCalendar cal = new GregorianCalendar(gmt);
      cal.setTime(startTime);

      for(i in 0..<numDays) {
         cal.add(GregorianCalendar.DATE, 1)
         def archiveSubDir = appendArchiveSubDir(basePathAppendType, cal.getTime(), productName, null)
         links.add([basePath, archiveSubDir].join(File.separator))
      }
      return links
   }

   def createDeletes = {ServiceProfile sip ->
      SPHeader header = sip.getSubmission().getHeader()
      SPIngest archive = sip.getSubmission().getArchive()

      def ptName = header.getProductType()
      ProductType pt = ProductType.findByIdentifier(ptName)
      ProductTypePolicy policy = ProductTypePolicy.findByPt(pt)
      ProductTypeLocationPolicy locationPolicy = ProductTypeLocationPolicy.findByPtAndType(pt, grailsApplication.config.constants.locationPolicy.ARCHIVE)
      
      //TODO: For some reason header.getReplace() always returns null. Look into it
      def replaceProductName = header.getProductName()

      if(!pt)
         throw new Exception("Product Type $ptName not found during deletes lookup")

      Product replaceProduct = Product.findByPtAndName(pt, replaceProductName)
      if(replaceProduct) {
         log.info "Found product $replaceProductName for replacement. Removing."

         def startTime = new Date(replaceProduct.startTime)
         def archiveBasePathAppendType = policy.basePathAppendType
         def archiveBasePath = locationPolicy.basePath

         def archiveSubDir = appendArchiveSubDir(archiveBasePathAppendType, startTime, replaceProduct.name, null)
         def archivePath = [
            archiveBasePath,
            archiveSubDir
         ].join(File.separator)

         def productFiles = ProductArchive.findAllByProduct(replaceProduct)

         productFiles.each { ProductArchive productFile ->
            def fileName = productFile.name
            def deletePath = [archivePath,fileName].join(File.separator)

            SPFileDestination deleteObj = archive.createDestination()
            //deleteObj.addLink(deletePath)
            deleteObj.setLocation(deletePath)
            archive.addDelete(deleteObj)
            def productLinks = ProductArchiveReference.findAllByProductArchive(productFile)
            productLinks.each { ProductArchiveReference productLink ->
               def linkPath = productLink.path
               SPFileDestination deleteLinkObj = archive.createDestination()
               //deleteObj.addLink(deletePath)
               deleteLinkObj.setLocation(linkPath)
               archive.addDelete(deleteLinkObj)

            }
         }

         //GIBS specific removal code
         def granuleImageryRows = GranuleImagery.findAllByProduct(replaceProduct)
         granuleImageryRows.each { row ->
            row.delete(flush:true)
         }
         replaceProduct.delete(flush:true)
      }

      //archive.addDelete(null)
      return sip
   }

   def appendArchiveSubDir = {String basePathAppendType, Date startTime, String productName, Integer cycle ->
      String versionString = "";

      if (basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR.toString())) {
         //Date startTime = this.granule.getStartTime();
         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);
         return ""+versionString  +cal.get(Calendar.YEAR) +"/"+productName;

      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR_DOY.toString())) {
         //Date startTime = this.granule.getStartTime();
         if (startTime==null) startTime = new Date();
         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);
         String zero="";
         if(cal.get(Calendar.DAY_OF_YEAR) < 100)
            zero="0";
         if(cal.get(Calendar.DAY_OF_YEAR) < 10)
            zero="00";

         return ""+versionString  +cal.get(Calendar.YEAR) + "/"  +zero+ cal.get(Calendar.DAY_OF_YEAR) + "/" + productName;

      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR_MONTH_DAY.toString())) {
         if (startTime==null) startTime = new Date();

         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);

         String dayZero="";
         String monthZero="";
         if(cal.get(Calendar.DAY_OF_MONTH) < 10)
            dayZero="0";
         if(cal.get(Calendar.MONTH)+1 < 10){
            monthZero="0";
         }

         return ""+versionString  +cal.get(Calendar.YEAR) + "-"  +monthZero.toString()+ (cal.get(Calendar.MONTH) +1) + "-"  +dayZero.toString()+ cal.get(Calendar.DAY_OF_MONTH);

      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.BATCH.toString())) {
         // need batch number from sip header so this is filled at InventoryImpl
         return ""+versionString ;
      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR_WEEK.toString())) {
         //week is value 1-52
         //Date startTime = this.granule.getStartTime();
         if (startTime==null) startTime = this.granule.getCreateTime();
         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);
         String zero = "0";
         if(cal.get(Calendar.WEEK_OF_YEAR) > 10)
         {
            zero="";
         }

         return ""+versionString  +cal.get(Calendar.YEAR) + "/"+zero+  + cal.get(Calendar.WEEK_OF_YEAR);
      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR_MONTH.toString())) {
         //week is value 1-52
         //Date startTime = this.granule.getStartTime();
         if (startTime==null) startTime = this.granule.getCreateTime();
         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);
         String zero = "0";
         if(cal.get(Calendar.MONTH)+1 >= 10)
         {
            zero="";
         }

         return ""+versionString  +cal.get(Calendar.YEAR) + "/" + zero + (cal.get(Calendar.MONTH)+1);
      } else if (basePathAppendType.equals(grailsApplication.config.constants.appendType.CYCLE.toString())) {
         try {
            //Integer cycle = metadata.getCycle();
            if (cycle==null) throw new Exception("Cycle null!");
            return versionString + "c"+String.format("%03d",cycle);
         } catch (NullPointerException npe) {
            throw new Exception("Cycle NPE!");
         } catch (Exception e) {
            throw new Exception(e.getMessage());
         }
      } else if(basePathAppendType.equals(grailsApplication.config.constants.appendType.YEAR_FLAT.toString())) {
         //Date startTime = this.granule.getStartTime();

         //if (startTime==null) startTime = product.createTime;
         TimeZone gmt = TimeZone.getTimeZone("GMT");
         Calendar tempCal = Calendar.getInstance();
         tempCal.setTime(startTime);
         GregorianCalendar cal = new GregorianCalendar(gmt);
         cal.setTime(startTime);
         return ""+versionString  +cal.get(Calendar.YEAR);

      } else {
         return null;
      }
   }

   def mapOptionalAttr = {Product p,  key, value ->
      if (value==null) {
         return;
      }
      if(key.equals("westLongitude")){
         key = "westernmostLongitude";
      }
      if(key.equals("eastLongitude")){
         key = "easternmostLongitude";
      }
      if(key.equals("southLatitude")){
         key = "southernmostLatitude";
      }
      if(key.equals("northLatitude")){
         key = "northernmostLatitude";
      }

      //Get the elementDD for the specified key
      def element = ElementDd.findByShortName(key);
      if(!element) {
         throw Exception("Could not find ElementDD named: " + key);
      }

      //Get the associated ptElement
      ProductTypeElement pe = ProductTypeElement.findByElement(element);

      if(!pe) {
         log.debug("Product element '" + key+"' not found in productTypeElement set for this product's productType.");
         return;
      }
      // datatype is dictated by dictionary
      String type = element.type;
      //System.out.println("translateOptMetadata: "+keyName+"="+keyValue+" "+type);
      log.info("translateOptMetadata: "+keyName+"="+keyValue+" "+type);
      if (type.equals("character"))
         p.addToProductCharacter(value:value.toString(), pe:pe);
      else if (type.equals("integer"))
         p.addToProductInteger(value:Integer.valueOf(keyValue.toString()), pe:pe);
      else if (type.equals("real")){
         p.addToProductReal(value: Double.valueOf(keyValue.toString()), pe:pe);
      }
      else if (type.equals("date"))
         p.addToProductDatetime(value:(Date)keyValue, pe:pe);
      else if (type.equals("time"))
         p.addToProductDatetime(value:(Date)keyValue, pe:pe);
   }

}

