package gov.nasa.gibs.generate.api;

public class Constants {

   static final public String API_VERSION = "5.0.0";
   static final public String VERSION = "5.0.0";
   static final public String VERSION_DATE = "August 2013";
   static final public String API_VERSION_STR = "MRF Generation API release $API_VERSION";
   static final public String COPYRIGHT = "Copyright 2013, Jet Propulsion Laboratory, Caltech, NASA";
   static final public String SERVER_VERSION_STR = "GIBS MRF Generator Release $VERSION, $VERSION_DATE";

   /* Request list capacity */
   static final public int REQUEST_CAPACITY = 16;
   static final public int REQUEST_CAPACITY_INCR = 8;

   /* Result list capacity */
   static final public int RESULT_CAPACITY = 128;
   static final public int RESULT_CAPACITY_INCR = 64;

   static final public boolean NEED_TYPE = true;

   static final public String PROP_RESTART_DIR = "horizon.restart.dir";
   static final public String PROP_DOMAIN_FILE = "horizon.domain.file";
   static final public String PROP_USER_APP = "horizon.user.application";

}
