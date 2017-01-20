package com.greenlock.frau;

import sx.blah.discord.handle.obj.IChannel;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LukeSmalley on 1/19/2017.
 */
public class ScriptService {

    private ScriptProperties properties;
    private Process process;
    private IChannel channel;

    private ScriptService(ScriptProperties properties, Process process, IChannel channel, Frau bot) {
        this.properties = properties;
        this.process = process;
        this.channel = channel;

        Thread stdoutListener = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while (process.isAlive()) {
                        while ((line = stdout.readLine()) != null) {
                            reply(line);
                        }
                    }
                } catch (Exception ex) {
                    QuickLog.failure("Failed to stay attached to stdout for script service '" + properties.getName() + "'.", "Cause is unknown.", ex);
                }
            }
        };
        stdoutListener.start();

        Thread stderrListener = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String line;
                    String error = "";
                    while (process.isAlive()) {
                        while ((line = stdout.readLine()) != null) {
                            error += line + "\n";
                        }
                    }
                    if (error.length() > 0) {
                        reply("'" + properties.getName() + "' had a problem and died:\n```\n" + error + "\n```");
                    }
                    bot.removeService(properties.getName());
                } catch (Exception ex) {
                    QuickLog.failure("Failed to stay attached to stderr for script service '" + properties.getName() + "'.", "Cause is unknown.", ex);
                }
            }
        };
        stderrListener.start();
    }

    public static ScriptService start(ScriptProperties properties, IChannel channel, Configuration configuration, Frau bot) throws Exception {
        List<String> pythonArgs = new ArrayList<>();
        pythonArgs.addAll(configuration.python);
        pythonArgs.add(new File(configuration.scriptDirectory + "/" + properties.getName() + ".py").getAbsolutePath());
        ProcessBuilder python = new ProcessBuilder()
                .command(pythonArgs)
                .directory(new File(configuration.scriptDirectory + "/" + properties.getName()));
        python.environment().put("PYTHONPATH", new File(configuration.scriptDirectory).getAbsolutePath());
        Process pythonProcess = python.start();
        return new ScriptService(properties, pythonProcess, channel, bot);
    }


    public void halt() {
        process.destroy();
        while (process.isAlive()) { }
    }

    public void send(String message) throws IOException {
        BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        stdin.write(message + "\n");
        stdin.flush();
    }

    private void reply(String response) {
        try {
            channel.sendMessage(response);
        } catch (Exception ex) {
            try { Thread.sleep(3000); } catch (InterruptedException iex) { }
            try {
                channel.sendMessage(response);
            } catch (Exception rex) {
                QuickLog.failure("Failed to reply to a message.", "The Discord message could not be sent.", rex);
            }
        }
    }

    public boolean isChatSubscriber() {
        return properties.isService();
    }
}
