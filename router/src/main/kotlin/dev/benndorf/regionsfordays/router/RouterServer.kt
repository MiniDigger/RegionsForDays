package dev.benndorf.regionsfordays.router

import dev.benndorf.regionsfordays.common.Connection
import dev.benndorf.regionsfordays.common.Region


class RouterServer(val region: Region, val address: Pair<String, Int>) {

  var connection: Connection<RouterServer>? = null
}
