package dev.benndorf.regionsfordays.god

import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.server.RegionServer
import java.util.*

class ServerToServerChannel(private val source: RegionServer, private val target: RegionServer) : EventHandler {
  override fun sendEvent(uuid: UUID, event: Event) {
    target.incoming(event, this)
  }

  override fun receiveEvent(uuid: UUID, event: Event) {
    source.incoming(event, this)
  }

  override fun toString(): String {
    return "ServerToServerChannel(source=${source.region.name}, target=${target.region.name})"
  }
}
