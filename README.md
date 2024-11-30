# MinecraftAnimalRoyale

A plug-in for Minecraft for a Twitch-chat-based battle-royale game where sheep fire TNT at each other. 🐑🧨

Recap video talking about the project here: https://youtu.be/3q9EyGqfuyc

# Where to play

There's a single host for the game, then everybody else plays through Twitch chat, _not_ the Minecraft client. If you're hosting, then read below about how to build the game. If you'd like to play through Twitch chat, then you'll need to find a stream where someone is already hosting it. I.e. _I'm_ not hosting it, so it's not like I can just give you a link or IP address here.

# Building / running

1. Install Maven
1. Install IntelliJ IDEA Community Edition

   - From there, there's a Maven tab that will let you run the `install` command. That is what produces the `jar` file (by invoking `package` for you and then installing it into the local Maven repo in `~/.m2`) that you need, _not_ `compile`.

   - The first `install` command took about 8 minutes to download all of the metadata. Also, I actually ran it from the CLI via `mvn clean install`.

1. Install PaperMC

   - Download it from [here](https://papermc.io/downloads/paper). FYI: Spigot builds on Bukkit, and PaperMC builds on Spigot, which is why you see "bukkit" and "paper" in the source.
   - Put PaperMC's `jar` that you downloaded into its own directory since it's going to create _tons_ of files.
   - Run the [setup-script generator](https://docs.papermc.io/misc/tools/start-script-gen), replacing `server.jar` with the `jar` that you downloaded.
   - Create a file named `.env` in this folder (where you see `bukkit.yml`, `paper.yml`, etc.). It should have the contents listed below.
   - It'll mention something about the EULA. Just open `eula.txt` and accept it.
   - At this point, you can connect from the Minecraft client to `localhost`. You should type `op YourMinecraftName` to make yourself an operator (which will let you run commands in the game).
   - Run `/gamemode creative` to be able to fly

1. Run `build_and_deploy`, which will copy the resulting plugin (`./target/animalroyale-1.0-SNAPSHOT.jar`) to your server's `plugins` directory. The destination for the `copy` command should be the directory you put the PaperMC `jar` in.

```env
# This is the .env file

# Use https://twitchtokengenerator.com/ to generate the token for your bot
TWITCH_CHAT_OAUTH_TOKEN=jktehwkjtewhjkewhtjlkewhtlk

# This is the name of the channel where commands like !join are accepted
TWITCH_CHANNEL_NAME=Adam13531
```

# How to play

I wrote [a quick guide](https://docs.google.com/document/d/1moUj-t_0jbqze7Hj56434eO6iMPjn2eO_aeHCuQcwMw/edit). The only undocumented commands are exclusively for the streamer:

- `!lava` - spawn a single block of lava somewhere in the arena (this is what Sudden Death does repeatedly at the end of the match)
- `!newarena` - start a new match
- `!startrounds` - force a match to start before the lobby time is up

# Known bugs

This was all made in 5 days on Adam Learns for the purpose of learning, so I wasn't planning on supporting it long-term. Here are some known bugs:

- Some names don't get crossed-out on the scoreboard even though the sheep is dead.
- When an arena is generated, the world sometimes doesn't render without jittering the mouse or keyboard first.
- Names are all made lowercase from Twitch rather than being their display names.
- The lobby time should probably be 90 seconds based on how long people typically spend setting up `!identify` and `!tnt`.
- Some things are hard-coded (e.g. "adm\*" emotes and AdamLearnsBot as the name to whisper).
- This isn't a bug, but I just wanted to point out that `doc/TODO_ARCHIVE` has lots of ideas that I couldn't get to, e.g. bouncing TNT, a rail gun, a lava gun, and the concept of a shop. Search for "Extra ideas".

# Launching from the command line

- Download [Prism Launcher](https://prismlauncher.org/)
- Add a client instance
- Edit the instance:
  - Settings → Miscellaneous → check "Set a target to join on launch" and fill out "localhost" for the server
  - Settings → Game windows → ✅ Game window → set the width and height (you can't start maximized on macOS as of Wed 11/27/2024 for some reason; it just doesn't launch in full-screen).
- Launch `/Applications/Prism\ Launcher.app/Contents/MacOS/prismlauncher --launch 1.21.3` (note: the final bit there, `1.21.3`, is the Instance ID. See [here](https://prismlauncher.org/wiki/getting-started/command-line-interface/) for how to get that ID (it's just the folder name of the instance)).
