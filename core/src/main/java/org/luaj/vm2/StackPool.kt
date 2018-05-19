package org.luaj.vm2

import java.util.*

class StackPool(private val size: Int)  {

  private var pool: Array<LuaStack> = arrayOfNulls(32)
  private var poolSize: Int = 0

  fun acquire(): LuaStack {
      return when {
        poolSize == 0 -> arrayOfNulls(size)
        else          -> pool[--poolSize]
      }
  }

  fun release(instance: LuaStack) {
    val newIdx = poolSize++
    if (newIdx >=  pool.size) {
      val newPool = arrayOfNulls<LuaStack>((pool.size * 1.5).toInt())
      System.arraycopy(pool, 0, newPool, 0, pool.size)
      pool = newPool
    }
    pool[newIdx] = instance
    Arrays.fill(instance, null)
  }
}

typealias LuaStack = Array<LuaValue?>?