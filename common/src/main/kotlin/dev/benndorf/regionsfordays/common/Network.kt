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
import java.net.SocketAddress


@ExperimentalSerializationApi
open class NettyServer<T : Any> {
  fun start(name: String, port: Int, handler: (Event, Connection<T>) -> Unit, connectCallback: ((Connection<T>) -> Unit)? = null) {
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
    println("$name: send $event")
    ctx.channel().writeAndFlush(event)
  }
}

class ChannelHandler<T : Any>(private val name: String, private val connectCallback: ((Connection<T>) -> Unit)?) : SimpleChannelInboundHandler<Event>() {
  var connection: Connection<T>? = null

  @Throws(Exception::class)
  override fun channelActive(ctx: ChannelHandlerContext) {
    connection = Connection(ctx, name)
    println("$name: [+] Channel connected: ${connection?.remoteAddress()}")
    connectCallback?.invoke(connection!!)
  }

  @Throws(Exception::class)
  override fun channelInactive(ctx: ChannelHandlerContext) {
    println("$name: [-] Channel disconnected: ${connection?.remoteAddress()}")
    this.connection = null
  }

  @Throws(Exception::class)
  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    if ("Connection reset" == cause.message) {
      println("$name: ${connection?.remoteAddress()}: Connection reset.")
    } else {
      println("$name: ${connection?.remoteAddress()}: Exception caught, closing channel.")
      cause.printStackTrace()
    }
    connection = null
    ctx.close()
  }

  @Throws(Exception::class)
  override fun channelRead0(ctx: ChannelHandlerContext, event: Event) {
    println("$name: incoming $event")
  }
}

@ExperimentalSerializationApi
class ProtoDecoder<T : Any>(val handler: (Event, Connection<T>) -> Unit) : ByteToMessageDecoder() {
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
    val connection = ctx.pipeline().get(ChannelHandler::class.java).connection!!

    val bytes = ByteArray(input.readableBytes())
    input.readBytes(bytes)

    val event = ProtoBuf.decodeFromByteArray<Event>(bytes)

    @Suppress("UNCHECKED_CAST")
    handler(event, connection as Connection<T>)
    out.add(event)

    if (input.readableBytes() > 0) {
      println("Didn't fully read event ${event.javaClass.simpleName}! ${input.readableBytes()} bytes to go")
      input.skipBytes(input.readableBytes())
    }
  }
}

@ExperimentalSerializationApi
class ProtoEncoder : MessageToByteEncoder<Event>() {
  override fun encode(ctx: ChannelHandlerContext, event: Event, out: ByteBuf) {
    try {
      out.writeBytes(ProtoBuf.encodeToByteArray(event))
    } catch (ex: Throwable) {
      println("wat")
      ex.printStackTrace()
    }
  }
}

class PacketLengthDecoder : ByteToMessageDecoder() {
  override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
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
    out.writeInt(msg.readableBytes())
    out.writeBytes(msg)
  }
}

@ExperimentalSerializationApi
class Pipeline<T : Any>(private val name: String, private val handler: (Event, Connection<T>) -> Unit, private val connectCallback: ((Connection<T>) -> Unit)?) :
  ChannelInitializer<SocketChannel>() {
  override fun initChannel(ch: SocketChannel) {
    val pipeline = ch.pipeline()

    pipeline.addLast("lengthDecoder", PacketLengthDecoder())
    pipeline.addLast("decoder", ProtoDecoder(handler))

    pipeline.addLast("lengthEncoder", PacketLengthEncoder())
    pipeline.addLast("encoder", ProtoEncoder())

    pipeline.addLast("handler", ChannelHandler(name, connectCallback))
  }
}
