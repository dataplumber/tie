package gov.nasa.horizon.inventory

class Contact {

   Provider provider
   String role
   String firstName
   String middleName
   String lastName
   String email
   String phone
   String fax
   String address
   String notifyType

   static constraints = {
      role(nullable:false)
      firstName(nullable:false)
      middleName(nullable:true)
      lastName(nullable:false)
      email(nullable:false)
      phone(nullable:true)
      fax(nullable:true)
      address(nullable:true)
      notifyType(nullable:true)
   }
   static mapping = {
      id generator: 'sequence', params: [sequence: 'contact_id_seq']
   }
}
