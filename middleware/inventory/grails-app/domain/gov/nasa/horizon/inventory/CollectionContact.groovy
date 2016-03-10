package gov.nasa.horizon.inventory

class CollectionContact {

   Collection collection
   Contact contact
   
   static constraints = {
      collection(nullable:false)
      contact(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'collection_contact_id_seq']
   }
}
