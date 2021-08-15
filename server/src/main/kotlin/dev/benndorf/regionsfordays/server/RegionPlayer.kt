package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.*
import dev.benndorf.regionsfordays.common.Observer
import java.util.*

class RegionPlayer(val player: Player) : Observer {
  lateinit var connection: Connection<RegionPlayer>

  // all chunks a player has in his area of interest. CAN BE IN ANOTHER REGION!
  var observingChunks: MutableSet<Chunk> = mutableSetOf()
  val observingEntities: MutableSet<UUID> = mutableSetOf()

  override val uuid: UUID
    get() = player.uuid

  override fun observe(event: Event) {
    connection.sendEvent(ServerEvent(event, uuid))
  }
}
