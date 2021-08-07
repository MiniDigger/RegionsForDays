package dev.benndorf.regionsfordays.common

open class Action(val id: Long, val player: Player) {
  override fun toString(): String {
    return "Action(id=$id, player=$player)"
  }
}

class MoveAction(val newPos: Vec2i, id: Long, player: Player) : Action(id, player) {
  override fun toString(): String {
    return "MoveAction(newPos=$newPos) ${super.toString()}"
  }
}

class JoinAction(id: Long, player: Player) : Action(id, player) {
  override fun toString(): String {
    return "JoinAction() ${super.toString()}"
  }
}
