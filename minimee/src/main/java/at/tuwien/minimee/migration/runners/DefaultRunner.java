/*******************************************************************************
 * Copyright (c) 2006-2010 Vienna University of Technology, 
 * Department of Software Technology and Interactive Systems
 *
 * All rights reserved. This program and the accompanying
 * materials are made available under the terms of the
 * Apache License, Version 2.0 which accompanies
 * this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0 
 *******************************************************************************/
package at.tuwien.minimee.migration.runners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.tuwien.minimee.util.CommandExecutor;


public class DefaultRunner implements IRunner {
    private String command;
    private String workingDir = null;
    
    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir; 
    }
    
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public RunInfo run()  {
        RunInfo r = new RunInfo();
    
        log.debug("running tool: "+command);
        CommandExecutor cmdExecutor = new CommandExecutor();     
        if (workingDir != null) {
            cmdExecutor.setWorkingDirectory(workingDir);
        }
        long startTime = System.nanoTime();
        try {
            int exitStatus = cmdExecutor.runCommand(command);
            r.setSuccess(exitStatus == 0);
            r.setReport(cmdExecutor.getCommandError());
            
            if (r.isSuccess() && "".equals(r.getReport())) {
                String report = cmdExecutor.getCommandOutput();
                if ("".equals(report)) {
                    r.setReport("Successfully executed command.");
                } else {
                    r.setReport(report);
                }
            } else if (!r.isSuccess() && "".equals(r.getReport())) {
                r.setReport("Failed to execute command.");
            }
        } catch (Exception e) {
          r.setSuccess(false);
          log.error(e.getMessage(),e);
          // For security reasons(!!) we do not provide very detailed information to the client here.
          r.setReport("An error occured while running the requested command on the server.");
          return r;
        }
        long elapsedTime = System.nanoTime()-startTime;
        r.setElapsedTimeMS(elapsedTime/1000000);
        return r;
    }
}
