package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.*

class RouterPlayer(uuid: UUID, name: String, pos: Vec2i) : Player(uuid, name, pos) {
  lateinit var channel: EventHandler
  lateinit var server: RouterServer
}
