package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.ActionEvent
import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.JoinAction
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.UUID

class Router(val name: String) {

  val servers: MutableList<RouterServer> = mutableListOf()
  val clients: MutableList<RouterPlayer> = mutableListOf()
  val playerState: MutableList<Player> = mutableListOf()

  fun start() {

  }

  fun discoverServers(servers: List<RouterServer>) {
    this.servers.clear()
    this.servers.addAll(servers)
  }

  fun discoverPlayerState(players: List<Player>) {
    this.playerState.clear()
    this.playerState.addAll(players)
  }

  fun incomming(player: RouterPlayer, event: Event, channel: EventHandler) {
//    println("$name: got event from ${player.name}: $event")
    if(event is ActionEvent && event.action is JoinAction) {
      acceptClient(player, channel)
    }

    player.server.channel.sendEvent(player.uuid, event)
  }

  fun acceptClient(player: RouterPlayer, channel: EventHandler) {
    clients.add(player)
    player.pos = loadLastPlayerPos(player) ?: throw RuntimeException("Unknown player loc")
    player.server = findServer(player.pos) ?: throw RuntimeException("No region found")
    player.channel = channel
    println("$name: connected ${player.name} to ${player.server.region.name}")
  }

  fun receiveEventFromServer(event: Event, uuid: UUID) {
    findClient(uuid)?.channel?.receiveEvent(uuid, event)
  }

  fun findClient(uuid: UUID) = clients.find { it.uuid == uuid }

  fun findServer(pos: Vec2i) = servers.find { it.region.contains(pos) }

  fun loadLastPlayerPos(player: RouterPlayer): Vec2i? = playerState.find { it.uuid == player.uuid }?.pos
}
