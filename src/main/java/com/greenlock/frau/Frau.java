package com.greenlock.frau;

import org.apache.commons.io.FileUtils;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.MessageUpdateEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by LukeSmalley on 1/19/2017.
 */
public class Frau {

    private IDiscordClient client = null;
    private Configuration configuration = null;
    private String botMention;
    private Map<String, ScriptService> services = new HashMap<>();

    private Frau(IDiscordClient client, Configuration configuration) {
        this.client = client;
        this.configuration = configuration;
    }

    public static void main(String[] args) {
        try {
            QuickLog.initialize();
        } catch (IOException ex) {
            System.out.println("(CRITICAL) Failed to initialize the QuickLog service due to IOException: " + ex.getMessage());
            return;
        }

        if (!new File("./config.json").isFile()) {
            try {
                QuickGson.serializeToPrettyJsonFile(new File("./config.json"), new Configuration());
                QuickLog.severe("Created main configuration file. Please specify the client token in this file and restart.");
            } catch (IOException ex) {
                QuickLog.failure("(CRITICAL) Frau failed to start.", "Failed to create the main configuration file.", ex);
            }
            return;
        }

        Configuration configuration = null;
        try {
            configuration = QuickGson.deserializeFromJsonFile(new File("./config.json"), Configuration.class);
        } catch (IOException ex) {
            QuickLog.failure("(CRITICAL) Frau failed to start.", "Failed to load the main configuration file.", ex);
            return;
        }

        if (configuration.clientToken == null) {
            QuickLog.severe("(CRITICAL) Client token is defined as 'null' in the config. You must specify a valid client token and restart.");
            return;
        }

        if (!new File(configuration.scriptDirectory).isDirectory()) {
            if (!new File(configuration.scriptDirectory).mkdirs()) {
                QuickLog.severe("(CRITICAL) Failed to create the script directory '" + configuration.scriptDirectory + "'.");
                return;
            }
        }

        IDiscordClient client = null;
        try {
            client = new ClientBuilder().withToken(configuration.clientToken).login();
            QuickLog.info("Discord has been started.");
        } catch (DiscordException ex) {
            QuickLog.failure("(CRITICAL) Frau failed to start.", "Failed to initialize Discord.", ex);
            return;
        }

        Frau instance = new Frau(client, configuration);
        client.getDispatcher().registerListener(instance);
        QuickLog.info("Event listeners have been registered.");

        Scanner s = new Scanner(System.in);
        while (true) {
            String input = s.nextLine();
            if (input.equalsIgnoreCase("stop")) {
                instance.onShutdown();
                System.exit(0);
            }
        }
    }

    private void onShutdown() {
        for (String key : services.keySet()) {
            try {
                services.get(key).send("#!frau-stop");
            } catch (Exception ex) {
                QuickLog.failure("Failed to correctly shut down.", "Failed to send sigterm to the service '" + key + "'.", ex);
            }
        }
    }

    public void removeService(String name) {
        if (services.containsKey(name)) {
            services.remove(name);
        }
    }


    @EventSubscriber
    public void onReady(ReadyEvent event) {
        botMention = "<@" + event.getClient().getOurUser().getID() + ">";
        QuickLog.info("Frau is awake after not sleeping.");
    }

    @EventSubscriber
    public void onMessageReceived(MessageReceivedEvent event) {
        if (onMessage(event.getMessage())) {
            String message = "#!frau-chat " + event.getMessage().getAuthor().getID() + " " + event.getMessage().getAuthor().getName() + " " +
                    (event.getMessage().getGuild() == null ? "null" : event.getMessage().getGuild().getID()) + " " +
                    event.getMessage().getContent();
            for (String key : services.keySet()) {
                if (services.get(key).isChatSubscriber()) {
                    try {
                        services.get(key).send(message);
                    } catch (IOException ex) {
                        QuickLog.failure("Failed to handle chat", "Failed to send a chat message to the service '" + key + "'.", ex);
                    }
                }
            }
        }
    }

    @EventSubscriber
    public void onMessageUpdated(MessageUpdateEvent event) {
        onMessage(event.getNewMessage());
    }

    public boolean onMessage(IMessage message) {
        if (message.getContent().startsWith(botMention)) {
            List<String> args = new CommandParser(message.getContent().substring(botMention.length())).run();
            if (args.size() > 0) {
                onCommandReceived(args, message);
            }
            return false;
        } else {
            String code = new CodeParser(message.getContent()).run();
            if (code.trim().length() > 0 &&
                    code.startsWith("#!frau\n")) {
                onScriptReceived(code, message);
                return false;
            } else {
                return true;
            }
        }
    }

    private void reply(String response, IMessage original) {
        try {
            original.getChannel().sendMessage(response);
        } catch (Exception ex) {
            try { Thread.sleep(3000); } catch (InterruptedException iex) { }
            try {
                original.getChannel().sendMessage(response);
            } catch (Exception rex) {
                QuickLog.failure("Failed to reply to a message.", "The Discord message could not be sent.", rex);
            }
        }
    }


