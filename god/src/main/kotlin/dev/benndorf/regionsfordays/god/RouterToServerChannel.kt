package dev.benndorf.regionsfordays.god

import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.router.Router
import dev.benndorf.regionsfordays.server.RegionServer
import java.util.UUID

class RouterToServerChannel(private val router: Router, private val regionServer: RegionServer): EventHandler {
  override fun sendEvent(uuid: UUID, event: Event) {
    regionServer.incoming(event, this)
  }

  override fun receiveEvent(uuid: UUID, event: Event) {
    router.receiveEventFromServer(event, uuid)
  }

  override fun toString(): String {
    return "RouterToServerChannel(router=${router.name}, regionServer=${regionServer.region.name})"
  }


}
