package gov.nasa.gibs.inventory

class OgcLayerDimension {

   OgcLayer layer
   String identifier
   String title
   String uom
   String defaultString
   Boolean current
   String value
   String abstractString
   String keywords
   String unitSymbol
   
   static belongsTo = [layer:OgcLayer]

   static constraints = {
   }
   
   static mapping = {
      //defaultString(column:"default")
      //abstractString(column:"abstract")
      id generator: 'sequence', params: [sequence: 'ogc_layer_dimension_id_seq']
   }
}
