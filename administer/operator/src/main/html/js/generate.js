/**
 * 
 */

var MAX_JOBS_DISPLAYED = 3;

function createGenContainer() {
   var mrfContainer = $("<div class='operator-section operator-section-half operator-storage-container'>\
         <h1><span class='operator-storage-label'>MRF Generation</span><div style='clear:both;'></div></h1>\
         <div class='operator-content'>\
            <div class='operator-mrf-joblist'>\
               <h2>Job Queue</h2>\
            </div>\
            <div class='operator-storage-engine-container'>\
               <h2>Engines</h2>\
            </div>\
         </div>\
      </div>\
   ");
   $(mrfContainer).data("name", name);
   
   $.ajax({
      url:"controllers/zkController.php",
      data:{oper:"generator_job_queue"},
      type:"GET",
      success:function(responseObject) {
         var jobContainer = mrfContainer.find(".operator-mrf-joblist");
         if (responseObject && responseObject.jobs.length == 0) {
            jobContainer.append("<span style='color:red; margin: 10px; font-size:12px;'>No Jobs Found</span>");
         }
         else{
            createGenJobList(responseObject.jobs, jobContainer);
         }
      },
      error:function(responseObject){displayDialog("Could not retrieve generator job list. Is ZK running?");}
   });
   
   $.ajax({
      url:"controllers/zkController.php",
      data:{oper:"generator_summary"},
      type:"GET",
      success:function(responseObject){
         mrfEngineListContainer = mrfContainer.find(".operator-storage-engine-container");
         if(responseObject.engines.length == 0) {
            mrfEngineListContainer.append("<span style='color:red; margin: 10px; font-size:12px;'>No Engines Running.</span>");
         }
         $.each(responseObject.engines, function(index, engineObject) {
            if(engineObject.timeStarted) 
               lastUpdatedString = formatUTCDate(parseInt(engineObject.timeStarted));//lastUpdated.toDateString()+" "+lastUpdated.getHours()+":"+lastUpdated.getMinutes()+":"+lastUpdated.getSeconds()
            else lastUpdatedString = "N/A";

            //startedAt.toDateString()+" "+startedAt.getHours()+":"+startedAt.getMinutes()+":"+startedAt.getSeconds()
            var newEngineContainer = createGenEngine(engineObject);
            mrfEngineListContainer.append(newEngineContainer);
            
            if(engineObject.status == "READY") {
               newEngineContainer.find(".operator-engine-resume").hide();
            }
            else if(engineObject.status == "__PAUSED__") {
               newEngineContainer.find(".operator-engine-resume").show();
               newEngineContainer.find(".operator-engine-pause").hide();
            }
            else {
               newEngineContainer.find(".operator-engine-pause").hide();
               newEngineContainer.find(".operator-engine-resume").show();
            }
            newEngineContainer.data("status", {engine:engineObject.name, storage:$(mrfContainer).data("name")});
            
            $(".operator-engine-pause", newEngineContainer).click(function() {
               var storage = newEngineContainer.data("status").storage;
               var engine = newEngineContainer.data("status").engine;
               var input = confirm("Are you certain you would like to pause this engine?");
               if(input) {
                  $.ajax({
                     url: "controllers/zkController.php",
                     data: {oper:"pause", storage:storage, engine:engine, generate:true},
                     type: "GET",
                     success:function(responseObject) {
                        if(responseObject.status == "OK") {
                           refreshStorage();
                        }
                        else {
                           displayDialog(responseObject.content);
                        }
                     },
                     error: function(){
                        
                     }
                  });
               }  
            });
            $(".operator-engine-resume", newEngineContainer).click(function() {
               var storage = newEngineContainer.data("status").storage;
               var engine = newEngineContainer.data("status").engine;
               var input = confirm("Are you certain you would like to resume this engine?");
               if(input) {
                  $.ajax({
                     url: "controllers/zkController.php",
                     data: {oper:"resume", storage:storage, engine:engine, generate:true},
                     type: "GET",
                     success:function(responseObject) {
                        if(responseObject.status == "OK") {
                           refreshStorage();
                        }
                        else {
                           displayDialog(responseObject.content);
                        }
                     },
                     error: function(){
                        
                     }
                  });
               }
            });
            $(".operator-engine-stop", newEngineContainer).click(function() {
               var storage = newEngineContainer.data("status").storage;
               var engine = newEngineContainer.data("status").engine;
               var input = confirm("Are you certain you would like to shut down (kill) this engine?");
               if(input) {
                  $.ajax({
                     url: "controllers/zkController.php",
                     data: {oper:"shutdown", storage:storage, engine:engine, generate:true},
                     type: "GET",
                     success:function(responseObject) {
                        if(responseObject.status == "OK") {
                           refreshStorage();
                        }
                        else {
                           displayDialog(responseObject.content);
                        }
                     },
                     error: function(){
                        
                     }
                  });
               }
            });
         });
      },
      error:function(responseObject){displayDialog("Could not retrieve generator engine list. Is ZK running?");}
   });
   
   return mrfContainer;
}

