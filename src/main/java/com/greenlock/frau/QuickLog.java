package com.greenlock.frau;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.FileUtils;
import sx.blah.discord.handle.obj.IMessage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by LukeSmalley on 10/15/2016.
 */
public class QuickLog {

    private static File logFile = null;
    private static DateFormat timestampFormat = null;
    private static List<String> fileOutputQueue = new ArrayList<>();

    public static void initialize() throws IOException {
        timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logFile = new File("./logs/" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar.getInstance().getTime()) + ".txt");
        if (!logFile.getParentFile().isDirectory() && !logFile.getParentFile().mkdirs()) throw new IOException("Failed to create a directory for log files.");
        try {
            logToFile("[" + timestampFormat.format(Calendar.getInstance().getTime()) + "][INFO] QuickLog: Logging has begun.");
        } catch (IOException ex) {
            throw new IOException("Failed to log to file: " + ex.getMessage());
        }
    }

    public static void info(String message) {
        logSafely("INFO", message);
    }

    public static void severe(String message) {
        logSafely("SEVERE", message);
    }

    public static void failure(String fail, String cause, Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        severe(fail + " " + cause);
        severe(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        severe(sw.toString());
    }

    private static void logSafely(String mode, String message) {
        String fullMessage = "[" + timestampFormat.format(Calendar.getInstance().getTime()) + "][" + mode + "] " + message;
        try {
            logToFile(fullMessage);
        } catch (Exception ex) {
            System.out.println("[LOGFILE FAILED]" + fullMessage);
            return;
        }
        System.out.println(fullMessage);
    }

    private static void logToFile(String message) throws IOException {
        fileOutputQueue.add(message);
        while (fileOutputQueue.get(0) != message);
        FileUtils.writeStringToFile(logFile, message + "\n", Charsets.UTF_8, true);
        fileOutputQueue.remove(0);
    }
}
