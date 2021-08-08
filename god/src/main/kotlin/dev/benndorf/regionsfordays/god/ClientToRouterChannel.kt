package dev.benndorf.regionsfordays.god

import dev.benndorf.regionsfordays.client.ClientPlayer
import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.router.Router
import dev.benndorf.regionsfordays.router.RouterPlayer
import java.util.*

class ClientToRouterChannel(private val clientPlayer: ClientPlayer, private val router: Router) : EventHandler {
  override fun sendEvent(uuid: UUID, event: Event) {
    // default to data from client here if router doesnt know the client yet, on join
    router.incomming(router.findClient(clientPlayer.uuid)
      ?: RouterPlayer(clientPlayer.uuid, clientPlayer.name, clientPlayer.pos), event, this)
  }

  override fun receiveEvent(uuid: UUID, event: Event) {
    clientPlayer.incomming(event)
  }

  override fun toString(): String {
    return "ClientToRouterChannel(clientPlayer=${clientPlayer.name}, router=${router.name})"
  }
}
