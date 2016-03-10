package gov.nasa.horizon.inventory

class ProductArchiveReference {

   String name
   String path
   String type
   String status
   String description
   
   static belongsTo = [productArchive:ProductArchive]

   static constraints = {
      name(nullable:false)
      path(nullable:false)
      type(nullable:true, inList:["LOCAL-FTP", "LOCAL-HTTP", "LOCAL-LINK", "REMOTE-FTP", "REMOTE-HTTP"])
      status(nullable:false, inList:["ONLINE", "OFFLINE"])
      description(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_archive_reference_id_seq']
   }
}
