package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.*
import dev.benndorf.regionsfordays.common.Observer
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.*
import kotlin.concurrent.thread

@ExperimentalSerializationApi
class RegionServer(val region: Region) : NettyServer<RegionPlayer>() {

  val players: MutableList<RegionPlayer> = mutableListOf()

  val watchers: MutableMap<Chunk, MutableList<Observer>> = mutableMapOf()

  val neighbors: MutableMap<Region, Pair<String, Int>> = mutableMapOf()
  val neighborConnections: MutableMap<Region, Connection<OtherServer>> = mutableMapOf()

  val borderChunks: MutableSet<MutableTriple<Chunk, Int, EventHandler?>> = mutableSetOf()

  fun discoverNeighbors(neighbors: Map<Region, Pair<String, Int>>) {
    this.neighbors.clear()
    this.neighbors.putAll(neighbors)
    this.neighborConnections.clear()
  }

  fun start(name: String, port: Int) {
    start(name, port, { event, connection -> incoming(event, connection) })
  }

  fun incoming(event: Event, connection: Connection<out Any>) {
    when (event) {
      is ActionEvent -> {
        when (event.action) {
          is JoinAction -> {
            // todo load these values from perisistence
            val player = RegionPlayer(Player(event.action.player.uuid, event.action.player.name, event.action.player.pos))
            @Suppress("UNCHECKED_CAST")
            player.connection = connection as Connection<RegionPlayer>
            connection.player = player.player
            connection.context = player
            players.add(player)
            println("${region.name}: ${event.action.player.name} joined")
            updateObserverList(player)
            emitEvent(player.player.pos, ObjectVisibleEvent(player.player, viewDistance, player.player.pos))
          }
          is MoveAction -> {
            val player = findPlayer(event.action.player.uuid) ?: throw RuntimeException("Not connected?!")
            val oldPos = player.player.pos
            player.player.pos = (event.action as MoveAction).newPos
            val oldChunk = Chunk(oldPos.x shr 4, oldPos.y shr 4)
            val newChunk = Chunk(player.player.pos.x shr 4, player.player.pos.y shr 4)
            // if we crossed a chunk boundary
            if (oldChunk != newChunk) {
              // start watching the new chunks and unwatching old ones
              updateObserverList(player)

              // TODO check if we moved into a new server
              if (!region.contains(newChunk)) {
                println("we moved out of region?!")
                player.player.pos = oldPos
                return
              }

              // check if the new chunk contains entities we need to load
              checkHideAndShowForWatcher(oldChunk, newChunk, player, oldPos)
            }
            emitEvent(player.player.pos, PositionEvent(player.player, player.player.pos))
          }
        }
      }
      is ServerToServerAuthEvent -> {
        @Suppress("UNCHECKED_CAST")
        val con = connection as Connection<OtherServer>
        con.context = OtherServer(event.region, con, UUID.randomUUID())
        neighborConnections[region] = con
        println("${region.name} established connection to ${event.region.name}")
      }
      is SubRequestEvent -> {
        watchers.computeIfAbsent(event.chunk) { mutableListOf() }.add(connection.context as OtherServer)
        connection.sendEvent(ServerEvent(ChunkLoadEvent(event.chunk, findChunkData(event.chunk)), event.player.uuid))
      }
      is UnsubRequestEvent -> {
        watchers[event.chunk]?.remove(connection.context as OtherServer)
        connection.sendEvent(ServerEvent(ChunkUnloadEvent(event.chunk), event.player.uuid))
      }
      is ServerEvent -> {
        findPlayer(event.target)?.connection?.sendEvent(event.event)
      }
      else -> {
        println("${region.name} unknown event $event")
      }
    }
  }

  fun checkHideAndShowForWatcher(oldChunk: Chunk, newChunk: Chunk, player: RegionPlayer, oldPos: Vec2i) {
    // we might need to notify that some new entity should be or should no longer displayed
    // so we check if the there are watchers that watch old chunk, but not new chunk
    val wannaHide = mutableListOf<RegionPlayer>()
    watchers[oldChunk]?.forEach {
      if (it is RegionPlayer && it.observingEntities.contains(player.player.uuid)) {
        wannaHide.add(it)
      }
    }
    watchers[newChunk]?.forEach {
      if (it is RegionPlayer) {
        if (wannaHide.contains(it)) {
          wannaHide.remove(it)
        } else if (!it.observingEntities.contains(player.player.uuid)) {
          it.observe(ObjectVisibleEvent(player.player, viewDistance, player.player.pos))
          it.observingEntities.add(player.player.uuid)
        }
      }
    }
    wannaHide.forEach {
      it.observe(ObjectInvisibleEvent(player.player, viewDistance, oldPos))
      it.observingEntities.remove(player.player.uuid)
    }
  }

