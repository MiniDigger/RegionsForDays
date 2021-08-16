package dev.benndorf.regionsfordays.common

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import mu.KotlinLogging
import org.slf4j.MDC
import java.net.SocketAddress

private val logger = KotlinLogging.logger {}
@ExperimentalSerializationApi
open class NettyServer<T : Any> {
  fun start(name: String, port: Int, handler: (Event, Connection<T>) -> Unit, connectCallback: ((Connection<T>) -> Unit)? = null) {
    MDC.put("context", name)
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()

    try {
      val bootstrap = ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel::class.java)
        .childHandler(Pipeline(name, handler, connectCallback))
      bootstrap.bind(port).sync().channel().closeFuture().sync()
    } catch (e: InterruptedException) {
      e.printStackTrace()
    } finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }
}

@ExperimentalSerializationApi
open class NettyClient<T : Any> {
  fun start(name: String, hostname: String, port: Int, handler: (Event, Connection<T>) -> Unit, connectCallback: ((Connection<T>) -> Unit)? = null) {
    MDC.put("context", name)
    val group: EventLoopGroup = NioEventLoopGroup()

    try {
      val bootstrap: Bootstrap = Bootstrap()
        .group(group)
        .channel(NioSocketChannel::class.java)
        .handler(Pipeline(name, handler, connectCallback))
      bootstrap.connect(hostname, port).sync().channel().closeFuture().sync()
    } catch (e: InterruptedException) {
      e.printStackTrace()
    } finally {
      group.shutdownGracefully()
    }
  }
}

class Connection<T : Any>(private val ctx: ChannelHandlerContext, private val name: String) {
  lateinit var player: Player
  lateinit var context: T

  fun remoteAddress(): SocketAddress = ctx.channel().remoteAddress()
  fun sendEvent(event: Event) {
    MDC.put("context", name)
    logger.debug { "send $event" }
    ctx.channel().writeAndFlush(event)
  }
}

class ChannelHandler<T : Any>(val handler: (Event, Connection<T>) -> Unit, private val connectCallback: ((Connection<T>) -> Unit)?) : SimpleChannelInboundHandler<Event>() {
  var connection: Connection<T>? = null

  @Throws(Exception::class)
  override fun channelActive(ctx: ChannelHandlerContext) {
    MDC.put("context", ctx.name())
    connection = Connection(ctx, ctx.name())
    logger.info { "[+] Channel connected: ${connection?.remoteAddress()}" }
    connectCallback?.invoke(connection!!)
  }

  @Throws(Exception::class)
  override fun channelInactive(ctx: ChannelHandlerContext) {
    MDC.put("context", ctx.name())
    logger.info { "[-] Channel disconnected: ${connection?.remoteAddress()}" }
    this.connection = null
  }

  @Throws(Exception::class)
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    MDC.put("context", ctx.name())
    if ("Connection reset" == cause.message) {
      logger.warn { "${connection?.remoteAddress()}: Connection reset." }
    } else {
      logger.warn { "${connection?.remoteAddress()}: Exception caught, closing channel." }
      cause.printStackTrace()
    }
    connection = null
    ctx.close()
  }

  @Throws(Exception::class)
  override fun channelRead0(ctx: ChannelHandlerContext, event: Event) {
    MDC.put("context", ctx.name())
    logger.debug { "incoming $event" }
    @Suppress("UNCHECKED_CAST")
    handler(event, connection as Connection<T>)
  }
}

@ExperimentalSerializationApi
class ProtoDecoder : ByteToMessageDecoder() {
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    MDC.put("context", ctx.name())

    val bytes = ByteArray(input.readableBytes())
    input.readBytes(bytes)

    val event = ProtoBuf.decodeFromByteArray<Event>(bytes)

    out.add(event)

    if (input.readableBytes() > 0) {
      logger.warn { "Didn't fully read event ${event.javaClass.simpleName}! ${input.readableBytes()} bytes to go" }
      input.skipBytes(input.readableBytes())
    }
  }
}

@ExperimentalSerializationApi
class ProtoEncoder : MessageToByteEncoder<Event>() {
  override fun encode(ctx: ChannelHandlerContext, event: Event, out: ByteBuf) {
    MDC.put("context", ctx.name())
    try {
      out.writeBytes(ProtoBuf.encodeToByteArray(event))
    } catch (ex: Throwable) {
      logger.error { "wat" }
      ex.printStackTrace()
    }
  }
}

class PacketLengthDecoder : ByteToMessageDecoder() {
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    MDC.put("context", ctx.name())
    input.markReaderIndex()
    val len = input.readInt()
    if (input.readableBytes() < len) {
      input.resetReaderIndex()
    } else {
      out.add(input.readBytes(len))
    }
  }
}

class PacketLengthEncoder : MessageToByteEncoder<ByteBuf>() {
  override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
    MDC.put("context", ctx.name())
    out.writeInt(msg.readableBytes())
    out.writeBytes(msg)
  }
}

@ExperimentalSerializationApi
class Pipeline<T : Any>(private val name: String, private val handler: (Event, Connection<T>) -> Unit, private val connectCallback: ((Connection<T>) -> Unit)?) :
  ChannelInitializer<SocketChannel>() {
  override fun initChannel(ch: SocketChannel) {
    MDC.put("context", name)
    val pipeline = ch.pipeline()

    pipeline.addLast("lengthDecoder", PacketLengthDecoder())
    pipeline.addLast("decoder", ProtoDecoder())

    pipeline.addLast("lengthEncoder", PacketLengthEncoder())
    pipeline.addLast("encoder", ProtoEncoder())

    pipeline.addLast(name, ChannelHandler(handler, connectCallback))
  }
}
