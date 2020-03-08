# MinecraftAnimalRoyale

A plug-in for Minecraft for a Twitch-chat-based battle-royale game where sheep fire TNT at each other. 🐑🧨

Recap video talking about the project here: https://youtu.be/3q9EyGqfuyc

# Where to play

There's a single host for the game, then everybody else plays through Twitch chat, _not_ the Minecraft client. If you're hosting, then read below about how to build the game. If you'd like to play through Twitch chat, then you'll need to find a stream where someone is already hosting it. I.e. _I'm_ not hosting it, so it's not like I can just give you a link or IP address here.

# Building / running

1. Install Maven
1. From the root of the repository, run `mvn clean install`
1. Copy the resulting plugin (`./target/animalroyale-1.0-SNAPSHOT.jar` to your server's `plugins` directory
1. Create a file named `.env` your server's root folder (where you see `bukkit.yml`, `paper.yml`, etc.). It should have the contents listed below:

```env
# Use https://twitchapps.com/tmi/ to generate the token
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
