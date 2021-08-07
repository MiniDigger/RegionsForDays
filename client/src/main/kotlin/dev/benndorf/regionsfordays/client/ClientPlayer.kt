package dev.benndorf.regionsfordays.client

import dev.benndorf.regionsfordays.common.ActionEvent
import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.JoinAction
import dev.benndorf.regionsfordays.common.MoveAction
import dev.benndorf.regionsfordays.common.ObjectInvisibleEvent
import dev.benndorf.regionsfordays.common.ObjectVisibleEvent
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.PositionEvent
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.UUID

class ClientPlayer(uuid: UUID, name: String, pos: Vec2i, areaOfInterest: Int) : Player(uuid, name, pos, areaOfInterest) {

  lateinit var channel: EventHandler
  val players: MutableSet<Player> = mutableSetOf()

  fun move(vec: Vec2i) {
    channel.sendEvent(uuid, ActionEvent(MoveAction(pos.add(vec), 1, this), areaOfInterest, pos))
  }

  fun incomming(event: Event) {
    when (event) {
      is PositionEvent -> {
        if (event.gameObject.uuid == uuid) {
          // we got our own location so we can update
          pos = event.newPos
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
    }
  }

  fun join() {
    channel.sendEvent(uuid, ActionEvent(JoinAction(1, this), areaOfInterest, pos))
    players.add(this)
  }

  override fun toString(): String {
    return "ClientPlayer(channel=$channel) ${super.toString()}"
  }
}
