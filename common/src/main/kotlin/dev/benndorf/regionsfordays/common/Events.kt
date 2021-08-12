package dev.benndorf.regionsfordays.common

import kotlinx.serialization.Serializable

@Serializable
sealed class Event {
  abstract val range: Int
  abstract val pos: Vec2i

  override fun toString(): String {
    return "Event(range=$range, pos=$pos)"
  }
}

@Serializable
class ActionEvent(val action: Action, override val range: Int, override val pos: Vec2i) : Event() {
  override fun toString(): String {
    return "ActionEvent(action=$action) ${super.toString()}"
  }
}

@Serializable
sealed class ServerEvent() : Event() {
  override fun toString(): String {
    return "ServerEvent() ${super.toString()}"
  }
}

@Serializable
class ChunkLoadEvent(val chunk: Chunk, val chunkData: ChunkData, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : ServerEvent() {
  override fun toString(): String {
    return "ChunkLoadEvent(chunk=$chunk, chunkData=$chunkData)"
  }
}

@Serializable
class ChunkUnloadEvent(val chunk: Chunk, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : ServerEvent() {
  override fun toString(): String {
    return "ChunkUnloadEvent(chunk=$chunk)"
  }
}

@Serializable
class SubRequestEvent(val chunk: Chunk, val player: Player, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : ServerEvent() {
  override fun toString(): String {
    return "SubRequestEvent(chunk=$chunk, player=$player)"
  }
}

@Serializable
class UnsubRequestEvent(val chunk: Chunk, val player: Player, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : ServerEvent() {
  override fun toString(): String {
    return "UnsubRequestEvent(chunk=$chunk, player=$player)"
  }
}

@Serializable
sealed class GameObjectEvent : ServerEvent() {
  abstract val gameObject: GameObject

  override fun toString(): String {
    return "GameObjectEvent(gameObject=$gameObject) ${super.toString()}"
  }
}

@Serializable
class PositionEvent(override val gameObject: GameObject, val newPos: Vec2i, override val range: Int = viewDistance, override val pos: Vec2i = newPos) : GameObjectEvent() {
  override fun toString(): String {
    return "PositionEvent(newPos=$newPos) ${super.toString()}"
  }
}

@Serializable
class ObjectVisibleEvent(override val gameObject: GameObject, override val range: Int, override val pos: Vec2i) : GameObjectEvent() {
  override fun toString(): String {
    return "ObjectVisibleEvent ${super.toString()}"
  }
}

@Serializable
class ObjectInvisibleEvent(override val gameObject: GameObject, override val range: Int, override val pos: Vec2i) : GameObjectEvent() {
  override fun toString(): String {
    return "ObjectInvisibleEvent ${super.toString()}"
  }
}
