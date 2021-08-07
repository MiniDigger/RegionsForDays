package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.Chunk
import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.UUID

class RegionPlayer(uuid: UUID, name: String, pos: Vec2i, areaOfInterest: Int) : Player(uuid, name, pos, areaOfInterest) {
  lateinit var channel: EventHandler
  var observingChunks: MutableSet<Chunk> = mutableSetOf()
  val observingEntities: MutableSet<UUID> = mutableSetOf()

  override fun observe(event: Event) {
    channel.receiveEvent(uuid, event)
  }
}
