package org.luaj.vm2

object PairVarargsPool  {
  private const val SIZE = 2000
  private val mPool: Array<Varargs.PairVarargs?> = arrayOfNulls(SIZE)
  private var mPoolSize: Int = 0

  fun acquire(v1: LuaValue?, v2: Varargs?): Varargs.PairVarargs? {
      return when {
        mPoolSize == 0 -> Varargs.PairVarargs(v1, v2)
        else           -> {
          val instance = mPool[--mPoolSize]!!
          instance.v1 = v1
          instance.v2 = v2
          instance
        }
      }
  }

  fun release(instance: Varargs.PairVarargs?) {
    mPool[mPoolSize++] = instance
    instance ?: return
    instance.v2 = null
    instance.v1 = null
  }
}