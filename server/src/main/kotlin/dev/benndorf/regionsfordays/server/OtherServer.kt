package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.*
import dev.benndorf.regionsfordays.common.Observer
import java.util.*

class OtherServer(val region: Region, val con: Connection<OtherServer>, override val uuid: UUID): Observer {
  override fun observe(event: Event) {
    con.sendEvent(event)
  }
}
