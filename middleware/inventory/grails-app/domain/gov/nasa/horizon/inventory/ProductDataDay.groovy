package gov.nasa.horizon.inventory

class ProductDataDay {
	
	Long dataDay
	
	static belongsTo = [product:Product]

    static constraints = {
    }
	
	static mapping = {
		id generator: 'sequence', params: [sequence: 'product_data_day_id_seq']
	 }
}
