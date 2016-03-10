package gov.nasa.gibs.generate.api.engine;

import gov.nasa.gibs.generate.api.job.MGJobConfig;

import java.util.Date;
import java.util.List;

/**
 * @author calarcon
 * @description Class representing a generator's metadata to post to ZK. This includes
 *                a list of most recently performed jobs)
 * @date 2014-03-26
 */
public interface Engine {
   public String getName();

   public void setName(String name);
   
   public String getFederation();

   public void setFederation(String federation);

   public Date getStarted();

   public void setStarted(Date started);
   
   public String getStatus();

   public void setStatus(String status);

   public List<MGJobConfig> getCurrentJobs();
   
   public void addToCurrentJobs(MGJobConfig job);
   
   public Boolean deleteJob(Integer index);
   
   public Boolean deleteJob(MGJobConfig job);

   public void clearCurrentJobs(List<MGJobConfig> recentJobs);

   public List<MGJobConfig> getRecentJobs();
   
   public void addToRecentJobs(MGJobConfig job);

   public void clearRecentJobs(List<MGJobConfig> recentJobs);
}
