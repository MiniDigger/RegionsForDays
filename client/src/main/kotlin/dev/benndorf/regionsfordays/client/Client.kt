package dev.benndorf.regionsfordays.client

import dev.benndorf.regionsfordays.common.*
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class Client(val player: Player) : NettyClient<Unit>() {

  lateinit var connection: Connection<Unit>
  val players: MutableSet<Player> = mutableSetOf()
  private var actionCounter: Long = 1

  fun move(vec: Vec2i) {
    connection.sendEvent(
      ActionEvent(
        MoveAction(player.pos.add(vec), actionCounter++, player),
        viewDistance,
        player.pos
      )
    )
  }

  fun incomming(event: Event, connection: Connection<Unit>) {
    when (event) {
      is PositionEvent -> {
        if (event.gameObject.uuid == player.uuid) {
          // we got our own location so we can update
          player.pos = event.newPos
        }
        val other = players.find { it.uuid == event.gameObject.uuid }
        if (other == null) {
          println("${player.name} got position event for unknown player ${event.gameObject.uuid}")
        } else {
          other.pos = event.newPos
        }
      }
      is ObjectVisibleEvent -> {
        when (event.gameObject) {
          is Player -> {
            if ((event.gameObject as Player).uuid != player.uuid) {
              players.add(event.gameObject as Player)
              println("${player.name}: woo, we now see ${event.gameObject}")
            }
          }
        }
      }
      is ObjectInvisibleEvent -> {
        when (event.gameObject) {
          is Player -> {
            if ((event.gameObject as Player).uuid != player.uuid) {
              players.remove(event.gameObject as Player)
              println("${player.name}: woo, we don't see ${event.gameObject} anymore")
            }
          }
        }
      }
      is ChunkLoadEvent -> {
        event.chunkData.gameObjects.filterIsInstance<Player>().forEach {
          if (it.uuid != player.uuid) {
            players.add(it)
            println("${player.name}: woo, we now see $it")
          }
        }
      }
      is ChunkUnloadEvent -> {
        players.filter { event.chunk.contains(it.pos) }.forEach { println("${player.name}: woo, we don't see $it anymore") }
        players.removeIf { event.chunk.contains(it.pos) }
      }
    }
  }

  fun start(name: String, hostname: String, port: Int) {
    start(name, hostname, port, { event, connection -> incomming(event, connection) }, { connection ->
      this.connection = connection
      connection.sendEvent(ActionEvent(JoinAction(actionCounter++, player), viewDistance, player.pos))
      players.add(player)
    })
  }
}
