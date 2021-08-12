package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.*
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketServer
import io.rsocket.kotlin.transport.local.LocalServer
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.*

@ExperimentalSerializationApi
class Router(val name: String) {

  val eventProcessor: EventProcessor = EventProcessor()

  val servers: MutableList<RouterServer> = mutableListOf()
  val clients: MutableList<RouterPlayer> = mutableListOf()
  val playerState: MutableList<Player> = mutableListOf()

  val server = LocalServer()

  fun start() {
    RSocketServer().bind(this.server) {
      RSocketRequestHandler {
        fireAndForget {
          val event = eventProcessor.decodeEvent(it)
          println("$name got $event")
//          incomming(event)
        }
      }
    }
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
    if (event is ActionEvent && event.action is JoinAction) {
      acceptClient(player, channel)
    }

    player.server.channel.sendEvent(player.player.uuid, event)
  }

  fun acceptClient(player: RouterPlayer, channel: EventHandler) {
    clients.add(player)
    player.player.pos = loadLastPlayerPos(player) ?: throw RuntimeException("Unknown player loc")
    player.server = findServer(player.player.pos) ?: throw RuntimeException("No region found")
    player.channel = channel
    println("$name: connected ${player.player.name} to ${player.server.region.name}")
  }

  fun receiveEventFromServer(event: Event, uuid: UUID) {
    findClient(uuid)?.channel?.receiveEvent(uuid, event)
  }

  fun findClient(uuid: UUID) = clients.find { it.player.uuid == uuid }

  fun findServer(pos: Vec2i) = servers.find { it.region.contains(pos) }

  fun loadLastPlayerPos(player: RouterPlayer): Vec2i? = playerState.find { it.uuid == player.player.uuid }?.pos
}
