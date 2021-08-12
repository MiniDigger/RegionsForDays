package dev.benndorf.regionsfordays.common

import io.ktor.utils.io.core.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.payload.metadata
import io.rsocket.kotlin.transport.local.LocalServer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.util.*

fun Payload(data: String, metadata: String? = null): Payload = buildPayload {
  data(data)
  if (metadata != null) metadata(metadata)
}

@ExperimentalSerializationApi
open class EventProcessor {
  lateinit var rSocket: RSocket

  suspend fun sendEvent(event: Event) {
    rSocket.fireAndForget(Payload(ByteReadPacket(ProtoBuf.encodeToByteArray(event))))
  }

  fun decodeEvent(payload: Payload) = ProtoBuf.decodeFromByteArray<Event>(payload.data.readBytes())

  suspend fun connect(server: LocalServer) {
    rSocket = RSocketConnector().connect(server)
  }
}
