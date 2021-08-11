package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.*
import dev.benndorf.regionsfordays.common.Observer
import java.util.*

class RegionServer(val region: Region) {

  val players: MutableList<RegionPlayer> = mutableListOf()

  val watchers: MutableMap<Chunk, MutableList<Observer>> = mutableMapOf()

  val neighbors: MutableMap<Region, EventHandler> = mutableMapOf()

  val borderChunks: MutableSet<MutableTriple<Chunk, Int, EventHandler?>> = mutableSetOf()

  fun discoverNeighbors(neighbors: Map<Region, EventHandler>) {
    this.neighbors.clear()
    this.neighbors.putAll(neighbors)
  }

  fun start() {
//    subscribeNeighborServersToBorderChunks()
  }

  fun subscribeNeighborServersToBorderChunks() {
    // find border chunks
    val distance = (viewDistance / 2) * (viewDistance / 2)
    for (x in (region.pos1.x + 8) until (region.pos2.x + 8) step 16) {
      for (y in (region.pos1.y + 8) until (region.pos2.y + 8) step 16) {
        val pos = Vec2i(x, y)
        if (pos.distanceSquared(Vec2i(region.pos1.x, y)) < distance) borderChunks.add(MutableTriple(Chunk(x shr 4, y shr 4), 1, null))
        if (pos.distanceSquared(Vec2i(x, region.pos1.y)) < distance) borderChunks.add(MutableTriple(Chunk(x shr 4, y shr 4), 2, null))
        if (pos.distanceSquared(Vec2i(region.pos2.x, y)) < distance) borderChunks.add(MutableTriple(Chunk(x shr 4, y shr 4), 3, null))
        if (pos.distanceSquared(Vec2i(x, region.pos2.y)) < distance) borderChunks.add(MutableTriple(Chunk(x shr 4, y shr 4), 4, null))
      }
    }

    // map directions to servers
    neighbors.forEach {

    }

    // for every border chunk, find the server that is closest
    borderChunks.forEach {
      // TODO huge hack, I don't care
      if ((region.name == "Left Region" && it.second == 3) || region.name == "Right Region" && it.second == 1) {
        it.third = neighbors.values.first()
        watchers.computeIfAbsent(it.first) { mutableListOf() }.add(neighbors.values.first())
      }
    }
  }

  fun incoming(event: Event, channel: EventHandler) {
    when (event) {
      is ActionEvent -> {
        when (event.action) {
          is JoinAction -> {
            // todo load these values from perisistence
            val player = RegionPlayer(event.action.player.uuid, event.action.player.name, event.action.player.pos)
            player.channel = channel
            players.add(player)
            println("${region.name}: ${event.action.player.name} joined")
            updateObserverList(player)
            emitEvent(player.pos, ObjectVisibleEvent(player, viewDistance, player.pos))
          }
          is MoveAction -> {
            val player = findPlayer(event.action.player.uuid) ?: throw RuntimeException("Not connected?!")
            val oldPos = player.pos
            player.pos = (event.action as MoveAction).newPos
            val oldChunk = Chunk(oldPos.x shr 4, oldPos.y shr 4)
            val newChunk = Chunk(player.pos.x shr 4, player.pos.y shr 4)
            // if we crossed a chunk boundary
            if (oldChunk != newChunk) {
              // start watching the new chunks and unwatching old ones
              updateObserverList(player)

              // TODO check if we moved into a new server
              if (!region.contains(newChunk)) {
                println("we moved out of region?!")
                player.pos = oldPos
                return
              }

              // check if the new chunk contains entities we need to load
              checkHideAndShowForWatcher(oldChunk, newChunk, player, oldPos)
            }
            emitEvent(player.pos, PositionEvent(player, player.pos))
          }
        }
      }
      is SubRequestEvent -> {
        // TODO need to actually open a channel here
        watchers.computeIfAbsent(event.chunk) { mutableListOf() }.add(event.player)
        event.player.observe(ChunkLoadEvent(event.chunk, findChunkData(event.chunk)))
      }
      is UnsubRequestEvent -> {
        // TODO need to actually close a channel here
        watchers[event.chunk]?.remove(event.player)
        event.player.observe(ChunkUnloadEvent(event.chunk))
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
      if (it is RegionPlayer && it.observingEntities.contains(player.uuid)) {
        wannaHide.add(it)
      }
    }
    watchers[newChunk]?.forEach {
      if (it is RegionPlayer) {
        if (wannaHide.contains(it)) {
          wannaHide.remove(it)
        } else if (!it.observingEntities.contains(player.uuid)) {
          it.observe(ObjectVisibleEvent(player, viewDistance, player.pos))
          it.observingEntities.add(player.uuid)
        }
      }
    }
    wannaHide.forEach {
      it.observe(ObjectInvisibleEvent(player, viewDistance, oldPos))
      it.observingEntities.remove(player.uuid)
    }
  }

  fun updateObserverList(player: RegionPlayer) {
    // find all chunks in range
    val chunks = findChunksInRange(player.pos, viewDistance)

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
        findChunkOwner(chunk).observe(SubRequestEvent(chunk, player))
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
        findChunkOwner(chunk).observe(UnsubRequestEvent(chunk, player))
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

  fun findPlayer(uuid: UUID) = players.find { it.uuid == uuid }

  fun findGameObjectInChunk(chunk: Chunk) = players.filter { chunk.contains(it.pos) }

  fun findChunkData(chunk: Chunk) = ChunkData(findGameObjectInChunk(chunk))

  fun findChunkOwner(chunk: Chunk) = neighbors.filterKeys { it.contains(chunk) }.values.first()

  override fun toString(): String {
    return "RegionServer(region=${region.name})"
  }
}
