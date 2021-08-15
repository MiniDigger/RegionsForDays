package dev.benndorf.regionsfordays.common

import kotlinx.serialization.Serializable
import java.util.*

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
class ServerEvent(val event: Event, val target: @Serializable(UUIDSerializer::class) UUID, override val range: Int = event.range, override val pos: Vec2i = event.pos) : Event() {
  override fun toString(): String {
    return "ServerEvent(event=$event, target=$target, range=$range, pos=$pos) ${super.toString()}"
  }
}

@Serializable
class ServerToServerAuthEvent(val region: Region, override val range: Int = viewDistance, override val pos: Vec2i = Vec2i(0, 0)) : Event() {
  override fun toString(): String {
    return "ServerToServerAuthEvent(region='$region', range=$range, pos=$pos) ${super.toString()}"
  }
}

@Serializable
class ChunkLoadEvent(val chunk: Chunk, val chunkData: ChunkData, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : Event() {
  override fun toString(): String {
    return "ChunkLoadEvent(chunk=$chunk, chunkData=$chunkData)"
  }
}

@Serializable
class ChunkUnloadEvent(val chunk: Chunk, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : Event() {
  override fun toString(): String {
    return "ChunkUnloadEvent(chunk=$chunk)"
  }
}

@Serializable
class SubRequestEvent(val chunk: Chunk, val player: Player, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : Event() {
  override fun toString(): String {
    return "SubRequestEvent(chunk=$chunk, player=$player)"
  }
}

@Serializable
class UnsubRequestEvent(val chunk: Chunk, val player: Player, override val range: Int = viewDistance, override val pos: Vec2i = chunk.centerPos()) : Event() {
  override fun toString(): String {
    return "UnsubRequestEvent(chunk=$chunk, player=$player)"
  }
}

@Serializable
sealed class GameObjectEvent : Event() {
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
