package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.*
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.*
import kotlin.concurrent.thread

@ExperimentalSerializationApi
class Router(val name: String) : NettyServer<RouterPlayer>() {

  val servers: MutableList<RouterServer> = mutableListOf()
  val clients: MutableList<Connection<RouterPlayer>> = mutableListOf()
  val playerState: MutableList<Player> = mutableListOf()

  fun start(name: String, port: Int) {
    start(name, port, { event, connection -> incoming(event, connection) })
  }

  fun discoverServers(servers: List<RouterServer>) {
    this.servers.clear()
    this.servers.addAll(servers)
  }

  fun discoverPlayerState(players: List<Player>) {
    this.playerState.clear()
    this.playerState.addAll(players)
  }

  fun incoming(event: Event, connection: Connection<RouterPlayer>) {
    if (event is ActionEvent && event.action is JoinAction) {
      connection.player = event.action.player
      connection.context = RouterPlayer(connection.player)
      acceptClient(connection) {
        connection.context.server.connection?.sendEvent(event)
      }
    } else {
      connection.context.server.connection?.sendEvent(event)
    }
  }

  fun acceptClient(connection: Connection<RouterPlayer>, callback: () -> Unit) {
    clients.add(connection)
    connection.player.pos = loadLastPlayerPos(connection.context) ?: throw RuntimeException("Unknown player loc")
    connection.context.server = findServer(connection.player.pos) ?: throw RuntimeException("No region found")
    if (connection.context.server.connection == null) {
      connect(connection.context.server, callback)
    } else {
      callback()
    }
    println("$name: connected ${connection.player.name} to ${connection.context.server.region.name}")
  }

  fun connect(routerServer: RouterServer, callback: () -> Unit) {
    thread(name = "$name -> ${routerServer.region.name}") {
      NettyClient<RouterServer>().start("$name -> ${routerServer.region.name}", routerServer.address.first, routerServer.address.second, { event, connection ->
        println("$name -> ${routerServer.region.name}: event from server $event")
        if (event is ServerEvent) {
          receiveEventFromServer(event)
        } else {
          println("not a server event?!")
        }
      }, {
        routerServer.connection = it
        callback()
      })
    }
  }

  fun receiveEventFromServer(event: ServerEvent) {
    findClient(event.target)?.sendEvent(event.event)
  }

  fun findClient(uuid: UUID) = clients.find { it.player.uuid == uuid }

  fun findServer(pos: Vec2i) = servers.find { it.region.contains(pos) }

  fun loadLastPlayerPos(player: RouterPlayer): Vec2i? = playerState.find { it.uuid == player.player.uuid }?.pos
}
