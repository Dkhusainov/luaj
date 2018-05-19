package org.luaj.vm2

import java.util.*

object LuajOptimizations {

  lateinit var luaThread: Thread
  var generator = false

  const val MAX_SIZE_OF_CACHED_STRING = 160

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

  private val buffer = ByteArray(4 * 1024)
  fun toLuaString(string: String): LuaString {
    val chars = string.toCharArray()
    val length = LuaString.lengthAsUtf8(chars)
    val buf = when {
      generator -> ByteArray(string.length * 4)
      else      -> buffer
    }
    LuaString.encodeToUtf8(chars, chars.size, buf, 0)
    val actualStringBytes = Arrays.copyOf(buf, length)
    return LuaString(actualStringBytes, 0, length)
  }

  private val sb = StringBuilder()
  private val charBuffer = CharArray(2 * 1024)
  @JvmStatic
  fun concatLuaStrings(one: LuaString, two: LuaString): LuaString {
    checkThread()

    sb.setLength(0)

    val buf = when {
      generator -> CharArray((one.m_length + two.m_length) * 4)
      else      -> charBuffer
    }
    one.decodeAsUtf8Into(buf)
    sb.append(buf, 0, one.m_length)

    two.decodeAsUtf8Into(buf)
    sb.append(buf, 0, two.m_length)

    return getOrCreateLuaString(sb.toString())
  }

  private inline fun checkThread() {
//    if (Thread.currentThread() !== luaThread) error(Thread.currentThread())
  }

  private inline fun <K, V> MutableMap<K, V>.getOrCreate(key: K, create: (K) -> V): V =
    get(key) ?: create(key).also { put(key, it) }
}