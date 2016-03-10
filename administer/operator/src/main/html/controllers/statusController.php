<?php

session_start();

include("operatorLibrary.php"); //SETS GLOBAL VARS + POST/CURL FUNCTION

if(!isset($_SESSION['operator-username'])) {
	header("HTTP/1.1 530 User Not Logged In");
}
else {
	header('Content-type: application/json');
	switch ($_REQUEST['oper']) {
		case 'summary':
			//Managers, inventory, zookeeper, sigevent,solr
			//Managers
			$return_object = array();
			foreach($OPERATOR_MANAGERS as $manager) {
				$manager_object = array();
				$response = null;
				$status = "offline";
				try {
					$response = do_curl_request($manager['url']."/".$OPERATOR_MANAGER_HEARTBEAT, array());
				}
				catch (exception $e) {
					
				}
				if($response != null  && $response['status'] == 200){
					$status = "online";
				}
				
				$manager_object['type'] = "manager";
				$manager_object['name'] = $manager['name'];
				$manager_object['url'] = $manager['url'];
				$manager_object['status'] = $status;
				array_push($return_object, $manager_object);
			}
			//Inventory
			$inventory_object = array();
			$response = null;
			$status = "offline";
			try {
			  $response = do_curl_request($OPERATOR_INVENTORY_URL."/inventory/".$OPERATOR_INVENTORY_HEARTBEAT, array());
			}
			catch (exception $e) {
			}
			if($response != null && $response['status'] == 200){
				$status = "online";
			}
			$inventory_object['type'] = "inventory";
			$inventory_object['url'] = $OPERATOR_INVENTORY_URL."/inventory";
			$inventory_object['status'] = $status;
			array_push($return_object, $inventory_object);			
			
			//Zookeeper
			$zookeeper_object = array();
			$response = null;
			$status = "offline";
			try {
				$response = do_curl_request($OPERATOR_ZOOKEEPER_URL."/".$OPERATOR_ZOOKEEPER_SUFFIX."/", array("view" => "children"), "GET");
			}
			catch (exception $e) {
			}
			if($response != null  && $response['status'] == 200){
				$status = "online";
			}
			$zookeeper_object['type'] = "zookeeper";
			$zookeeper_object['url'] = $OPERATOR_ZOOKEEPER_URL."/".$OPERATOR_ZOOKEEPER_SUFFIX."/";
			$zookeeper_object['status'] = $status;
			array_push($return_object, $zookeeper_object);		
			
			//Solr
			/*
			$solr_object = array();
			$response = null;
			$status = "offline";
			try {
				$response = do_curl_request($OPERATOR_SOLR_URL, array());
			}
			catch (exception $e) {
				
			}
			if($response['status'] == 200 || $response['status'] == 302){
				$status = "online";
			}
			$solr_object['type'] = "solr";
			$solr_object['url'] = $OPERATOR_SOLR_URL;
			$solr_object['status'] = $status;
			array_push($return_object, $solr_object);		
			*/
			
			//Sigevent
			$sig_object = array();
			$response = null;
			$status = "offline";
			try {
			   $response = do_curl_request($OPERATOR_SIGEVENT_URL, array());
			}
			catch (exception $e) {
			
			}
			if($response['status'] == 200 || $response['status'] == 302){
			   $status = "online";
			}
			$sig_object['type'] = "sigevent";
			$sig_object['url'] = $OPERATOR_SIGEVENT_URL;
			$sig_object['status'] = $status;
			array_push($return_object, $sig_object);
			
			//Security
			$sec_object = array();
			$response = null;
			$status = "offline";
			try {
			   $response = do_curl_request($OPERATOR_AUTH_URL."/security", array());
			}
			catch (exception $e) {
			   	
			}
			if($response['status'] == 200 || $response['status'] == 302){
			   $status = "online";
			}
			$sec_object['type'] = "security";
			$sec_object['url'] = $OPERATOR_AUTH_URL."/security";
			$sec_object['status'] = $status;
			array_push($return_object, $sec_object);
			
			print json_encode($return_object);
			break;
	}
	
}

?>
