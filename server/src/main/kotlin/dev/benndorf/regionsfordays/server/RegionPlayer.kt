package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.*
import java.util.*

class RegionPlayer(uuid: UUID, name: String, pos: Vec2i) : Player(uuid, name, pos) {
  lateinit var channel: EventHandler

  // all chunks a player has in his area of interest. CAN BE IN ANOTHER REGION!
  var observingChunks: MutableSet<Chunk> = mutableSetOf()
  val observingEntities: MutableSet<UUID> = mutableSetOf()

  override fun observe(event: Event) {
    channel.receiveEvent(uuid, event)
  }
}
