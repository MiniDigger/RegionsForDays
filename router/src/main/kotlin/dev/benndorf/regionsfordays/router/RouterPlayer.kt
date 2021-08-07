package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.UUID

class RouterPlayer(uuid: UUID, name: String, pos: Vec2i, areaOfInterest: Int) : Player(uuid, name, pos, areaOfInterest) {
  lateinit var channel: EventHandler
  lateinit var server: RouterServer
}
