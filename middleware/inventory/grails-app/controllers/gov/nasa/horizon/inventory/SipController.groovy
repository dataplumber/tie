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
