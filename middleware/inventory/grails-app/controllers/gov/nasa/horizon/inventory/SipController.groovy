package gov.nasa.horizon.inventory

import gov.nasa.horizon.common.api.serviceprofile.ServiceProfile;
import gov.nasa.horizon.common.api.serviceprofile.ServiceProfileFactory;
import gov.nasa.horizon.common.api.serviceprofile.*
import gov.nasa.horizon.common.api.serviceprofile.jaxb.*

class SipController {

   def authenticationService
   def inventoryService

   def index() {
      redirect(action: addSip, params:params)
   }

   def addSip = {
      //def authenticated = authenticationService.authenticate(params.userName, params.password)//authenticationService.authenticate(userName: params.userName, password: params.password)
      //String sipTest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><message xmlns=\"http://horizon.nasa.gov\"><submission><header><productType>MYRCR143LLDY</productType><productName>RRGlobal_r01c00.2013242.terra.250m</productName><officialName>RRGlobal_r01c00.2013242.terra.250m</officialName><version>1</version><createTime>1377913080000</createTime><status>READY</status><replace>RRGlobal_r01c00.2013242.terra.250m</replace><catalogOnly>false</catalogOnly><operations><operation><agent>MODAPSHandler</agent><operation>ACQUIRED</operation><time><start>1378251874425</start></time></operation></operations></header><metadata><numberOfLines>20</numberOfLines><numberOfColumns>40</numberOfColumns><spatialCoverage><rectangles><rectangle><westLongitude>-180.0</westLongitude><northLatitude>-72.0</northLatitude><eastLongitude>-171.0</eastLongitude><southLatitude>-81.0</southLatitude></rectangle></rectangles></spatialCoverage><history><version>1</version><createDate>1377913080000</createDate><lastRevisionDate>1377913080000</lastRevisionDate><revisionHistory>TBD</revisionHistory><sources><source><productType>MYRCR143LLDY</productType><product>T132422250 T132422110</product></source></sources></history></metadata><ingest><productFiles><IngestProductFile><productFile><file><name>RRGlobal_r01c00.2013242.terra.txt</name><links><link>file:///tmp/tie/staging/data/MYRCR143LLDY/RRGlobal_r01c00.2013242.terra.250m//RRGlobal_r01c00.2013242.terra.txt</link></links><size>377</size><checksum><type>MD5</type><value>4685f262fcd4cd5f126d8c77262d139b</value></checksum><format>ASCII</format></file><type>METADATA</type></productFile></IngestProductFile><IngestProductFile><productFile><file><name>RRGlobal_r01c00.2013242.terra.250m.jpg</name><links><link>file:///tmp/tie/staging/data/MYRCR143LLDY/RRGlobal_r01c00.2013242.terra.250m//RRGlobal_r01c00.2013242.terra.250m.jpg</link></links><size>394186</size><checksum><type>MD5</type><value>f9c32a51d0d906f0b9f22bf1b79e6346</value></checksum><format>JPEG</format></file><type>IMAGE</type></productFile></IngestProductFile><IngestProductFile><productFile><file><name>RRGlobal_r01c00.2013242.terra.250m.jgw</name><links><link>file:///tmp/tie/staging/data/MYRCR143LLDY/RRGlobal_r01c00.2013242.terra.250m//RRGlobal_r01c00.2013242.terra.250m.jgw</link></links><size>96</size><checksum><type>MD5</type><value>7c39e816e0aeacd655ecefc8ee45b9ec</value></checksum><format>JGW</format></file><type>GEOMETADATA</type></productFile></IngestProductFile></productFiles><operationSuccess>false</operationSuccess></ingest></submission><origin address=\"137.79.16.38\" name=\"MODAPS Data Handler\" time=\"1378251874388\"/><target name=\"Manager:MYRCR143LLDY\"/></message>"
      if(!authenticationService.authenticate(params.userName, params.password)){
         response.status =  404
         render "You're not authorized to update the granule status"
         return
      }
      log.debug("Ingest SIP requested")
      if(params.sip == null) {
         response.status = 500
         render "No SIP specified"
      }
      else {
         ServiceProfile serviceProfile
         try {
            serviceProfile = ServiceProfileFactory.getInstance().createServiceProfileFromMessage(params.sip)
         }
         catch(Exception e) {
            log.error("Could not bind SIP xml to ServiceProfile object")
            log.error(e.getMessage())
            response.status = 500
            render "Could not bind SIP xml to ServiceProfile object (Validation did not pass)"
         }
         if(serviceProfile) {
            def start = new Date().getTime();
               // No archive found, ingest sip
            try {
               serviceProfile = inventoryService.ingestSip(serviceProfile)
            }
            catch(Exception e) {
               log.error(e.getMessage())
               response.status = 500
               render e.getMessage()
               return
            }

            def stop = new Date().getTime();
            log.debug("Sip processing took " + (stop-start)/1000 + " seconds to complete")

            response.status =  201
            render(text: serviceProfile.toString(), contentType: "application/xml", encoding: "UTF-8")

         }
      }
   }
   
   def checkSip = {
      log.debug("Check SIP requested")
      if(params.sip == null) {
         response.status = 500
         render "No SIP specified"
      }
      else {
         ServiceProfile serviceProfile
         try {
            serviceProfile = ServiceProfileFactory.getInstance().createServiceProfileFromMessage(params.sip)
         }
         catch(Exception e) {
            log.error("Could not bind SIP xml to ServiceProfile object")
            log.error(e.getMessage())
            response.status = 500
            render "Could not bind SIP xml to ServiceProfile object (Validation did not pass)"
         }
         if(serviceProfile) {
            def start = new Date().getTime();
            def result = false
            try {
               result = inventoryService.checkSip(serviceProfile)
            }
            catch(Exception e) {
               log.error(e.getMessage())
               response.status = 500
               render e.getMessage()
               return
            }

            def stop = new Date().getTime();
            log.debug("Sip check took " + (stop-start)/1000 + " seconds to complete")

            if (!result) {
               log.info("Sip did not pass check, responding with 403")
               response.status = 403
               render "Not a valid Sip"
               return
            }
            else {
               log.info("Sip is OK")
               response.status = 200
               render "Sip is OK"
               return
            }
         }
      }
   }
}
