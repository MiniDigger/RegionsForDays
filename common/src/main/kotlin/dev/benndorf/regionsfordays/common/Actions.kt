package dev.benndorf.regionsfordays.common

import kotlinx.serialization.Serializable

@Serializable
sealed class Action {
  abstract val id: Long
  abstract val player: Player

  override fun toString(): String {
    return "Action(id=$id, player=$player)"
  }
}

@Serializable
class MoveAction(val newPos: Vec2i, override val id: Long, override val player: Player) : Action() {
  override fun toString(): String {
    return "MoveAction(newPos=$newPos) ${super.toString()}"
  }
}

@Serializable
class JoinAction(override val id: Long, override val player: Player) : Action() {
  override fun toString(): String {
    return "JoinAction() ${super.toString()}"
  }
}
