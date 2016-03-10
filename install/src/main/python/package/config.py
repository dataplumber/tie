'''
Created on Mar 25, 2014

@author: calarcon
'''
import ConfigParser
import logging
import os
import sys
import re
from package.ostools import print_combo

class HorizonConfig(object):
    '''
    classdocs
    '''
    #CONSTANTS
    GLOBAL = "Global"
    DEFAULT = "Default"
    
    HAS_TOP_LEVEL = "hasTopLevel"
    MAVEN_TREE = "mavenTree"
    MAVEN_PATH = "mavenPath"
    CONFIG_PATH = "configPath"
    CONFIG_DEST = "configDest"
    BIN_PATH = "binPath"
    BIN_DEST = "binDest"
    LIB_PATH = "libPath"
    LIB_DEST = "libDest"
    FILE_PATH_PREFIX = "filePath"
    FILE_DEST_PREFIX = "fileDest"
    
    
    # ConfigParser Object
    configObj = None
    # Path for temporary tar extraction (filled in by config file)
    tmpPath = None
    # Full maven tree? if not, use manual dir
    horizonRepoPath = None
    tieRepoPath = None
    manualTarPath = None
    
    # Use default section with unknown tar type? If not, raise error when new tar type found
    defaultFlag = False
    # Overwrite existing files in destinations?
    overwriteFlag = False
    # Do a full tear down before exiting?
    fullTeardownFlag = False
    # Use templates?
    templateFlag = True

    def __init__(self, configFile):
        '''
        Constructor
        '''
        self.configObj = ConfigParser.ConfigParser()
        configFile = configFile if configFile is not None else os.path.join(os.path.dirname(sys.argv[0]), "config", "horizoninstall.conf")
        try:
            self.configObj.read(configFile)
        except ConfigParser.Error:
            raise 
        try:
            self.tmpPath = self.configObj.get(HorizonConfig.GLOBAL, "tmpPath")
            self.defaultFlag = self.configObj.get(HorizonConfig.GLOBAL, "useDefault")
            self.overwriteFlag = self.configObj.getboolean(HorizonConfig.GLOBAL, "overwriteFlag")
            self.horizonRepoPath = self.configObj.get(HorizonConfig.GLOBAL, "horizonRepoPath") if self.configObj.get("Global", "horizonRepoPath") != "" else None 
            self.tieRepoPath = self.configObj.get(HorizonConfig.GLOBAL, "tieRepoPath") if self.configObj.get("Global", "tieRepoPath") != "" else None 
            self.manualTarPath = self.configObj.get(HorizonConfig.GLOBAL, "manualTarPath") if self.configObj.get("Global", "manualTarPath") != "" else None 
            self.overwriteFlag = self.configObj.get(HorizonConfig.GLOBAL, "overwriteFlag") if self.configObj.get("Global", "overwriteFlag") != "" else None
        except ConfigParser.NoOptionError as e:
            print_combo(e.message, logging.WARN)
            raise
        
        # Run user config
        self.user_config()
        
        return
    
    def get(self, section, propertyName, exceptFlag = False):
        try:
            return self.configObj.get(section, propertyName)
        except ConfigParser.NoOptionError:
            print_combo("No property '"+propertyName+"' in section '"+section+"' found.", logging.DEBUG)
            if exceptFlag:
                raise
            else:
                return None
    def getboolean(self, section, propertyName, exceptFlag = False):
        try:
            return self.configObj.getboolean(section, propertyName)
        except ConfigParser.NoOptionError:
            print_combo("No property '"+propertyName+"' in section '"+section+"' found.", logging.DEBUG)
            if exceptFlag:
                raise
            else:
                return None
    
    def get_file_keys(self, section):
        # NOTE: config keys are forced into all lower case when reading with ConfigParser
        allKeys = self.configObj.options(section)
        fileKeys = []
        for key in allKeys:
            matchObj = re.search("filepath\[(.*)\]", key, re.IGNORECASE)
            if matchObj != None:
                fileKey = matchObj.group(1)
                # Check if all fields are there besides "filePath"
                #if "filelink["+fileKey+"]" in allKeys and "filedest["+fileKey+"]" in allKeys:
                if "filedest["+fileKey+"]" in allKeys:
                    fileKeys.append(fileKey)
                else:
                    print_combo("Could not find all required keys for file key "+fileKey+". Requires 'filePath', 'fileDest'", logging.WARN)
        return fileKeys
    
    def user_config(self):
        
        if self.manualTarPath == None and self.horizonRepoPath == None and self.tieRepoPath == None:
            parsedMethodInput = None
            print "\n\nPlease select one of the following deployment options:"
            print "[1] - Install via Maven tree structure (will specify top level paths for Horizon and TIE trees)"
            print "[2] - Install via tarballs co-located in a directory (will specify directory)"
            while parsedMethodInput == None:
                rawMethodInput =  raw_input("\nMake your selection: ")
                if rawMethodInput == "1" or rawMethodInput == "2":
                    parsedMethodInput = rawMethodInput
                else:
                    print "Please choose a valid option (1 or 2)\n"
            if parsedMethodInput == "1":
                self.horizonRepoPath = None
                while self.horizonRepoPath == None:
                    rawHorizonTree = raw_input("\nPlease specify the top level directory for the Horizon tree: ")
                    if not os.path.isdir(rawHorizonTree):
                        print "\nNot a valid directory or directory does not exist"
                    else:
                        self.horizonRepoPath = rawHorizonTree
                    
                self.tieRepoPath = None
                while self.tieRepoPath == None:
                    rawTieTree = raw_input("\nPlease specify the top level directory for the TIE tree: ")
                    if not os.path.isdir(rawTieTree):
                        print "\nNot a valid directory or directory does not exist."
                    else:
                        self.tieRepoPath = rawTieTree
            elif parsedMethodInput == "2":
                self.manualTarPath = None
                while self.manualTarPath == None:
                    rawManualTar = raw_input("\nPlease specify the directory containing all packaged TARs: ")
                    if not os.path.isdir(rawManualTar):
                        print "\nNot a valid directory or directory does not exist."
                    else:
                        self.manualTarPath = rawManualTar
        return
    
    def get_sections(self):
        filtered = []
        for section in self.configObj.sections():
            if section != "Default" and section != "Global":
                filtered.append(section)
        return filtered
    
