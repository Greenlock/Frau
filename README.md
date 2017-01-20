# Frau -- A Python sandbox for Discord
![Kona Furugoori](http://vignette4.wikia.nocookie.net/robotics-notes/images/2/23/Fraukojiro.jpg "Kona Furugoori")
Frau is a Discord bot that allows scripts written in Python to be run in a Discord server.

### Creating Scripts
Scripts are chat messages which include code segments in markdown (\` and \`\`\`). The syntax highlighting setting of the code blocks does not matter. Scripts must also being with `#!frau` and a newline to be valid Frau scripts. Scripts are saved in a single directory (`%PYTHONPATH%`) and each script has its own working directory during execution. Code is automatically executed after being recieved by Frau, and can be started and stopped using her commands.

### Commands
Commands are invoked by Tagging @Frau in a message (`@Frau command arg1 arg2 ...`). Frau currently offers only a few basic commands to control script execution:
| Command 
| -------
| start (script-name)
