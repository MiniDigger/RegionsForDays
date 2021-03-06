package dev.benndorf.regionsfordays.god

import dev.benndorf.regionsfordays.client.Client
import dev.benndorf.regionsfordays.common.Player
import dev.benndorf.regionsfordays.common.Region
import dev.benndorf.regionsfordays.common.Vec2i
import dev.benndorf.regionsfordays.common.viewDistance
import dev.benndorf.regionsfordays.router.Router
import dev.benndorf.regionsfordays.router.RouterServer
import dev.benndorf.regionsfordays.server.RegionServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.UIManager
import kotlin.concurrent.thread

fun main() {
  Main().start()
}

@ExperimentalSerializationApi
class Main {
  val dimension = Dimension(50 * 16, 50 * 16)

  val region1 = Region("Left Region", Vec2i(0, 0), Vec2i(25 * 16, 50 * 16))
  val region2 = Region("Right Region", Vec2i(25 * 16, 0), Vec2i(50 * 16, 50 * 16))
  val regions = listOf(region1, region2)

  val server1 = RegionServer(region1)
  val server2 = RegionServer(region2)
  val servers = listOf(server1, server2)

  val router1 = Router("Router 1")
  val router2 = Router("Router 2")
  val routers = listOf(router1, router2)

  val player1 = Player(UUID.fromString("11111111-1111-1111-1111-111111111111"), "Player 1", Vec2i(21 * 16 + 8, 25 * 16 + 8))
  val player2 = Player(UUID.fromString("22222222-2222-2222-2222-222222222222"), "Player 2", Vec2i(29 * 16 - 8, 25 * 16 + 8))
  val players = listOf(player1, player2)

  val client1 = Client(player1)
  val client2 = Client(player2)
  val clients = listOf(client1, client2)

  var mode = Mode.GOD
  var showGrid = false
  var showRegions = true
  var showBorderChunks = false

  fun start() = runBlocking {
    thread(name = "Server 1") { server1.start("Server 1", 1001) }
    thread(name = "Server 2") { server2.start("Server2", 1002) }
    // todo eventually the server does this...
    server1.discoverNeighbors(mapOf(region2 to Pair("localhost", 1002)))
    server2.discoverNeighbors(mapOf(region1 to Pair("localhost", 1001)))

    thread(name = "Router 1") { router1.start("Router 1", 2001) }
    thread(name = "Router 2") { router2.start("Router 2", 2002) }
    // todo eventually the router does this...
    router1.discoverServers(listOf(RouterServer(region1, Pair("localhost", 1001)), RouterServer(region2, Pair("localhost", 1002))))
    router2.discoverServers(listOf(RouterServer(region1, Pair("localhost", 1001)), RouterServer(region2, Pair("localhost", 1002))))
    router1.discoverPlayerState(players)
    router2.discoverPlayerState(players)

    thread(name = "Client 1") { client1.start("Client 1", "localhost", 2001) }
    thread(name = "Client 2") { client2.start("Client 2", "localhost", 2002) }

    createFrame()
  }

  fun createFrame() {
    EventQueue.invokeLater {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (ex: Exception) {
        ex.printStackTrace()
      }

      val frame = JFrame("RegionsForDays")
      frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
      frame.layout = BorderLayout()
      frame.addKeyListener(object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
          when (e.keyChar) {
            // player 1 movement
            'w' -> client1.move(Vec2i(0, -1))
            'a' -> client1.move(Vec2i(-1, 0))
            's' -> client1.move(Vec2i(0, 1))
            'd' -> client1.move(Vec2i(1, 0))
            // player 2 movement
            'i' -> client2.move(Vec2i(0, -1))
            'j' -> client2.move(Vec2i(-1, 0))
            'k' -> client2.move(Vec2i(0, 1))
            'l' -> client2.move(Vec2i(1, 0))
            // modes
            '1' -> mode = Mode.GOD
            '2' -> mode = Mode.PLAYER1
            '3' -> mode = Mode.PLAYER2
            // other options
            '4' -> showRegions = !showRegions
            '5' -> showGrid = !showGrid
            '6' -> showBorderChunks = !showBorderChunks
          }
          frame.repaint()
        }
      })
      frame.add(GodPane(this))
      frame.pack()
      frame.setLocationRelativeTo(null)
      frame.isVisible = true
    }
  }
}

@ExperimentalSerializationApi
class GodPane(private val main: Main) : JPanel() {
  override fun getPreferredSize(): Dimension {
    return main.dimension
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val graphics2D = g.create() as Graphics2D
    if (main.showGrid) {
      paintGrid(graphics2D)
    }
    if (main.showRegions) {
      paintRegions(graphics2D)
    }
    if (main.showBorderChunks) {
      paintBorderChunks(graphics2D)
    }
    paintPlayers(graphics2D)
    paintDebug(graphics2D)
  }

  private fun paintPlayers(g: Graphics2D) {
    when (main.mode) {
      Mode.GOD -> {
        for (client in main.clients) {
          paintPlayer(g, client.player)
        }
      }
      Mode.PLAYER1 -> {
        for (player in main.client1.players) {
          paintPlayer(g, player)
        }
      }
      Mode.PLAYER2 -> {
        for (player in main.client2.players) {
          paintPlayer(g, player)
        }
      }
    }
  }

  private fun paintPlayer(g: Graphics2D, player: Player) {
    g.color = Color.BLUE
    g.fillOval(player.pos.x - 5, player.pos.y - 5, 10, 10)
    g.drawOval(player.pos.x - (viewDistance / 2), player.pos.y - (viewDistance / 2), viewDistance, viewDistance)
    g.stroke = BasicStroke(1f)
    g.drawString(player.name, player.pos.x - 20, player.pos.y + 20)
  }

  private fun paintGrid(g: Graphics2D) {
    g.color = Color.BLACK
    for (i in 0..size.height step 16) {
      g.drawLine(0, i, size.height, i)
    }
    for (i in 0..size.width step 16) {
      g.drawLine(i, 0, i, size.width)
    }
  }

  private fun paintBorderChunks(g: Graphics2D) {
    g.color = Color.ORANGE
    main.servers.forEach { server ->
      server.borderChunks.forEach {
        g.fillOval((it.first.x shl 4) + 6, (it.first.z shl 4) + 6, 4, 4)
      }
    }
  }

  private fun paintRegions(g: Graphics2D) {
    for (server in main.servers) {
      paintRegion(g, server.region)
    }
  }

  private fun paintRegion(g: Graphics2D, region: Region) {
    g.color = Color.RED
    g.stroke = BasicStroke(3f)
    g.drawRect(region.pos1.x, region.pos1.y, (region.pos2.x - region.pos1.x), (region.pos2.y - region.pos1.y))
    g.stroke = BasicStroke(1f)
    g.drawString(region.name, region.pos1.x + 5, region.pos1.y + 15)
  }

  private fun paintDebug(g: Graphics2D) {
    g.color = Color.BLACK
    g.drawString("Mode: ${main.mode}", 5, size.height - 5)
  }
}

enum class Mode {
  GOD, PLAYER1, PLAYER2
}
