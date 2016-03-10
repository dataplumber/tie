package gov.nasa.gibs.inventory

class Projection {

   String name
   String epsgCode
   String wg84Bounds
   String nativeBounds
   String ogc_crs
   String description

   static constraints = {
      name(nullable:false)
      epsgCode(nullable:false)
      wg84Bounds(nullable:false)
      nativeBounds(nullable:false)
      ogc_crs(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'projection_id_seq']
   }
}