function createGenEngine(engineObj) {
   
   var imageURL = "";

   if(engineObj.status == "READY") {
      imageURL = "images/greenlight.png";
      status = "Online";
   }
   else if (engineObj.status == "__PAUSED__" || engineObj.status == "__PAUSE__"){
      imageURL = "images/yellowlight.png";
      status = "Paused";
   }
   else {
      imageURL = "images/redlight.png";
      status = "Offline";
   }
   
   var engineContainer =  $("<div class='operator-engine-container'>\
         <div class='operator-engine-button-container'>\
            <button class='operator-engine-pause clean-gray'>\
               <span alt='Pause Engine' class='operator-button-icon ui-icon ui-icon-pause'></span> <span class='operator-button-text'>Pause</span>\
            </button>\
            <button class='operator-engine-resume clean-gray' style='display: none;'>\
               <span class='operator-button-icon ui-icon ui-icon-play'></span> <span class='operator-button-text'>Resume</span>\
            </button>\
            <button class='operator-engine-stop clean-gray'>\
               <span class='operator-button-icon ui-icon ui-icon-closethick'></span> <span class='operator-button-text'>Stop</span>\
            </button>\
         </div>\
         <img class='operator-engineIcon' src='"+imageURL+"'>\
         <div class='operator-engineSummaryInfo'>\
            <span style='font-weight: bold; display: block;'>"+engineObj.name+"<br></span>\
            <div class='operator-engine-status-container'>\
               <span>Status: </span><span class='operator-engine-status operator-engine-online'>"+status+"</span>\
            </div>\
            <span>Started at: "+formatUTCDate(engineObj.started)+"</span>\
         </div>\
         <div class='operator-mrf-currentjobs' style='margin-top: 5px;'>\
            <h2 style='border-bottom: 0 !important; margin-bottom: 1px;'>Current Jobs</h2>\
         </div>\
         <div class='operator-mrf-recentjobs' style='margin-top: 5px;'>\
         <h2 style='border-bottom: 0 !important; margin-bottom: 1px;'>Recent Jobs</h2>\
      </div>\
   </div>");
   
   // Process current jobs
   var currentJobsContainer = engineContainer.find(".operator-mrf-currentjobs");
   if(engineObj.currentJobs.length == 0) {
      currentJobsContainer.append("<span style='margin: 10px; font-size:12px;'>Not working on any jobs.</span>");
   }
   else {
      createGenJobList(engineObj.currentJobs, currentJobsContainer);
   }
   
   // Process recent jobs
   var recentJobsContainer = engineContainer.find(".operator-mrf-recentjobs");
   if(engineObj.recentJobs.length == 0) {
      recentJobsContainer.append("<span style='margin: 10px; font-size:12px;'>No history of job.</span>");
   }
   else {
      createGenJobList(engineObj.recentJobs.reverse(), recentJobsContainer);
   }
   
   return engineContainer;
}

function createGenJobList(jobList, container) {
   var hiddenFlag = false;
   $.each(jobList, function(index, jobObj){
      hiddenFlag = (index >= MAX_JOBS_DISPLAYED);
      container.append(createGenJob(jobObj, index+1, hiddenFlag));
   });
   if(hiddenFlag) {
      container.append($("<button class='clean-gray showMore'>Show More</button>"));
      container.append("<div style='clear:both;'/>");
      container.find(".showMore").toggle(function(){
         $(this).siblings(".hiddenFlag").show();
         $(this).text("Show Less");
      }, function() {
         $(this).siblings(".hiddenFlag").hide();
         $(this).text("Show More");
      });
   }
}

