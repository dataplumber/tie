<?php
session_start ();

include ("operatorLibrary.php"); //SETS GLOBAL VARS + POST/CURL FUNCTION


//$OPERATOR_EDIT_DATASET_URL = $OPERATOR_MANAGERS[$manager_id]."/$OPERATOR_EDIT_DATASET";
$OPERATOR_ZOOKEEPER_SERVICE = $OPERATOR_ZOOKEEPER_URL . "/" . $OPERATOR_ZOOKEEPER_SUFFIX;

if (! isset ( $_SESSION ['operator-username'] )) {
   header ( "HTTP/1.1 530 User Not Logged In" );
} else {
   header ( 'Content-type: application/json' );
   switch ($_REQUEST ['oper']) {
      case 'proxy' :
         $path = $_REQUEST ['path'];
         $data = array ();
         foreach ( $_REQUEST as $key => $value ) {
            if ($key != "oper" || $key != "path")
               $data [$key] = $value;
         }
         $url = $OPERATOR_ZOOKEEPER_SERVICE . "/" . $path;
         $result = do_curl_request ( $url, $data, "GET" );
         print ($result ['content']) ;
         break;
      
      case 'list_storages' :
         $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines";
         $data = array (
               "view" => "children" );
         $result = do_curl_request ( $url, $data, "GET" );
         print ($result ['content']) ;
         break;
      
      case 'list_engines' :
         $storage_name = $_REQUEST ['storage'];
         $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage_name;
         $data = array (
               "view" => "children" );
         $result = do_curl_request ( $url, $data, "GET" );
         print ($result ['content']) ;
         break;
      
      case 'list_data' :
         $storage_name = $_REQUEST ['storage'];
         $engine_name = $_REQUEST ['engine'];
         $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage_name . "/" . $engine_name;
         $data = array (
               "dataformat" => "utf8" );
         $result = do_curl_request ( $url, $data, "GET" );
         print ($result ['content']) ;
         break;
      
      case 'shutdown' :
         if ($_SESSION ['operator-admin']) {
            $storage_name = $_REQUEST ['storage'];
            $engine_name = $_REQUEST ['engine'];
            $generatorFlag = isset($_REQUEST['generate']);
            if(!$generatorFlag)
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage_name . "/" . $engine_name;
            else 
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS/" . $engine_name;
            $result = do_curl_request ( $url, "", "DELETE" );
            if (isset ( $result ) && $result ['status'] == 204)
               print json_encode ( array (
                     "status" => "OK",
                     "content" => "Process successful" ) );
            else
               print json_encode ( array (
                     "status" => "ERROR",
                     "content" => "Process did not complete successfully" ) );
         } else
            print json_encode ( array (
                  "status" => "ERROR",
                  "content" => "User does not have the correct admin rights." ) );
         break;
      
      case 'pause' :
         if ($_SESSION ['operator-admin']) {
            $storage_name = $_REQUEST ['storage'];
            $engine_name = $_REQUEST ['engine'];
            $generatorFlag = isset($_REQUEST['generate']) ? true : false;
            $data = null;
            if(!$generatorFlag) {
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage_name . "/" . $engine_name;
               $data = "PAUSE";
            }
            else { 
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS/" . $engine_name;
               $params = array (
                     "dataformat" => "utf8" );
               $engine_response = do_curl_request ( $url,$params, "GET" );
               if ($engine_response ['content'] != "") {
                  $engine_response_object = json_decode ( $engine_response ['content'] );
                  $engine_data = json_decode($engine_response_object->dataUtf8);
               } else
                  header ( "HTTP/1.1 500 Server Error" );
               if(isset($engine_data) && $engine_data != null) {
                  $engine_data->status = "__PAUSED__";
               }
               $data = str_replace('\\/', '/', json_encode($engine_data));
            }
            if($data != null)
               $result = do_curl_request ( $url, "", "PUT", $data);
            if (isset ( $result ) && $result ['status'] == 200)
               print json_encode ( array (
                     "status" => "OK",
                     "content" => "Process successful" ) );
            else
               print json_encode ( array (
                     "status" => "ERROR",
                     "content" => "Process did not complete successfully" ) );
         } else
            print json_encode ( array (
                  "status" => "ERROR",
                  "content" => "User does not have the correct admin rights." ) );
         break;
      case 'resume' :
         if ($_SESSION ['operator-admin']) {
            $storage_name = $_REQUEST ['storage'];
            $engine_name = $_REQUEST ['engine'];
            $generatorFlag = isset($_REQUEST['generate']) ? true : false;
            
            $data = null;
            if(!$generatorFlag) {
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage_name . "/" . $engine_name;
               $time = time () * 1000;
               $data = "Processing resumed at " . $time;
            }
            else {
               $url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS/" . $engine_name;
               $params = array (
                     "dataformat" => "utf8" );
               $engine_response = do_curl_request ( $url,$params, "GET" );
               if ($engine_response ['content'] != "") {
                  $engine_response_object = json_decode ( $engine_response ['content'] );
                  $engine_data = json_decode($engine_response_object->dataUtf8);
               } else
                  header ( "HTTP/1.1 500 Server Error" );
               if(isset($engine_data) && $engine_data != null) {
                  $engine_data->status = "READY";
               }
               $data = str_replace('\\/', '/', json_encode($engine_data));
            }
            if($data != null)
               $result = do_curl_request ( $url, "", "PUT", $data);

            if (isset ( $result ) && $result ['status'] == 200)
               print json_encode ( array (
                     "status" => "OK",
                     "content" => "Process successful" ) );
            else
               print json_encode ( array (
                     "status" => "ERROR",
                     "content" => "Process did not complete successfully" ) );
         } else
            print json_encode ( array (
                  "status" => "ERROR",
                  "content" => "User does not have the correct admin rights." ) );
         break;
      
      case 'summary' :
         $return_object = array ();
         $return_object ['storages'] = array ();
         
         $url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines";
         $data = array (
               "view" => "children" );
         $response = do_curl_request ( $url, $data, "GET" );
         if ($response ['content'] != "") {
            $storage_response_object = json_decode ( $response ['content'] );
            foreach ( $storage_response_object->children as $storage ) {
               $new_storage_object = array ();
               $new_storage_object ['name'] = $storage;
               $new_storage_object ['engines'] = array ();
               
               $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage;
               $data = array (
                     "view" => "children" );
               $engine_response = do_curl_request ( $engine_url, $data, "GET" );
               if ($engine_response ['content'] != "") {
                  $engine_response_object = json_decode ( $engine_response ['content'] );
                  foreach ( $engine_response_object->children as $engine ) {
                     $engine_object = array ();
                     $engine_object ['name'] = $engine;
                     $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage . "/" . $engine;
                     $data = array (
                           "dataformat" => "utf8" );
                     $engine_response = do_curl_request ( $engine_url, $data, "GET" );
                     if ($engine_response ['content'] != "") {
                        $engine_response_object = json_decode ( $engine_response ['content'] );
                        $engine_object ['status'] = $engine_response_object->dataUtf8;
                     } else
                        header ( "HTTP/1.1 500 Server Error" );
                     array_push ( $new_storage_object ['engines'], $engine_object );
                  }
               } else {
                  header ( "HTTP/1.1 500 Server Error" );
               }
               array_push ( $return_object ['storages'], $new_storage_object );
            }
         } else {
            header ( "HTTP/1.1 500 Server Error" );
         }
         print str_replace('\\/', '/', json_encode ( $return_object ));
         break;
      
      case "storage_summary" :
         $storage = $_REQUEST ['storage'];
         $new_storage_object = array ();
         $new_storage_object ['name'] = $storage;
         $new_storage_object ['engines'] = array ();
         
         $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage;
         $data = array (
               "view" => "children" );
         
         $engine_response = do_curl_request ( $engine_url, $data, "GET" );
         if ($engine_response ['content'] != "") {
            $engine_response_object = json_decode ( $engine_response ['content'] );
            if (isset ( $engine_response_object->children )) {
               foreach ( $engine_response_object->children as $engine ) {
                  $engine_object = array ();
                  $engine_object ['name'] = $engine;
                  $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/engines/" . $storage . "/" . $engine;
                  $data = array (
                        "dataformat" => "utf8" );
                  $engine_response = do_curl_request ( $engine_url, $data, "GET" );
                  if ($engine_response ['content'] != "") {
                     $engine_response_object = json_decode ( $engine_response ['content'] );
                     foreach ( $engine_response_object as $key => $val ) {
                        //$engine_object[$key] = $val;
                     }
                     if (preg_match ( "/Registered for processing at (\d+)/", $engine_response_object->dataUtf8, $matches )) {
                        $engine_object ['status'] = "online";
                        $engine_object ['timeStarted'] = $matches [sizeof ( $matches ) - 1];
                     } else if (preg_match ( "/Processing resumed at (\d+)/", $engine_response_object->dataUtf8, $matches )) {
                        $engine_object ['status'] = "online";
                        $engine_object ['timeStarted'] = $matches [sizeof ( $matches ) - 1];
                     } else if ($engine_response_object->dataUtf8 == "PAUSE" || $engine_response_object->dataUtf8 == "PAUSED") {
                        $engine_object ['status'] = "paused";
                     }
                     $engine_object ['data'] = $engine_response_object->dataUtf8;
                  } else
                     header ( "HTTP/1.1 500 Server Error" );
                  array_push ( $new_storage_object ['engines'], $engine_object );
               }
            }
            print str_replace('\\/', '/', json_encode ( $new_storage_object));
         } else {
            header ( "HTTP/1.1 500 Server Error" );
         }
         
         break;
      
      case "generator_summary" :
         $new_storage_object = array ();
         $new_storage_object ['name'] = "MRF Generators";
         $new_storage_object ['engines'] = array ();
         
         $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS";
         $data = array (
               "view" => "children" );
                  
         $engine_response = do_curl_request ( $engine_url, $data, "GET" );
         if ($engine_response ['content'] != "") {
            $engine_response_object = json_decode ( $engine_response ['content'] );
            if (isset ( $engine_response_object->children )) {
               foreach ( $engine_response_object->children as $engine ) {
                  $engine_object = array ();
                  $engine_object ['name'] = $engine;
                  $engine_url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS/" . $engine;
                  $data = array (
                        "dataformat" => "utf8" );
                  $engine_response = do_curl_request ( $engine_url, $data, "GET" );
                  if ($engine_response ['content'] != "") {
                     $engine_response_object = json_decode ( $engine_response ['content'] );
                     $engine_object = json_decode($engine_response_object->dataUtf8);
                  } else
                     header ( "HTTP/1.1 500 Server Error" );
                  array_push ( $new_storage_object ['engines'], $engine_object );
               }
            }
            //$jobs_url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS";
            print str_replace('\\/', '/', json_encode ( $new_storage_object));
         } else {
            header ( "HTTP/1.1 500 Server Error" );
         }
         
         break;
         
      case "generator_job_queue":
         $new_list_object = array ();
         $new_list_object ['name'] = "MRF Jobs In Queue";
         $new_list_object ['jobs'] = array ();
         
         $federation = (isset($_REQUEST['federation'])) ? $_REQUEST['federation'] : "GIBS";
         $jobs_url = $OPERATOR_ZOOKEEPER_SERVICE . "/manager/queue/generate/$federation";
         $data = array (
               "view" => "children" );
         
         $joblist_response = do_curl_request ($jobs_url, $data, "GET" );

         if ($joblist_response ['content'] != "") {
            $joblist_response_object = json_decode ( $joblist_response ['content'] );
            if (isset ( $joblist_response_object->children )) {
               $job_name_list = $joblist_response_object->children;
               sort($job_name_list);
               if(isset($OPERATOR_MAX_GEN_JOBS)) {
                  $job_name_list = array_slice($job_name_list, 0, $OPERATOR_MAX_GEN_JOBS);
               }
               foreach ( $job_name_list as $job_name ) {
                  $job_object_container = null;
                  $job_url = $OPERATOR_ZOOKEEPER_SERVICE . "/manager/queue/generate/$federation/$job_name";
                  $data = array (
                        "dataformat" => "utf8" );
                  $job_response = null;
                  $job_response = do_curl_request ( $job_url, $data, "GET" );
                  if ($job_response != null && $job_response ['content'] != "") {
                     $job_object_container = json_decode(json_decode( $job_response ['content'])->dataUtf8);
                     foreach($job_object_container->jobs as $job_object) {
                        $job_object->jobName = $job_name;
                        array_push ( $new_list_object ['jobs'], $job_object->jobConfig );
                     }
                  } else {
                     header ( "HTTP/1.1 500 Server Error" );
                  }
               }
            }
            //$jobs_url = $OPERATOR_ZOOKEEPER_SERVICE . "/generators/GIBS";
            print str_replace('\\/', '/', json_encode ( $new_list_object));
         } else {
            header ( "HTTP/1.1 500 Server Error" );
         }
         break;
      default :
         break;
   }
}

?>
