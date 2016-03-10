package gov.nasa.gibs.generate.api.job;

import java.util.List;

public interface MGJob {

   public List<MGSource> getSources();
   public void setSources(List<MGSource> sources);
   public MGSource createSource();
   public void addToSources(MGSource source);

   public MGJobConfig getJobConfig();
   public void setJobConfig(MGJobConfig config);
   public MGJobConfig createJobConfig();
   
   
}
