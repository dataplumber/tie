package gov.nasa.gibs.generate.api.job;

import java.util.*;

public interface MGJobConfig {
   
   public Date getDateOfData();
   public void setDateOfData(Date dateOfData);
   
   public String getParameter();
   public void setParameter(String parameter);
   
   public List<String> getInputs();
   public void setInputs(List<String> inputs);
   
   public String getOutputDir();
   public void setOutputDir(String outputDir);
   
   public String getCacheDir();
   public void setCacheDir(String cacheDir);
   
   public String getWorkingDir();
   public void setWorkingDir(String workingDir);
      
   public String getLogFileDir();
   public void setLogFileDir(String logFileDir);
   
   public String getEmptyTile();
   public void setEmptyTile(String emptyTile);
   
   public Integer getVrt();
   public void setVrt(Integer vrt);
   
   public Integer getBlockSize();
   public void setBlockSize(Integer blockSize);
   
   public String getCompressionType();
   public void setCompressionType(String compressionType);
   
   public Integer getTargetX();
   public void setTargetX(Integer targetX);
   
   public String getSipPath();
   public void setSipPath(String sipPath);
   
   public String getMrfProductName();
   public void setMrfProductName(String mrfProductName);
   
   public Boolean getDaemonFlag();
   public void setDaemonFlag(Boolean daemonFlag);
   
   public Date getTimePosted();
   public void setTimePosted(Date timePosted);
   
   public Date getTimeStarted();
   public void setTimeStarted(Date timeStarted);

   public Date getTimeCompleted();
   public void setTimeCompleted(Date timeCompleted);
   
   public String getCompletedStatus();
   public void setCompletedStatus(String completedStatus);
   
   public String getCompletedMessage();
   public void setCompletedMessage(String completedMessage);
   
   public Integer getSourceEPSG();
   public void setSourceEPSG(Integer sourceEPSG);
   
   public Integer getTargetEPSG();
   public void setTargetEPSG(Integer targetEPSG);
   
   public String getMrfEmptyTileFilename();
   public void setMrfEmptyTileFilename(String mrfEmptyTileFilename);
   
   public String getColormap();
   public void setColormap(String colormap);
   
   public Integer getOutputSizeX();
   public void setOutputSizeX(Integer outputSizeX);
   
   public Integer getOutputSizeY();
   public void setOutputSizeY(Integer outputSizeY);
   
   public Integer getOverviewScale();
   public void setOverviewScale(Integer overviewScale);
   
   public Integer getOverviewLevels();
   public void setOverviewLevels(Integer overviewLevels);
   
   public String getOverviewResample();
   public void setOverviewResample(String overviewResample);
   
   public String getResizeResample();
   public void setResizeResample(String resizeResample);
   
   public String getReprojectionResample();
   public void setReprojectionResample(String reprojectionResample);
   
   public String getVrtNodata();
   public void setVrtNodata(String vrtNodata);
   
   public Integer getMrfBlockSize();
   public void setMrfBlockSize(Integer mrfBlockSize);
   
   public String getExtents();
   public void setExtents(String extents);
   
   public String getTargetExtents();
   public void setTargetExtents(String targetExtents);
   
   public Long getStartTime();
   public void setStartTime(Long startTime);
   
   public Long getStopTime();
   public void setStopTime(Long stopTime);
   
}
