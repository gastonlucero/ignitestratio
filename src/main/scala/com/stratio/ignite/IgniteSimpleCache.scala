package com.stratio.ignite

import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.{IgniteCache, Ignition}


//Start one instance
//Start another instance withput for loop code
object IgniteSimpleCache extends App {

  val igniteConfig = new IgniteConfiguration()
  val cacheConfig = new CacheConfiguration("ignite")
  igniteConfig.setCacheConfiguration(cacheConfig)
  val ignite = Ignition.start(igniteConfig)
  val cache: IgniteCache[Int, String] = ignite.getOrCreateCache[Int, String]("ignite")
  for (i <- 1 to 10) {
    cache.put(i, s"value-$i")
  }
  println(s"From cache 7 -> ${cache.get(7)}")
  println(s"Not exists 20 -> ${cache.get(20)}")

}
