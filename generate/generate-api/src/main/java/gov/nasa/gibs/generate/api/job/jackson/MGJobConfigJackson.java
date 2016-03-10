package gov.nasa.gibs.generate.api.job.jackson;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import gov.nasa.gibs.generate.api.job.*;

public class MGJobConfigJackson implements MGJobConfig{
   private Date dateOfData;
   private String parameter;
   private List<String> inputs;
   private String outputDir;
   private String cacheDir;
   private String workingDir;
   private String logFileDir;
   private String emptyTile;
   private Integer vrt;
   private Integer blockSize;
   private String compressionType;
   private Integer targetX;
   private String sipPath;
   private String mrfProductName;
   private Boolean daemonFlag;
   
   //The following is temporary metadata used for operator tool visibility
   private Date timePosted;
   private Date timeStarted;
   private Date timeCompleted;
   private String completedStatus;
   private String completedMessage;
   
   private Integer sourceEPSG;
   private Integer targetEPSG;
   private String mrfEmptyTileFilename;
   private String colormap;

   private Integer outputSizeX = null;
   private Integer outputSizeY = null;
   private Integer overviewScale = null;
   private Integer overviewLevels = null;
   private String overviewResample = null;
   private String resizeResample = null;
   private String reprojectionResample = null;
   private String vrtNodata = null;
   private Integer mrfBlockSize = null;
   
   private String extents;
   private String targetExtents;
   
   private Long startTime;
   private Long stopTime;
   
   public Date getDateOfData() {
      return dateOfData;
   }
   public void setDateOfData(Date dateOfData) {
      this.dateOfData = dateOfData;
   }
   public String getParameter() {
      return parameter;
   }
   public void setParameter(String parameter) {
      this.parameter = parameter;
   }
   public List<String> getInputs() {
      return inputs;
   }
   public void setInputs(List<String> inputs) {
      this.inputs = inputs;
   }
   public String getOutputDir() {
      return outputDir;
   }
   public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
   }
   public String getCacheDir() {
      return cacheDir;
   }
   public void setCacheDir(String cacheDir) {
      this.cacheDir = cacheDir;
   }
   public String getWorkingDir() {
      return workingDir;
   }
   public void setWorkingDir(String workingDir) {
      this.workingDir = workingDir;
   }
   public String getLogFileDir() {
      return logFileDir;
   }
   public void setLogFileDir(String logFileDir) {
      this.logFileDir = logFileDir;
   }
   public String getEmptyTile() {
      return emptyTile;
   }
   public void setEmptyTile(String emptyTile) {
      this.emptyTile = emptyTile;
   }
   public Integer getVrt() {
      return vrt;
   }
   public void setVrt(Integer vrt) {
      this.vrt = vrt;
   }
   public Integer getBlockSize() {
      return blockSize;
   }
   public void setBlockSize(Integer blockSize) {
      this.blockSize = blockSize;
   }
   public String getCompressionType() {
      return compressionType;
   }
   public void setCompressionType(String compressionType) {
      this.compressionType = compressionType;
   }
   public Integer getTargetX() {
      return targetX;
   }
   public void setTargetX(Integer targetX) {
      this.targetX = targetX;
   }
   
   public String getSipPath() {
      return this.sipPath;
   }
   
   public void setSipPath(String sipPath) {
      this.sipPath = sipPath;
   }

   public String getMrfProductName() {
      return mrfProductName;
   }
   public void setMrfProductName(String mrfProductName) {
      this.mrfProductName = mrfProductName;
   }
   
   public Boolean getDaemonFlag() {
      return daemonFlag;
   }
   public void setDaemonFlag(Boolean daemonFlag) {
      this.daemonFlag = daemonFlag;
   }
   
   public Date getTimePosted() {
      return timePosted;
   }
   public void setTimePosted(Date timePosted) {
      this.timePosted = timePosted;
   }
   public Date getTimeStarted() {
      return timeStarted;
   }
   public void setTimeStarted(Date timeStarted) {
      this.timeStarted = timeStarted;
   }
   public Date getTimeCompleted() {
      return timeCompleted;
   }
   public void setTimeCompleted(Date timeCompleted) {
      this.timeCompleted = timeCompleted;
   }
   public String getCompletedStatus() {
      return completedStatus;
   }
   public void setCompletedStatus(String completedStatus) {
      this.completedStatus = completedStatus;
   }
   public String getCompletedMessage() {
      return completedMessage;
   }
   public void setCompletedMessage(String completedMessage) {
      this.completedMessage = completedMessage;
   }
   
   public Integer getSourceEPSG() {
      return sourceEPSG;
   }
   public void setSourceEPSG(Integer sourceEPSG) {
      this.sourceEPSG = sourceEPSG;
   }
   
   public Integer getTargetEPSG() {
      return targetEPSG;
   }
   public void setTargetEPSG(Integer targetEPSG) {
      this.targetEPSG = targetEPSG;
   }
   
   public String getMrfEmptyTileFilename() {
      return mrfEmptyTileFilename;
   }
   public void setMrfEmptyTileFilename(String mrfEmptyTileFilename) {
      this.mrfEmptyTileFilename = mrfEmptyTileFilename;
   }
   
   public String getColormap() {
      return colormap;
   }
   public void setColormap(String colormap) {
      this.colormap = colormap;
   }
   
   public Integer getOutputSizeX() {
      return outputSizeX;
   }
   public void setOutputSizeX(Integer outputSizeX) {
      this.outputSizeX = outputSizeX;
   }
   public Integer getOutputSizeY() {
      return outputSizeY;
   }
   public void setOutputSizeY(Integer outputSizeY) {
      this.outputSizeY = outputSizeY;
   }
   public Integer getOverviewScale() {
      return overviewScale;
   }
   public void setOverviewScale(Integer overviewScale) {
      this.overviewScale = overviewScale;
   }
   public Integer getOverviewLevels() {
      return overviewLevels;
   }
   public void setOverviewLevels(Integer overviewLevels) {
      this.overviewLevels = overviewLevels;
   }
   public String getOverviewResample() {
      return overviewResample;
   }
   public void setOverviewResample(String overviewResample) {
      this.overviewResample = overviewResample;
   }
   public String getResizeResample() {
      return resizeResample;
   }
   public void setResizeResample(String resizeResample) {
      this.resizeResample = resizeResample;
   }
   public String getReprojectionResample() {
      return reprojectionResample;
   }
   public void setReprojectionResample(String reprojectionResample) {
      this.reprojectionResample = reprojectionResample;
   }
   public String getVrtNodata() {
      return vrtNodata;
   }
   public void setVrtNodata(String vrtNodata) {
      this.vrtNodata = vrtNodata;
   }
   public Integer getMrfBlockSize() {
      return mrfBlockSize;
   }
   public void setMrfBlockSize(Integer mrfBlockSize) {
      this.mrfBlockSize = mrfBlockSize;
   }
   public String getExtents() {
      return extents;
   }
   public void setExtents(String extents) {
      this.extents = extents;
   }
   public String getTargetExtents() {
      return targetExtents;
   }
   public void setTargetExtents(String targetExtents) {
        this.targetExtents = targetExtents;
   }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }
    public Long getStopTime() {
        return stopTime;
    }

    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

   
   @Override
   public String toString() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      
      String output = null;
      try {
         output = mapper.writeValueAsString(this);
      }
      catch(JsonProcessingException e) {
         //Error processing object to json
         System.out.println("Error processing object to Json");
      }
      return output;
   }
}