  fun updateObserverList(player: RegionPlayer) {
    // find all chunks in range
    val chunks = findChunksInRange(player.player.pos, viewDistance)

    // look whats new and whats old
    val oldChunks = mutableListOf<Chunk>()
    val newChunks = mutableListOf<Chunk>()
    player.observingChunks.forEach {
      if (!chunks.contains(it)) {
        oldChunks.add(it)
      }
    }
    chunks.forEach {
      if (!player.observingChunks.contains(it)) {
        newChunks.add(it)
      }
    }

    newChunks.forEach { chunk ->
      // keep track
      player.observingChunks.add(chunk)
      // check if we handle this chunk
      if (region.contains(chunk)) {
        // sub to new chunks
        watchers.computeIfAbsent(chunk) { mutableListOf() }.add(player)
        // load
        player.observe(ChunkLoadEvent(chunk, findChunkData(chunk)))
      } else {
        // tell the other server to open a channel and sub the player
        openChannel(chunk, SubRequestEvent(chunk, player.player))
      }
    }

    oldChunks.forEach { chunk ->
      // keep track
      player.observingChunks.remove(chunk)
      // check if we handle this chunk
      if (region.contains(chunk)) {
        // unsub from chunks out of range
        watchers[chunk]?.remove(player)
        // unload
        player.observe(ChunkUnloadEvent(chunk))
        // untrack entities
        findGameObjectInChunk(chunk).forEach { player.observingEntities.remove(it.uuid) }
      } else {
        // tell the other server to close the channel and unsub the player
        closeChannel(chunk, UnsubRequestEvent(chunk, player.player))
      }
    }
  }

  fun emitEvent(pos: Vec2i, event: Event) {
    findObservers(pos)?.forEach { it.observe(event) }
  }

  fun findChunksInRange(pos: Vec2i, range: Int): Set<Chunk> {
    val chunkRange = range shr 4 + 1
    val baseX = pos.x shr 4
    val baseZ = pos.y shr 4
    val chunks = mutableSetOf<Chunk>()
    for (x in -chunkRange..chunkRange) {
      for (z in -chunkRange..chunkRange) {
        chunks.add(Chunk(baseX + x, baseZ + z))
      }
    }
    return chunks
  }

  fun findChunksByObserver(observer: Observer) = watchers.filter { it.value.contains(observer) }

  fun findObservers(pos: Vec2i) = watchers[Chunk(pos.x shr 4, pos.y shr 4)]

  fun findPlayer(uuid: UUID) = players.find { it.player.uuid == uuid }

  fun findGameObjectInChunk(chunk: Chunk) = players.map { it.player }.filter { chunk.contains(it.pos) }

  fun findChunkData(chunk: Chunk) = ChunkData(findGameObjectInChunk(chunk))

  fun openChannel(chunk: Chunk, event: Event) {
    val (region, address) = neighbors.filter { it.key.contains(chunk) }.entries.first()
    val connection = neighborConnections[region]
    if (connection != null) {
      connection.sendEvent(event)
    } else {
      thread(name = "${this.region.name} -> ${region.name}") {
        NettyClient<OtherServer>().start("${this.region.name} -> ${region.name}", address.first, address.second, { e, c -> incoming(e, c) }, { con ->
          con.context = OtherServer(region, con, UUID.randomUUID())
          neighborConnections[region] = con
          con.sendEvent(ServerToServerAuthEvent(this.region))
          con.sendEvent(event)
        })
      }
    }
  }

  fun closeChannel(chunk: Chunk, event: Event) {
    val (region, address) = neighbors.filter { it.key.contains(chunk) }.entries.first()
    neighborConnections[region]?.sendEvent(event)
    // TODO check if we the last one, if so, close
  }

  override fun toString(): String {
    return "RegionServer(region=${region.name})"
  }
}
