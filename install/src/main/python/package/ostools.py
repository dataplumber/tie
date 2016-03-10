'''
Created on Mar 23, 2014

@author: calarcon
'''
import logging
import os
import shutil
import re

log = logging.getLogger(__name__)

def print_combo(msg, level=logging.INFO):
    if level == logging.DEBUG:
        #print msg
        log.debug(msg)
    elif level == logging.INFO:
        print msg
        log.info(msg)
    elif level == logging.WARN:
        print "WARNING - " + msg
        log.warn(msg)
    elif level == logging.ERROR:
        print "ERROR - " + msg
        log.error(msg)
    else:
        print msg
        #log.debug(msg)
    return


def clean_up_dir(path):
    abandonedList = []
    for basePath, subDirs, fileList in os.walk(path):
        for fileName in fileList:
            if re.match("\.svn", basePath):
                print_combo(".svn directory contents found, ignoring...", logging.DEBUG)
            else:
                print_combo("Adding file to abandoned list: "+basePath+" - "+fileName, logging.DEBUG)
                abandonedList.append(fileName)
    if len(abandonedList) > 0:
        print "WARNING: The following files have not been accounted for during deployment:"
        print "*"*30
        for fileName in abandonedList:
            print fileName
        deleteFlag = None
        while deleteFlag is None:
            rawConfirm = raw_input("\nContinue removing directory contents? (y or n): ")
            if rawConfirm == 'y' or rawConfirm == 'Y' or rawConfirm == "yes" or rawConfirm == "Yes":
                deleteFlag = True
            elif rawConfirm == 'n' or rawConfirm == 'N' or rawConfirm == "no" or rawConfirm == "No":
                deleteFlag = False
            else:
                print "Please enter 'y' or 'n'"
        if deleteFlag:
            print_combo("Selected to delete the temporary path despite having files in place: "+path, logging.INFO)
            shutil.rmtree(path, True)
        else:
            print_combo("Selected to leave files in "+path, logging.INFO)
        return
    else:
        try:
            shutil.rmtree(path, True)
        except shutil.Error:
            print_combo("Couldn't delete empty directory "+path, logging.ERROR)
            return
        print_combo("Auto cleanup of empty path "+path+" completed successfully", logging.INFO)


def move_dir_contents(src, dest, overwriteFlag = True):
    # Delete existing directory if exists
    if overwriteFlag == True and os.path.isdir(dest):
        try:
            print_combo("Removing existing directory path: "+dest, logging.DEBUG)
            shutil.rmtree(dest, True)
        except shutil.Error:
            print_combo("Could not remove destination directory during a replace: "+dest, logging.ERROR)
    
    # Check dest directory existance
    if not os.path.isdir(dest):
        os.makedirs(dest)
    # Move files one at a time
    for fileName in os.listdir(src):
        if re.match("^\.svn", fileName):
            log.info("SVN directory found.... ignoring: "+fileName)
        else:
            src_file = os.path.join(src, fileName)
            dst_file = os.path.join(dest, fileName)
            try:
                shutil.move(src_file, dst_file)
            except shutil.Error as e:
                print_combo(str(e), logging.ERROR)
    #os.rmdir(src)
    return

def move_file(src, dest, overwriteFlag = True):
    # Check if file exists, if so... overwrite
    if os.path.exists(dest) and overwriteFlag == True:
        print_combo("Removing existing file path: "+dest, logging.DEBUG)
        
        try:
            os.remove(dest)
        except OSError as e:
            print_combo("Could not remove destination file during a replace: "+dest, logging.ERROR)

    try:
        shutil.move(src, dest)
    except shutil.Error as e:
        
        print_combo(e.message, logging.ERROR)
    return

# def rm_dir(path):
#     problemChildren = []
#     try:
#         shutil.rmtree(path, True)
#     except:
#         pass
#     return problemChildren

def dump_object(obj):
    for attr in dir(obj):
        print "obj.%s = %s" % (attr, getattr(obj, attr))
        
if __name__ == '__main__':
    pass
