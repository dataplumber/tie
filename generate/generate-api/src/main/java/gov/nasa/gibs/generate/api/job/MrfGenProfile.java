package gov.nasa.gibs.generate.api.job;

import java.util.List;

public interface  MrfGenProfile {

   public String getOperation();
   public void setOperation(String operation);
   
   public List<MGJob> getJobs();
   public void setJobs(List<MGJob> jobs);
   
   public MGJob createJob();
   public void addToJobs(MGJob job);
   
}
