'''
Created on Mar 23, 2014

@author: calarcon
'''
import os
import re
import shutil
import sys
import logging
from package.config import HorizonConfig
from package.ostools import print_combo, move_file, copy_file
import package.ostools

def replace_string_in_dir(search, replace, path):
    if os.path.isdir(path):
        for basePath, subDirs, fileList in os.walk(path):
            for fileName in fileList:
                replace_string_in_file(search, replace, os.path.join(basePath, fileName))
    else:
        print_combo("Directory specified for string replace is not valid!", logging.ERROR)

def replace_string_in_file(search, replace, path):
    # open source file
    srcFile = open(path, "rt")
    tmpLines = []
    
    try: 
        for line in srcFile:
            tmpLines.append(line.replace(search, replace))
            #tmpFile.write(line.replace(search, replace))
    except:
        print_combo("Could not write replacement file: "+path+".replaced", logging.ERROR)
    
    writeFile = open(path, "w")
    for line in tmpLines:
        writeFile.write(line)
    return

def load_templates(section, templatePath, destination):
    fileCount = 0
    try:
        for basePath, subDirs, fileList in os.walk(templatePath):
            if re.search("\.svn", basePath):
                print_combo("Skipping svn files in template path: "+basePath, logging.DEBUG)
            else:
                relativePath = ""
                matches = re.match(templatePath+"\/(.*)", basePath)
                if matches is not None:
                    relativePath = matches.group(1)
                if not os.path.isdir(os.path.join(destination, relativePath)) and destination is not None and destination != "":
                    os.mkdir(os.path.join(destination, relativePath))
                for fileName in fileList:
                    print_combo("Copying template: "+os.path.join(basePath, fileName)+" to "+ os.path.join(destination, relativePath), logging.DEBUG)
                    fileCount = fileCount + 1
                    shutil.copy2(os.path.join(basePath, fileName), os.path.join(destination, relativePath))
    except OSError:
        print_combo("No template directory found for section "+section+". Expecting to see it in "+templatePath, logging.WARN)
    except shutil.Error as e:
        print_combo("Error occurred when copying template: "+e.message, logging.ERROR)
    print_combo("Copied "+str(fileCount)+" template files to "+destination, logging.DEBUG)
    return

def process_extract(section, destination, config):
    # Orchestrate two actions
    # 1. Replace any files with the same name located in the template dir into the extracted dir
    # 2. String replace any instance of global variables HORIZON_CONFIG and HORIZON_LOGGING with values from the config
    
    templateConfigPath = os.path.join(os.path.dirname(sys.argv[0]), "templates", "config", section)
    load_templates(section, templateConfigPath, os.path.join(destination, config.get(section, HorizonConfig.CONFIG_PATH)))

    templateBinPath = os.path.join(os.path.dirname(sys.argv[0]), "templates", "bin", section)
    load_templates(section, templateBinPath, os.path.join(destination, config.get(section, HorizonConfig.BIN_PATH)))

    templateLibPath = os.path.join(os.path.dirname(sys.argv[0]), "templates", "lib", section)
    load_templates(section, templateLibPath, os.path.join(destination, config.get(section, HorizonConfig.LIB_PATH)))
    
    
    binPath = config.get(section, HorizonConfig.BIN_PATH) 
    binPath = "" if binPath is None else binPath
    configPath = config.get(section, HorizonConfig.CONFIG_PATH)
    configPath = "" if configPath is None else configPath
    
    logPath = config.get(HorizonConfig.GLOBAL, "defaultLogPath")
    if logPath is not None:
        replace_string_in_dir("$ENV{HORIZON_LOGGING}", os.path.join(logPath, section) , os.path.join(destination, binPath))
    
    configDest = config.get(section, HorizonConfig.CONFIG_DEST)
    if configDest is not None:
        #Replace string in bin
        replace_string_in_dir("=$ENV{HORIZON}", "="+configDest, os.path.join(destination, binPath))
        replace_string_in_dir("file://$ENV{HORIZON}", "file://"+configDest, os.path.join(destination, binPath))
        replace_string_in_dir("$ENV{HORIZON}", "'"+configDest+"'", os.path.join(destination, binPath))
        
        #Replace strings in config
        replace_string_in_dir("=$ENV{HORIZON}", "="+configDest, os.path.join(destination, configDest))
        replace_string_in_dir("file://$ENV{HORIZON}", "file://"+configDest, os.path.join(destination, binPath))
        replace_string_in_dir("$ENV{HORIZON}", "'"+configDest+"'", os.path.join(destination, binPath))
    
    libDest = config.get(section, HorizonConfig.LIB_DEST)
    if libDest is not None:
        replace_string_in_dir("$ENV{CLASSPATH}", "\""+libDest+"\"", os.path.join(destination, binPath))
        
def repo_config_deployment(config):
    deployFlag = None
    while deployFlag is None and not config.nonInteractive:
        rawConfirm = raw_input("\nAre you CERTAIN you want to blast all configurations from the repository (No going back)? (y or n): ")
        if rawConfirm == 'y' or rawConfirm == 'Y' or rawConfirm == "yes" or rawConfirm == "Yes":
            deployFlag = True
        elif rawConfirm == 'n' or rawConfirm == 'N' or rawConfirm == "no" or rawConfirm == "No":
            print_combo("Exiting...")
            exit(0)
        else:
            print "Please enter 'y' or 'n'"
    root_path = os.path.join(config.configRepoPath, config.env)
    if os.path.exists(root_path) and os.path.isdir(root_path):
        files_for_process = []
        for (path, dirs, files) in  os.walk(root_path):
            for current_file in files:
                files_for_process.append(os.path.join(path, current_file))
        # List of files assembled, time to redistribute
        print_combo("\nCopying files:")
        for current_file in files_for_process:
            dest_path = os.path.join("/", os.path.relpath(current_file, root_path))
            print_combo(dest_path)
            copy_file(current_file, dest_path)
    else:
        print_combo("No environment directory found at "+root_path, logging.ERROR)
        return
    
def repo_config_collection(config):
    deployFlag = None
    while deployFlag is None and not config.nonInteractive:
        rawConfirm = raw_input("\nAre you CERTAIN you want to copy all configurations from the deployment TO the repository (No going back)? (y or n): ")
        if rawConfirm == 'y' or rawConfirm == 'Y' or rawConfirm == "yes" or rawConfirm == "Yes":
            deployFlag = True
        elif rawConfirm == 'n' or rawConfirm == 'N' or rawConfirm == "no" or rawConfirm == "No":
            print_combo("Exiting...")
            exit(0)
        else:
            print "Please enter 'y' or 'n'"
    root_path = os.path.join(config.configRepoPath, config.env)
    if os.path.exists(root_path) and os.path.isdir(root_path):
        files_for_process = []
        for (path, dirs, files) in  os.walk(root_path):
            for current_file in files:
                files_for_process.append(os.path.join(path, current_file))
        # List of files assembled, time to redistribute
        print_combo("\nCopying files:")
        for current_file in files_for_process:
            dest_path = os.path.join("/", os.path.relpath(current_file, root_path))
            print_combo(current_file)
            copy_file(dest_path, current_file)
    else:
        print_combo("No environment directory found at "+root_path, logging.ERROR)
        return

if __name__ == '__main__':
    print "Not intended to be executed alone. Please run horizoninstall.py"