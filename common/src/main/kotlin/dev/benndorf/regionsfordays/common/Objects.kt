package dev.benndorf.regionsfordays.common

import java.util.*
import kotlin.math.abs

data class MutableTriple<A, B, C>(var first: A, var second: B, var third: C)

data class Vec2i(val x: Int, val y: Int) {
  fun add(vec: Vec2i) = Vec2i(x + vec.x, y + vec.y)
  fun distanceSquared(pos: Vec2i): Int {
    return abs((x - pos.x) * (x - pos.x) - (y - pos.y) * (y - pos.y))
  }
}

abstract class GameObject(var pos: Vec2i, val uuid: UUID) {

  override fun toString(): String {
    return "GameObject(pos=$pos, uuid=$uuid)"
  }
}

open class Player(uuid: UUID, val name: String, pos: Vec2i) : GameObject(pos, uuid), Observer {
  override fun observe(event: Event) {
    TODO("Not yet implemented")
  }

  override fun toString(): String {
    return "Player(name='$name') ${super.toString()}"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Player

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}

data class Region(val name: String, val pos1: Vec2i, val pos2: Vec2i) {
  fun contains(pos: Vec2i) = pos.x in pos1.x..pos2.x && pos.y in pos1.y..pos2.y
  fun contains(chunk: Chunk) = (chunk.x shl 4) + 8 in pos1.x..pos2.x && (chunk.z shl 4) + 8 in pos1.y..pos2.y
}

data class Chunk(val x: Int, val z: Int) {
  fun contains(pos: Vec2i) = pos.x shr 4 == x && pos.y shr 4 == z
}

interface Observer {
  fun observe(event: Event)
}

interface EventHandler : Observer {
  fun sendEvent(uuid: UUID, event: Event)
  fun receiveEvent(uuid: UUID, event: Event)
  override fun observe(event: Event) {
    sendEvent(UUID.randomUUID(), event)
  }
}



