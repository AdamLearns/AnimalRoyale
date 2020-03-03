package live.adamlearns.animalroyale;

/**
 * This represents a particular phase of the game, e.g. waiting for players to connect.
 * <p>
 * CREATING_ARENA: no one can even join
 * <p>
 * LOBBY: people can join and run a subset of commands
 * <p>
 * PRE_GAMEPLAY: people can't do anything, that way the Arena has time to "settle". This is important since joining and
 * jumping can kill your sheep during GAMEPLAY, but not in this phase.
 * <p>
 * GAMEPLAY: people can't join, but the sheep start performing their actions
 */
public enum GamePhase {CREATING_ARENA, LOBBY, PRE_GAMEPLAY, GAMEPLAY, POST_GAME}