package dev.benndorf.regionsfordays.client

import dev.benndorf.regionsfordays.common.*
import java.util.*

class ClientPlayer(uuid: UUID, name: String, pos: Vec2i) : Player(uuid, name, pos) {

  lateinit var channel: EventHandler
  val players: MutableSet<Player> = mutableSetOf()
  private var actionCounter: Long = 1

  fun move(vec: Vec2i) {
    channel.sendEvent(uuid, ActionEvent(MoveAction(pos.add(vec), actionCounter++, this), viewDistance, pos))
  }

  fun incomming(event: Event) {
    when (event) {
      is PositionEvent -> {
        if (event.gameObject.uuid == uuid) {
          // we got our own location so we can update
          pos = event.newPos
        }
        val player = players.find { it.uuid == event.gameObject.uuid }
        if (player == null) {
          println("$name got position event for unknown player ${event.gameObject.uuid}")
        } else {
          player.pos = event.newPos
        }
      }
      is ObjectVisibleEvent -> {
        when (event.gameObject) {
          is Player -> {
            if ((event.gameObject as Player).uuid != uuid) {
              players.add(event.gameObject as Player)
              println("$name: woo, we now see ${event.gameObject}")
            }
          }
        }
      }
      is ObjectInvisibleEvent -> {
        when (event.gameObject) {
          is Player -> {
            if ((event.gameObject as Player).uuid != uuid) {
              players.remove(event.gameObject as Player)
              println("$name: woo, we don't see ${event.gameObject} anymore")
            }
          }
        }
      }
      is ChunkLoadEvent -> {
        event.chunkData.gameObjects.filterIsInstance<Player>().forEach {
          if (it.uuid != uuid) {
            players.add(it)
            println("$name: woo, we now see $it")
          }
        }
      }
      is ChunkUnloadEvent -> {
        players.filter { event.chunk.contains(it.pos) }.forEach { println("$name: woo, we don't see $it anymore") }
        players.removeIf { event.chunk.contains(it.pos) }
      }
    }
  }

  fun join() {
    channel.sendEvent(uuid, ActionEvent(JoinAction(actionCounter++, this), viewDistance, pos))
    players.add(this)
  }

  override fun toString(): String {
    return "ClientPlayer(channel=$channel) ${super.toString()}"
  }
}
