package dev.benndorf.regionsfordays.client

import dev.benndorf.regionsfordays.common.*
import io.rsocket.kotlin.transport.local.LocalServer
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class Client(val player: Player) {

  val eventProcessor: EventProcessor = EventProcessor()
  val players: MutableSet<Player> = mutableSetOf()
  private var actionCounter: Long = 1

  suspend fun move(vec: Vec2i) {
    eventProcessor.sendEvent(ActionEvent(MoveAction(player.pos.add(vec), actionCounter++, player), viewDistance, player.pos))
  }

  fun incomming(event: Event) {
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

  suspend fun join(server: LocalServer) {
    eventProcessor.connect(server)
    eventProcessor.sendEvent(ActionEvent(JoinAction(actionCounter++, player), viewDistance, player.pos))
    players.add(player)
  }
}