function createGenJob(jobObj, index, hiddenFlag) {
   // Figure out what the title of the preview will be
   var maxTitleWidth = 26;
   var title = jobObj.parameter;
   if(title.length > maxTitleWidth) {
      title = title.substr(0,maxTitleWidth-3)+"...";
   }
   
   // Format date of the job for preview
   var dateString = formatShortDate(jobObj.dateOfData);
   
   var hiddenFlagText = "";
   if(hiddenFlag) {
      hiddenFlagText = " hiddenFlag";
   }
   
   var jobContainer = $("<div class='operator-job-item"+hiddenFlagText+"'>\
         <span style='font-size: 11px;'>"+index+"- </span> <span class='operator-job-title'><b>"+title+"</b> <span style='color: #E47911; margin-left: 4px;'>"+dateString+"</span> - "+jobObj.inputs.length+" product(s)</span>\
         <button class='clean-gray operator-mrf-details-button' style='float: right;'>DETAILS</button>\
         <div class='operator-jobDetails operator-roundAll' style='display: none;'>\
            <div class='ui-icon ui-icon-circle-close operator-jobDetailsCloseButton'></div>\
            <div class='operator-mrf-jobdetails-title' style=''>MRF Job Details</div>\
            <table style='clear: both;'>\
               <tbody>\
                  <tr>\
                     <td class='operator-mrf-detailslabel'><span style='font-weight: bold;'>Product Name: </span></td>\
                     <td><span class='operator-storageConstant'>"+jobObj.mrfProductName+"</span></td>\
                  </tr>\
                  <tr>\
                     <td class='operator-mrf-detailslabel'><span style='font-weight: bold;'>Date: </span></td>\
                     <td><span class='operator-storageConstant'>"+formatShortDate(jobObj.dateOfData)+"</span></td>\
                  </tr>\
                  <tr>\
                     <td class='operator-mrf-detailslabel'><span style='font-weight: bold; margin-right: 5px;'>Inputs: </span></td>\
                     <td><span class='operator-storageConstant operator-mrf-inputlist'>\
                         <ul></ul>\
                     </span></td>\
                  </tr>\
               </tbody>\
            </table>\
         </div>\
      </div>");
   
   //Add inputs
   $.each(jobObj.inputs, function(index, input){
      jobContainer.find(".operator-mrf-inputlist ul").append($("<li>"+input+"</li>"));
   });
   
   //Add rest of metadata
   var metadataTable = jobContainer.find(".operator-jobDetails>table");
   
   var jobKeys = Object.keys(jobObj);
   $.each(jobKeys, function(index, key){
      if(key != "mrfProductName" && key != "dateOfData" && key != "inputs" && jobObj[key] != null) {
         var value = jobObj[key];
         if(key == "timePosted" || key == "timeStarted" || key == "timeCompleted") {
            value = formatUTCDate(jobObj[key]);
         }
         var label = camelCaseToNormal(key);
         if(key == "sourceEPSG")
        	 label = "Source EPSG";
         if(key == "targetEPSG")
        	 label = "Target EPSG";
         var newRow = $("<tr>\
                     <td class='operator-mrf-detailslabel'><span style='font-weight: bold;'>"+label+": </span></td>\
                     <td><span class='operator-storageConstant'>"+value+"</span></td>\
                  </tr>");
         metadataTable.append(newRow);
      }
   });
   
   jobContainer.find(".operator-mrf-details-button").click(function() {
      $(this).siblings(".operator-jobDetails:hidden").show("drop", "fast");
      //$(this).parents(".operator-section").find(".operator-storageDetails:visible").hide("drop", "fast");
   });
   jobContainer.find(".operator-jobDetailsCloseButton").click(function(){
      $(this).parent(":visible").hide("drop", "fast");
   });   
   /*jobContainer.find(".operator-jobDetails").draggable({
      handle:".operator-mrf-jobdetails-title",   
      cursor:"move",
      stop: function(event, ui) {
         $(ui.element).css({'z-index': 1000});
      }});*/
   return jobContainer;
}

function formatShortDate(input, options, rowObject) {
   var d = new Date(parseInt(input));
   var dateString = d.getUTCFullYear() + "-";
  if (d.getUTCMonth() < 9) {
      dateString += "0";
  }
  dateString += (d.getUTCMonth() + 1) + "-";
  if (d.getUTCDate() < 10) {
      dateString += "0";
  }
  dateString += d.getUTCDate();

  if(isNaN(d.getUTCDate()))
   dateString = "";
  else {
	  dateString += " UTC"
  }
  
  return dateString;
}

function formatUTCDate(input, options, rowObject) {
	var d = new Date(parseInt(input));
	var dateString = d.getUTCFullYear() + "-";
  if (d.getUTCMonth() < 9) {
      dateString += "0";
  }
  dateString += (d.getUTCMonth() + 1) + "-";
  if (d.getDate() < 10) {
      dateString += "0";
  }
  dateString += d.getUTCDate();
  
  dateString += " "; 
  if (d.getUTCHours() < 9) {
      dateString += "0";
  }
  dateString += d.getUTCHours() + ":";    
  if (d.getUTCMinutes() < 9) {
      dateString += "0";
  }
  dateString += d.getUTCMinutes() + ":";    
  if (d.getUTCSeconds() < 9) {
      dateString += "0";
  }
  dateString += d.getUTCSeconds();
  
  if(isNaN(d.getUTCDate()))
  	dateString = "";
  else {
	  dateString += " UTC";
  }
  return dateString;
	//return dateObject.format("Y-mm-dd HH-mm-ss");
	//return "<button row='"+options.rowId+"' note='"+input+"'class='operator-noteButton' onclick=\"noteClick($(this));\">View</button>";
}

function camelCaseToNormal(input) {
   return input
   .replace(/([A-Z])/g, ' $1')
   // uppercase the first character
   .replace(/^./, function(str){ return str.toUpperCase(); });
}
