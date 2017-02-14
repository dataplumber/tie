'''
Created on Mar 24, 2014

@author: calarcon
'''
# FOR EACH TARBALL IN A DIRECTORY
# - Identify tar file
# - Unpack tar file to temp space
# - Verify tar ball
# - Look up config to get paths
# - Copy files to final location
# - Clean up temp space

#Built-ins
import ConfigParser
import tarfile
import argparse
#Custom
from package.postprocess import *
from package.ostools import *
from package.config import HorizonConfig

## GLOBALS(most to be replaced by config)
##
COPYRIGHT = "Copyright 2014, Jet Propulsion Laboratory, Caltech, NASA"
VERSION = "0.5.0"
VERSION_DATE = "March 2014"
VERSION_SUMMARY = "GIBS Installation Package "+VERSION+", "+VERSION_DATE 

config = HorizonConfig("config/horizoninstall.conf")
args = []
#
# METHODS
#
def setup():
    try:
        print_combo("Creating temporary workspace at "+config.tmpPath, logging.DEBUG)
        os.makedirs(config.tmpPath)
    except OSError:
        print_combo(config.tmpPath + " already exists!", logging.DEBUG)
        pass
    return

def get_tar_files(sectionList):
    global horizonRepoPath, tieRepoPath, manualTarPath
    fileList = []
    if config.manualTarPath != None and os.path.isdir(config.manualTarPath):
        # manual path selected, get dir contents and filter to only tarballs
        filePaths = os.listdir(config.manualTarPath)
        for filePath in filePaths:
            if re.match(".*\.tar\.gz", filePath):
                fullPath = os.path.join(config.manualTarPath, filePath)
                #print_combo("Full path is: "+fullPath)
                fileList.append(fullPath)
    elif config.horizonRepoPath != None and os.path.isdir(config.horizonRepoPath) and config.tieRepoPath != None and os.path.isdir(config.tieRepoPath):
        # iterate across all  modules and look for tar.gz files in the specified dirs
        for section in sectionList:
            basePath = ""
            if section != HorizonConfig.GLOBAL and section != HorizonConfig.DEFAULT:
                try:
                    mavenTree = config.get(section, HorizonConfig.MAVEN_TREE)
                    mavenPath = config.get(section, HorizonConfig.MAVEN_PATH)
                    if mavenTree == "tie":
                        basePath = os.path.join(config.tieRepoPath, mavenPath)
                    elif mavenTree == "horizon":
                        basePath = os.path.join(config.horizonRepoPath, mavenPath)
                    fileNames = os.listdir(basePath)
                    for fileName in fileNames:
                        if re.match(".*\.tar\.gz", fileName):
                            fullPath = os.path.join(basePath, fileName)
                            fileList.append(fullPath)
                except ConfigParser.NoOptionError:
                    print_combo("No maven tree/path specified in configuration for section "+section+"... skipping", logging.WARN)
                except OSError:
                    print_combo("Section's target directory not found or was not specified for section: "+section, logging.WARN)
    else:
        print_combo("Not enough configuration was provided to find tar files! Please specify a manual tar ball path, or top level directories for the repository trees (horizon and tie).", logging.ERROR)
    return fileList

