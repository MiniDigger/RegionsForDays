package dev.benndorf.regionsfordays.server

import dev.benndorf.regionsfordays.common.ActionEvent
import dev.benndorf.regionsfordays.common.Chunk
import dev.benndorf.regionsfordays.common.Event
import dev.benndorf.regionsfordays.common.EventHandler
import dev.benndorf.regionsfordays.common.JoinAction
import dev.benndorf.regionsfordays.common.MoveAction
import dev.benndorf.regionsfordays.common.ObjectInvisibleEvent
import dev.benndorf.regionsfordays.common.ObjectVisibleEvent
import dev.benndorf.regionsfordays.common.Observer
import dev.benndorf.regionsfordays.common.PositionEvent
import dev.benndorf.regionsfordays.common.Region
import dev.benndorf.regionsfordays.common.Vec2i
import java.util.UUID

class RegionServer(val region: Region) {

  val players: MutableList<RegionPlayer> = mutableListOf()

  val watchers: MutableMap<Chunk, MutableList<Observer>> = mutableMapOf()

  fun start() {

  }

  fun incoming(event: Event, channel: EventHandler) {
    when(event) {
      is ActionEvent -> {
        when(event.action) {
          is JoinAction -> {
            // todo load these values from perisistence
            val player = RegionPlayer(event.action.player.uuid, event.action.player.name, event.action.player.pos, event.action.player.areaOfInterest)
            player.channel = channel
            players.add(player)
            println("${region.name}: ${event.action.player.name} joined")
            updateObserverList(player)
            emitEvent(player.pos, ObjectVisibleEvent(player, 100, player.pos))
          }
          is MoveAction -> {
            val player = findPlayer(event.action.player.uuid) ?: throw RuntimeException("Not connected?!")
            val oldPos = player.pos
            val oldChunk = Chunk(player.pos.x shr 4, player.pos.y shr 4)
            player.pos = (event.action as MoveAction).newPos
            val newChunk = Chunk(player.pos.x shr 4, player.pos.y shr 4)
            // if we crossed a chunk boundary
            if(oldChunk != newChunk) {
              // start watching the new chunks and unwatching old ones
              updateObserverList(player)
              // we might need to notify that some new entity should be displayed
              watchers[oldChunk]?.forEach {
                if(it is RegionPlayer && it.observingEntities.contains(player.uuid)) {
                  it.observe(ObjectInvisibleEvent(player, player.areaOfInterest, oldPos))
                  it.observingEntities.remove(player.uuid)
                }
              }
              watchers[newChunk]?.forEach {
                if(it is RegionPlayer && !it.observingEntities.contains(player.uuid)) {
                  it.observe(ObjectVisibleEvent(player, player.areaOfInterest, oldPos))
                  it.observingEntities.add(player.uuid)
                }
              }
            }
            emitEvent(player.pos, PositionEvent(player, player.pos))
          }
        }
      }
    }
  }

  fun updateObserverList(player: RegionPlayer) {
    // find all chunks in range
    val chunks = findChunksInRange(player.pos, player.areaOfInterest)

    // look whats new and whats old
    val oldChunks = mutableListOf<Chunk>()
    val newChunks = mutableListOf<Chunk>()
    player.observingChunks.forEach {
      if(!chunks.contains(it)) {
        oldChunks.add(it)
      }
    }
    chunks.forEach {
      if(!player.observingChunks.contains(it)) {
        newChunks.add(it)
      }
    }

    newChunks.forEach { chunk ->
      // add to observer list of new chunks
      watchers.computeIfAbsent(chunk) { mutableListOf() }.add(player)
      player.observingChunks.add(chunk)
      // check if we need to send events for loaded game object
      findGameObjectInChunk(chunk).forEach {
        player.observingEntities.add(it.uuid)
        player.observe(ObjectVisibleEvent(it, it.areaOfInterest, it.pos))
      }
    }

    oldChunks.forEach { chunk ->
      // remove from chunks out of range
      watchers[chunk]?.remove(player)
      player.observingChunks.remove(chunk)
      // check if we need to send events to unload stuff
      findGameObjectInChunk(chunk).forEach {
        player.observingEntities.remove(it.uuid)
        player.observe(ObjectInvisibleEvent(it, it.areaOfInterest, it.pos))
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
    for(x in -chunkRange..chunkRange) {
      for(z in -chunkRange..chunkRange) {
        chunks.add(Chunk(baseX + x, baseZ + z))
      }
    }
    return chunks
  }

  fun findObservers(pos: Vec2i) = watchers[Chunk(pos.x shr 4, pos.y shr 4)]

  fun findPlayer(uuid: UUID) = players.find { it.uuid == uuid }

  fun findGameObjectInChunk(chunk: Chunk) = players.filter { chunk.contains(it.pos) }
}
