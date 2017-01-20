# Frau -- A Python sandbox for Discord
Frau is a Discord bot that allows scripts written in Python to be run in a Discord server.

![Kona Furugoori](http://vignette4.wikia.nocookie.net/robotics-notes/images/2/23/Fraukojiro.jpg "Kona Furugoori")

======

### Creating Scripts
Scripts are chat messages which include code segments in markdown (\` and \`\`\`). The syntax highlighting setting of the code blocks does not matter. Scripts must also begin with `#!frau` and a newline to be valid Frau scripts. Scripts are saved in a single directory (`%PYTHONPATH%`) and each script has its own working directory during execution. Code is automatically executed after being recieved by Frau, and can be started and stopped using her commands.

### Commands
Commands are invoked by tagging the bot in a message (`@Frau command arg1 arg2 ...`). Frau currently offers only a few basic commands to control script execution:

| Command                 | Description                         |
|:----------------------- |:----------------------------------- |
| start (script-name)     | Starts a previously saved script    |
| stop (script-name)      | Stops a currently running script    |
| stopall                 | Stops all currently running scripts |
| list                    | Lists all currently running scripts |
| send (script) (command) | Sends a message to a running script |

### Services
Services are scripts that run continuously. This is theoretically useful for creating scripts to handle general chat messages. Technically all scripts are run as services, but only scripts with the `#$service=true` option set will have chat redirected to their stdin. A service should loop infinitely and read stdin to handle Frau command packet things as they are sent. There are three service commands at this time:

`#!frau-stop` - Sent before the bot shuts down.

`#!frau-command (command...)` - Sent by the bot's 'send' command.

`#!frau-chat (author) (author-name) (guild) (message...)` - Sent to communicate a chat message.

### Script Metadata
Script metadata consists of the comment tags at the beginning of each file (`#$var=value`). These tags are processed before script execution and facilitate functionality including the naming of scripts and defining if scripts are services.

| Option  | Value Type | Description                                         |
|:------- | ---------- | --------------------------------------------------- |
| name    | Text       | Defines the name a script will be saved with        |
| service | true/false | Defines whether or not to redirect chat to a script |

### Development and Futurology
Frau is very much a work-in-progress since ~~her~~ its model for running scripts is highly insecure, as it has no means for sandboxing. There is pretty much room for improvement everywhere.

Feel free to fork and make improvements as you like. It needs them.
