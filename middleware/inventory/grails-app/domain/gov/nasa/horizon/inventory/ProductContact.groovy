package gov.nasa.horizon.inventory

class ProductContact {

	Product product
	Contact contact
	
    static constraints = {
    }
    
    static mapping = {
       id generator: 'sequence', params: [sequence: 'product_contact_id_seq']
    }
}
