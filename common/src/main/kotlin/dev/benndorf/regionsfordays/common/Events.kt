package dev.benndorf.regionsfordays.common

sealed class Event(val range: Int, val pos: Vec2i) {
  override fun toString(): String {
    return "Event(range=$range, pos=$pos)"
  }
}

class ActionEvent(val action: Action, range: Int, pos: Vec2i) : Event(range, pos) {
  override fun toString(): String {
    return "ActionEvent(action=$action) ${super.toString()}"
  }
}

open class ServerEvent(range: Int, pos: Vec2i) : Event(range, pos) {
  override fun toString(): String {
    return "ServerEvent() ${super.toString()}"
  }
}

class ChunkLoadEvent(val chunk: Chunk, val chunkData: ChunkData) : ServerEvent(viewDistance, chunk.centerPos()) {
  override fun toString(): String {
    return "ChunkLoadEvent(chunk=$chunk, chunkData=$chunkData)"
  }
}

class ChunkUnloadEvent(val chunk: Chunk) : ServerEvent(viewDistance, chunk.centerPos()) {
  override fun toString(): String {
    return "ChunkUnloadEvent(chunk=$chunk)"
  }
}

class SubRequestEvent(val chunk: Chunk, val player: Player) : ServerEvent(viewDistance, chunk.centerPos()) {
  override fun toString(): String {
    return "SubRequestEvent(chunk=$chunk, player=$player)"
  }
}

class UnsubRequestEvent(val chunk: Chunk, val player: Player) : ServerEvent(viewDistance, chunk.centerPos()) {
  override fun toString(): String {
    return "UnsubRequestEvent(chunk=$chunk, player=$player)"
  }
}

open class GameObjectEvent(val gameObject: GameObject, range: Int, pos: Vec2i) : ServerEvent(range, pos) {
  override fun toString(): String {
    return "GameObjectEvent(gameObject=$gameObject) ${super.toString()}"
  }
}

class PositionEvent(gameObject: GameObject, val newPos: Vec2i) : GameObjectEvent(gameObject, viewDistance, newPos) {
  override fun toString(): String {
    return "PositionEvent(newPos=$newPos) ${super.toString()}"
  }
}

class ObjectVisibleEvent(gameObject: GameObject, range: Int, pos: Vec2i) : GameObjectEvent(gameObject, range, pos) {
  override fun toString(): String {
    return "ObjectVisibleEvent ${super.toString()}"
  }
}

class ObjectInvisibleEvent(gameObject: GameObject, range: Int, pos: Vec2i) : GameObjectEvent(gameObject, range, pos) {
  override fun toString(): String {
    return "ObjectInvisibleEvent ${super.toString()}"
  }
}
