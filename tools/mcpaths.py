"""Where the generators find vanilla Minecraft's own assets (block textures, models, lang) to build on.

Set the environment variable TS_MC_CLIENT_JAR to a Minecraft 1.21.1 client jar - for example
~/.minecraft/versions/1.21.1/1.21.1.jar (Windows: %APPDATA%/.minecraft/versions/1.21.1/1.21.1.jar).
"""
import os

CLIENT_JAR = os.environ.get('TS_MC_CLIENT_JAR', '/home/claude/libs/client-1.21.1-20240808.144430-extra.jar')