def process_tar(tarPath):
    tarName = os.path.basename(tarPath)
    basePath = os.path.dirname(tarPath)
    currentTar = tarfile.open(tarPath)
    tarFiles = currentTar.getmembers()
    section = identify(tarFiles)
    
    if section == None:
        section = "Default"
        
    if section not in config.filteredSections:
        return
        
    workingDir = os.path.join(config.tmpPath, section)
    #Boolean values can return true, false, or None so BE EXPLICIT!
    hasTopLevel = True#config.getboolean(section, HorizonConfig.HAS_TOP_LEVEL) if config.getboolean(section, HorizonConfig.HAS_TOP_LEVEL) is not None else True 
    configDest = config.get(section, HorizonConfig.CONFIG_DEST)
    binDest = config.get(section, HorizonConfig.BIN_DEST)
    libDest = config.get(section, HorizonConfig.LIB_DEST)
    
    
    print_combo("Processing "+tarName+" identified as "+section, logging.DEBUG)
    
    print "\n"
    print "*"*30
    print "PROCESSING PACKAGE "+tarName+"\n"
    if section == "Default":
        print "Subsystem:     Default (WARNING: No subsystem identified)"
    else:
        print "Subsystem:     "+section
    print "Base Path:     "+basePath
    print "Temporary Extraction To: "+workingDir
    if not config.noConfig:
        print "config Deployment: "+configDest
    print "bin Deployment: "+binDest
    print "lib Deployment: "+libDest
    print ""
    #print "*"*30
    
    if not config.nonInteractive:
        tarFlag = None
        while tarFlag is None:
            rawConfirm = raw_input("Continue processing this package?(y or n): ")
            if rawConfirm == 'y' or rawConfirm == 'Y' or rawConfirm == "yes" or rawConfirm == "Yes":
                tarFlag = True
            elif rawConfirm == 'n' or rawConfirm == 'N' or rawConfirm == "no" or rawConfirm == "No":
                tarFlag = False
            else:
                print "Please enter 'y' or 'n'"
        
        # User selected no, skip this file
        if not tarFlag:
            print "... skipping"
            print_combo("User has selected to SKIP package "+tarName, logging.DEBUG)
            return
        
        print_combo("User has selected to PROCESS package "+tarName, logging.DEBUG)

    #Untar to tmp directory
    currentTar.extractall(workingDir)
    if hasTopLevel:
        try:
            workingDir = os.path.join(workingDir, os.listdir(workingDir)[0])
        except OSError as e:
            print_combo(e.message, logging.WARN)
    if config.templateFlag:
        #Run post process on section
        process_extract(section, workingDir, config)
    try:
        redistribute(workingDir, section)
        clean_up_dir(workingDir, config.nonInteractive)
    except OSError as e:
        print_combo("Could not redistribute package "+section+" because of misconfigured file paths: "+str(e), logging.ERROR)
        shutil.rmtree(workingDir, True)
    
    return

def teardown():
    if config.fullTeardownFlag == True:
        shutil.rmtree(config.tmpPath, True)
    return

def redistribute(path, section):
    ###
    ### DONT FORGET TO ADD LINKS AS WELL (check included link flag in config)
    ###
    srcDestList = []
    
    # handle config
    if not config.noConfig:
        try:
            srcPath = os.path.join(path, config.get(section, HorizonConfig.CONFIG_PATH))
            if srcPath != None and srcPath != "" and os.path.isdir(srcPath):
                destPath = config.get(section, HorizonConfig.CONFIG_DEST)
                srcDestList.append({"src": srcPath, "dest": destPath})
                print_combo("Moving contents from "+srcPath+" to "+destPath, logging.DEBUG)
                move_dir_contents(srcPath, destPath, config.overwriteFlag)
            else:
                print_combo("No config directory specified or found... skipping", logging.INFO)
        except ConfigParser.NoOptionError:
            print_combo("configPath not found for section "+section+"... skipping")
    else:
        print_combo("Skipping configuration", logging.DEBUG)
    # handle bin
    try:
        srcPath = os.path.join(path, config.get(section, HorizonConfig.BIN_PATH))
        if srcPath != None and srcPath != "" and os.path.isdir(srcPath):
            destPath = config.get(section, HorizonConfig.BIN_DEST)
            srcDestList.append({"src": srcPath, "dest": destPath})
            print_combo("Moving contents from "+srcPath+" to "+destPath, logging.DEBUG)
            move_dir_contents(srcPath, destPath, config.overwriteFlag)
        else:
            print_combo("No bin directory specified or found... skipping", logging.INFO)
    except ConfigParser.NoOptionError:
        print_combo("binPath not found for section "+section+"... skipping")
    
    # handle lib
    try:
        srcPath = os.path.join(path, config.get(section, HorizonConfig.LIB_PATH))
        if srcPath != None and srcPath != "" and os.path.isdir(srcPath):
            destPath = config.get(section, HorizonConfig.LIB_DEST)
            srcDestList.append({"src": srcPath, "dest": destPath})
            print_combo("Moving contents from "+srcPath+" to "+destPath, logging.DEBUG)
            move_dir_contents(srcPath, destPath, config.overwriteFlag)
        else:
            print_combo("No lib directory specified or found... skipping", logging.INFO)
    except ConfigParser.NoOptionError:
        print_combo("libPath not found for section "+section+"... skipping")
    
    # handle everything else
    fileKeys = config.get_file_keys(section)
    fileKeys.sort()
    for fileKey in fileKeys:
        try:
            srcPath = os.path.join(path, config.get(section, HorizonConfig.FILE_PATH_PREFIX+"["+fileKey+"]"))
            if srcPath != None and srcPath != "" and os.path.isfile(srcPath):
                destPath = config.get(section,HorizonConfig.FILE_DEST_PREFIX+"["+fileKey+"]")
                srcDestList.append({"src": srcPath, "dest": destPath})
                print_combo("Moving individual file from "+srcPath+" to "+destPath, logging.DEBUG)
                move_file(srcPath, destPath, config.overwriteFlag)
            else:
                print_combo("No custom file "+config.get(section, "filePath["+fileKey+"]")+" directory specified or found... skipping", logging.INFO)
        except ConfigParser.NoOptionError:
            print_combo("filePath["+fileKey+"] not found for section "+section+"... skipping")
    print_combo(str(len(srcDestList))+" directories in "+path+" distributed successfully to the following locations:", logging.INFO)
    for srcDest in srcDestList:
        print_combo( " ---- "+srcDest['dest'], logging.INFO)
    print ""
    return

