package com.googlecode.iptableslog;

import android.util.Log;

import java.lang.Runtime;
import java.lang.Process;
import java.io.InputStream;
import java.lang.Thread;
import java.util.Arrays;

public class ShellCommand {
  Runtime rt;
  String[] command;
  String tag = "";
  Process process;
  InputStream stdout;
  byte[] buf = new byte[8192 * 2];
  char[] charbuf = new char[8192 * 2];
  StringBuilder result = new StringBuilder(8192 * 2);

  public ShellCommand(String[] command, String tag) {
    this(command);
    this.tag = tag;
  }

  public ShellCommand(String[] command) {
    this.command = command;
    rt = Runtime.getRuntime();
  }

  public void start(boolean waitForExit) {
    MyLog.d("ShellCommand: starting [" + tag + "] " + Arrays.toString(command));
    try {
      process = new ProcessBuilder()
        .command(command)
        .redirectErrorStream(true)
        .start();

      stdout = process.getInputStream();
    } catch(Exception e) {
      Log.d("IptablesLog", "Failure starting shell command [" + tag + "]", e);
      return;
    }

    if(waitForExit) {
      waitForExit();
    }
  }

  public void waitForExit() {
    while(checkForExit() == false) {
      if(stdoutAvailable())
        readStdout();
      else 
        try { Thread.sleep(100); } catch (Exception e) { Log.d("IptablesLog", "waitForExit error", e); }
    }
  }

  public void finish() {
    MyLog.d("ShellCommand: finishing [" + tag + "] " + Arrays.toString(command));
    process.destroy();
    process = null;
  }

  public boolean checkForExit() {
    try {
      int exit = process.exitValue();
      MyLog.d("ShellCommand exited: [" + tag + "] exit " + exit);
    } catch(Exception IllegalThreadStateException) {
      return false;
    }

    finish();
    return true;
  }

  public boolean stdoutAvailable() {
    try {
      MyLog.d("stdoutAvailable [" + tag + "]: " + stdout.available());
      return stdout.available() > 0;
    } catch (java.io.IOException e) { Log.d("IptablesLog", "stdoutAvailable error", e); return false; }
  }

  public String readStdoutBlocking() {
    int read;

    MyLog.d("readStdoutBlocking [" + tag + "]");

    result.setLength(0);
    while(true) {
      try {
        read = stdout.read(buf);

        if(read < 0) {
          MyLog.d("readStdoutBlocking [" + tag + "] return null");
          return null;
        }

        MyLog.d("read returned " + read);

        for(int i = 0; i < read; i++) {
          charbuf[i] = (char) buf[i];
        }

        result.append(charbuf, 0, read);
        if(!stdoutAvailable())
          break;
      } catch (Exception e) {
        Log.d("IptablesLog", "readStdoutBlocking error", e);
        return "ERROR\n";
      }
    }

    //MyLog.d("readStdoutBlocking [" + tag + "] return [" + result + "]");
    return result.toString();
  }

  public String readStdout() {
    int read;

    MyLog.d("readStdout [" + tag + "]");

    try {
      int available;

      result.setLength(0);
      while((available = stdout.available()) > 0) {
        MyLog.d("stdout available: " + available);

        read = stdout.read(buf);

        MyLog.d("read returned " + read);

        if(read < 0) {
          MyLog.d("readStdout return null");
          return null;
        }

        for(int i = 0; i < read; i++) {
          charbuf[i] = (char) buf[i];
        }
        
        result.append(charbuf, 0, read);
      }
      //MyLog.d("readStdout return [" + result + "]");
      return result.toString();
    } catch(Exception e) {
      Log.d("IptablesLog", "readStdout error", e);
      return "ERROR\n";
    }
  }
}