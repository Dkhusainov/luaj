package org.luaj.vm2

import java.util.*

object LuajOptimizations {

  lateinit var luaThread: Thread

  const val MAX_SIZE_OF_CACHED_STRING = 160
  private val BUFFER_SIZE = 2 * 1024

  @JvmStatic val intArray32 = IntArray(32)

  private val stacks = mutableMapOf<Int, StackPool>()
  @JvmStatic
  fun acquireStackOfSize(size: Int): LuaStack {
    checkThread()
    return stacks.getOrCreate(size) { StackPool(size) }.acquire()
  }

  @JvmStatic
  fun releaseStackOfSize(size: Int, stack: LuaStack) {
    checkThread()
    stacks.getOrCreate(size) { StackPool(size) }.release(stack)
  }

  private val buffers = mutableMapOf<Int, ByteArrayPool>()
  @JvmStatic
  fun acquireByteArray(size: Int): ByteArray? {
    checkThread()
    return buffers.getOrCreate(size) { ByteArrayPool(size) }.acquire()
  }

  @JvmStatic
  fun releaseByteArray(arr: ByteArray) {
    checkThread()
    buffers.getOrCreate(arr.size) { ByteArrayPool(arr.size) }.release(arr)
  }


  @JvmStatic
  fun acquirePairVarargs(v1: LuaValue, v2: Varargs): Varargs.PairVarargs? {
    checkThread()
    return PairVarargsPool.acquire(v1, v2)
  }

  @JvmStatic
  fun releasePairVarargs(instance: Varargs.PairVarargs) {
    checkThread()
    PairVarargsPool.release(instance)
  }

  private val stringCache = HashMap<String, LuaString>()
  @JvmStatic
  fun getOrCreateLuaString(string: String): LuaString {
    checkThread()
    return when {
      string.length > MAX_SIZE_OF_CACHED_STRING -> toLuaString(string)
      else                                      -> stringCache.getOrCreate(string, ::toLuaString)
    }
  }

  private val buffer = ByteArray(BUFFER_SIZE)
  fun toLuaString(string: String): LuaString {
    val chars = string.toCharArray()
    val length = LuaString.lengthAsUtf8(chars)
    val buf = when {
      length > buffer.size -> ByteArray(length * 2)
      else                 -> buffer
    }
    LuaString.encodeToUtf8(chars, chars.size, buf, 0)
    val actualStringBytes = Arrays.copyOf(buf, length)
    return LuaString(actualStringBytes, 0, length)
  }

  private val sb = StringBuilder()
  private val charBuffer = CharArray(BUFFER_SIZE)
  @JvmStatic
  fun concatLuaStrings(one: LuaString, two: LuaString): LuaString {
    checkThread()

    sb.setLength(0)

    val total = one.m_length + two.m_length
    val buf = when {
      total >= charBuffer.size -> CharArray(total * 2)
      else                     -> charBuffer
    }
    //todo функция LuaString.decodeAsUtf8 может вернуть меньше символов чем получила, поэтому берем каунт с нее
    val n1 = one.decodeAsUtf8Into(buf)
    sb.append(buf, 0, n1)

    val n2 = two.decodeAsUtf8Into(buf)
    sb.append(buf, 0, n2)

    return getOrCreateLuaString(sb.toString())
  }

  private inline fun checkThread() {
//    if (Thread.currentThread() !== luaThread) error(Thread.currentThread())
  }

  private inline fun <K, V> MutableMap<K, V>.getOrCreate(key: K, create: (K) -> V): V =
    get(key) ?: create(key).also { put(key, it) }
}