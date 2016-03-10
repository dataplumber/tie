package gov.nasa.horizon.inventory

import gov.nasa.horizon.security.client.*;
import gov.nasa.horizon.security.client.api.*;
import org.codehaus.groovy.grails.commons.*
import java.security.MessageDigest

/**
 * TODO: Need to revisit this. 
 */
class AuthenticationService {
   def grailsApplication
   boolean transactional = false

   private static SecurityAPI sapi = null;

   //cache looks like [user:[pass:PASSWORD,time:TIMECACHED]]
   def cacheMap = [:]


   boolean isLoggedIn(session) {
      return (session['monitor.user'] != null)
   }

   void logIn(session, user) {
      session['monitor.user'] = user
   }

   void logOut(session) {
      session['monitor.user'] = null
   }

   //IngSystemUser verifyUser(String username, String password) {
   //   println "username, password: $username:$password"
   //   return IngSystemUser.findWhere(name: username, password: Encrypt.encrypt(password))
   //}

   public boolean authenticate(String userName, String password) {
      //return true

      def useCache = (boolean)grailsApplication.config.gov.nasa.horizon.auth.cache.useCache
      def limit = (Integer)grailsApplication.config.gov.nasa.horizon.auth.cache.timeLimit = 2

      log.debug("Authenticating...")
      def start = new Date().getTime();
      if(userName == null || password == null)
         return false;

      def encPass = encrypt(password);


      if(cacheMap.get(userName)?.pass?.equals(encPass))
      {
         if(limit != 0){
            def diff = start - cacheMap.get(userName)?.time
            log.debug("Diff: $diff")
            //if(diff > (limit * 60 * 60 * 1000)) //2 hours in miliseconds
            if(diff > (limit * 60 * 1000)) //2 minutes in miliseconds
            {
               log.debug("invalidating cache, must re-auth")
            }
            else{
               def stop = new Date().getTime();
               log.debug("Auth took " + (stop-start)/1000 + " seconds to complete")
               log.debug("User in Cache, returning true")
               return true;
            }
         }
         else{
            def stop = new Date().getTime();
            log.debug("Auth took " + (stop-start)/1000 + " seconds to complete")
            log.debug("User in Cache, returning true")
            return true;
         }
      }

      //def systemUser = IngSystemUser.findByNameAndPassword(userName, encPass)
      def systemUser = null;

      def host = (String)grailsApplication.config.gov.nasa.horizon.security.host
      def port = (Integer)grailsApplication.config.gov.nasa.horizon.security.port
      def realm = (String)grailsApplication.config.gov.nasa.horizon.security.realm
      def roles = grailsApplication.config.gov.nasa.horizon.security.roles

      sapi =  SecurityAPIFactory.getInstance(host, port)
      if(sapi.authenticate(userName,password,realm))
      {
         log.debug("Size: " + roles.size())
         if(roles != null && roles.size() > 0){
            def found = false;
            log.debug("Authorizing users")
            def allowedRoles = sapi.getRoles(userName,realm)
            log.debug("System Roles: $roles")
            log.debug("AllowedRoles: $allowedRoles")
            for(String s: allowedRoles){
               if(roles.contains(s)){
                  log.debug("Roles contains $s")
                  found = true;
                  break;
               }
            }
            if(!found){
               log.debug("Roles not found, not authorizing")
               return false;
            }
         }

         if(useCache)
            cacheMap.put(userName,[pass:encPass, time:start])
         def stop = new Date().getTime();
         log.debug("*** New Auth took " + (stop-start)/1000 + " seconds to complete")

         return true
      }
      else
      {
         def stop = new Date().getTime();
         log.debug("Auth took " + (stop-start)/1000 + " seconds to complete")
         return false
      }

   }
   static String encrypt(byte[] data, String alg="SHA-1") {
      MessageDigest digest = MessageDigest.getInstance(alg)
      byte[] buf = digest.digest(data)
      StringBuffer sb = new StringBuffer("")
      String hex = "0123456789abcdef"
      byte value
      for (i in buf) {
         sb.append(hex.charAt((i & 0xf0)>>4))
         sb.append(hex.charAt(i & 0x0f))
      }
      return sb.toString()
   }

   static String encrypt(String message, String alg="SHA-1") {
      return encrypt(message.bytes, alg)
   }

}
