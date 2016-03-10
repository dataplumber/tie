package gov.nasa.gibs.distribute.subscriber.api;

//import gov.nasa.horizon.distribute.subscriber.model.*;
import gov.nasa.horizon.inventory.model.*;

import java.util.*;

import java.util.Date;

public interface DataSubscriber {

	/*
	 * @method list - Interface which will query for granules add to a dataset since lastRunTime
	 * @param dataset - the dataset object to which we will add granule name/information 
	 * @param lastRunTime -the start time of the subscribers last run/query
	 */
	public List<Product> list(ProductType productType, Date lastRunTime) ;
	
	public List<Product> listRange(ProductType productType, Date start, Date end);
}
