package live.adamlearns.animalroyale

/**
 * This represents a particular phase of the game, e.g. waiting for players to connect.
 *
 *
 * CREATING_ARENA: no one can even join
 *
 *
 * LOBBY: people can join and run a subset of commands
 *
 *
 * PRE_GAMEPLAY: people can't do anything, that way the Arena has time to "settle". This is important since joining and
 * jumping can kill your sheep during GAMEPLAY, but not in this phase.
 *
 *
 * GAMEPLAY: people can't join, but the sheep start performing their actions
 */
enum class GamePhase {
    CREATING_ARENA, LOBBY, PRE_GAMEPLAY, GAMEPLAY
}
