package org.luaj.vm2

class ByteArrayPool(private val size: Int)  {

  private var pool: Array<ByteArray?> = arrayOfNulls(32)
  private var poolSize: Int = 0

  fun acquire(): ByteArray? {
      return when {
        poolSize == 0 -> ByteArray(size)
        else          -> pool[--poolSize]
      }
  }

  fun release(instance: ByteArray?) {
    val newIdx = poolSize++
    if (newIdx >=  pool.size) {
      val newPool = arrayOfNulls<ByteArray?>((pool.size * 1.5).toInt())
      System.arraycopy(pool, 0, newPool, 0, pool.size)
      pool = newPool
    }
    pool[newIdx] = instance
  }
}