    private void onScriptReceived(String script, IMessage message) {
        ScriptProperties properties = new ScriptProperties(script);

        if (!new File(configuration.scriptDirectory + "/" + properties.getName()).isDirectory()) {
            if (!new File(configuration.scriptDirectory + "/" + properties.getName()).mkdirs()) {
                reply("I couldn't make a working directory for your script.", message);
                return;
            }
        }

        try {
            FileUtils.writeStringToFile(new File(configuration.scriptDirectory + "/" + properties.getName() + ".py"), script, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            QuickLog.failure("Failed to run script.", "The script could not be saved.", ex);
            reply("I couldn't save your script.", message);
            return;
        }

        if (!properties.hasMetadataValue("name")) {
            reply("This script is saved as '" + properties.getName() + "'.", message);
        }

        if (services.containsKey(properties.getName())) {
            services.get(properties.getName()).halt();
        }

        try {
            services.put(properties.getName(), ScriptService.start(properties, message.getChannel(), configuration, this));
        } catch (Exception ex) {
            QuickLog.failure("Failed to run script.", "The script process could not be spawned.", ex);
            reply("I couldn't run your script.", message);
        }
    }


    private void onCommandReceived(List<String> args, IMessage message) {
        String command = args.get(0);
        List<String> subargs = args.size() > 1 ? args.subList(1, args.size()) : new ArrayList<>();

        if (command.equalsIgnoreCase("frau") || command.equalsIgnoreCase("hello")
                || command.equalsIgnoreCase("hi") || command.equalsIgnoreCase("hey")) {
            reply("I'm Kona Furugoori. I'll run any Python scripts you send me.", message);
            reply("```Use code blocks but put my name on the first line like this:\n#!frau\n\n11/10 kid your praticaly mlgpro now goodnight *goes back to her computer*```", message);
            reply("Message @ me to run commands.\n" +
                    "- start <script>\n" +
                    "- stop <script>\n" +
                    "- stopall\n" +
                    "- list\n" +
                    "- send <script> <command>", message);

        } else if (command.equalsIgnoreCase("start") || command.equalsIgnoreCase("run")
                || command.equalsIgnoreCase("exec") || command.equalsIgnoreCase("execute")) {
            if (args.size() < 2) {
                reply("Start what? Gotta give me more than that.", message);
                return;
            }
            onCommand_Start(subargs, message);
        } else if (command.equalsIgnoreCase("stop") || command.equalsIgnoreCase("kill")) {
            if (args.size() < 2) {
                reply("Stop what? Gotta give me more than that.", message);
                return;
            }
            onCommand_Stop(subargs, message);
        } else if (command.equalsIgnoreCase("stopall") || command.equalsIgnoreCase("killall")) {
            for (String key : services.keySet()) {
                try {
                    services.get(key).send("#!frau-stop");
                } catch (Exception ex) {
                    QuickLog.failure("Failed to stop all processes.", "Failed to send sigterm to the service '" + key + "'.", ex);
                }
            }
            reply("I told everything to stop.", message);
        } else if (command.equalsIgnoreCase("list") || command.equalsIgnoreCase("running")) {
            String list = "";
            for (String key : services.keySet()) {
                if (list.length() > 0) {
                    list += ", " + key;
                } else {
                    list += key;
                }
            }
            reply(list.length() > 0 ? ("What's running now: " + list) : "There's nothing running.", message);
        } else if (command.equalsIgnoreCase("send")) {
            if (args.size() < 3) {
                reply("Send to whom and wat? Gotta give me more than that.", message);
                return;
            }
            onCommand_Send(subargs, message);
        }
    }

    private void onCommand_Start(List<String> args, IMessage message) {
        if (!new File(configuration.scriptDirectory + "/" + args.get(0) + ".py").isFile()) {
            reply("'" + args.get(0) + "' does not exist.", message);
            return;
        }

        String script;
        try {
            script = FileUtils.readFileToString(new File(configuration.scriptDirectory + "/" + args.get(0) + ".py"), Charset.forName("UTF-8"));
        } catch (IOException ex) {
            QuickLog.failure("Failed to load script.", "The script could not be loaded.", ex);
            reply("I couldn't save your script.", message);
            return;
        }

        ScriptProperties properties = new ScriptProperties(script);

        if (!new File(configuration.scriptDirectory + "/" + args.get(0)).isDirectory()) {
            if (!new File(configuration.scriptDirectory + "/" + args.get(0)).mkdirs()) {
                reply("I couldn't make a working directory for your script.", message);
                return;
            }
        }

        if (services.containsKey(args.get(0))) {
            services.get(args.get(0)).halt();
        }

        try {
            services.put(args.get(0), ScriptService.start(properties, message.getChannel(), configuration, this));
        } catch (Exception ex) {
            QuickLog.failure("Failed to run script.", "The script process could not be spawned.", ex);
            reply("I couldn't run your script.", message);
            return;
        }

        reply("It's running now.", message);
    }

    private void onCommand_Stop(List<String> args, IMessage message) {
        if (services.containsKey(args.get(0))) {
            services.get(args.get(0)).halt();
        }
        reply("kk it's dead now.", message);
    }

    private void onCommand_Send(List<String> args, IMessage message) {
        if (!services.containsKey(args.get(0))) {
            reply("'" + args.get(0) + "' is not running.", message);
            return;
        }
        try {
            services.get(args.get(0)).send("#!frau-command " + args.get(1));
        } catch (IOException ex) {
            QuickLog.failure("Failed to send command to service", "Failed to send a message to the service '" + args.get(0) + "'.", ex);
            reply("Ayeee, I can't send the command! The script is not listening to me.", message);
            return;
        }
        reply("The script is informed thusly.", message);
    }
}