def identify(members):
    tarType = None
#     if re.match("generate-mrf",members[0].name):
#         tarType = "generate-mrf"
#     elif re.match("subscriber",members[0].name):
#         tarType = "subscriber"
#     elif re.match("ingest-server",members[0].name):
#         tarType = "ingest-server"
#     else:
    sections = config.get_sections()
    for section in sections:
        if re.match(section, members[0].name):
            tarType = section;
    return tarType

###
### MAIN SCRIPT
###

def main(argv=None):
    print COPYRIGHT
    print VERSION_SUMMARY
    
    # Configure logging
    formatter = '%(asctime)s - %(levelname)s - %(message)s'
    logging.basicConfig(filename="horizoninstall.log", level=logging.DEBUG, format=formatter)
    
    #Argparse section
    parser = argparse.ArgumentParser(description="Horizon/TIE Install Script Usage")
    parser.add_argument("-e", dest='env', required=False, action="store", help="Specify which environment to use in the configuration")
    parser.add_argument("-n", dest='noconfig', required=False, action="store_true", help="Flag to NOT deploy configuration during install (bin and lib only)")
    parser.add_argument("-i", dest='noninteractive', required=False, action="store_true", help="Flag to skip all prompts")
    parser.add_argument("-d", dest='deploy', required=False, action="store_true", help="Enables repo configuration deployment mode (copy to final location)")
    parser.add_argument("-c", dest='collect', required=False, action="store_true", help="Enables repo configuration collection mode (copy back to repo)")
    
    args = parser.parse_args()
    
    if args.env:
        config.env = args.env
    config.update_filtered()
    print_combo( "\n ---- INSTALLING WITH ENVIRONMENT: "+config.env)
    if args.noninteractive:
        print_combo( "\n ---- ASSUMING ALL AFFIRMATIVE ANSWERS")
        config.nonInteractive = True
    if args.deploy:
        print_combo( "\n ---- ENABLING REPO CONFIG DEPLOYMENT")
        repo_config_deployment(config)
        return
    if args.collect:
        print_combo( "\n ---- ENABLING REPO CONFIG COLLECTION")
        repo_config_collection(config)
        return
    if args.noconfig:
        print_combo( "\n ---- NO CONFIGURATION WILL BE INSTALLED")
        config.noConfig = True

    
    print_combo("Starting tool configuration!", logging.DEBUG)
    # Set up config values and temp directory
    setup()
    print_combo("Configuration complete, proceeding to tar processing.", logging.DEBUG)
    sectionList = config.configObj.sections()
    tarFilePaths = get_tar_files(sectionList)
    
    
    for tarFilePath in tarFilePaths:
        process_tar(tarFilePath)

if __name__ == '__main__':
    pass