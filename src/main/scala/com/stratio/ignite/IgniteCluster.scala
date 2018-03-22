package com.stratio.ignite

import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteRunnable
import org.apache.ignite.{IgniteCache, Ignition}

//Start instances and watch the message on console
object IgniteCluster extends App {

  val igniteConfig = new IgniteConfiguration()
  val cacheConfig = new CacheConfiguration("ignite")
  igniteConfig.setCacheConfiguration(cacheConfig)
  val ignite = Ignition.start(igniteConfig)
  val igniteCluster = ignite.cluster() //IgniteCluster interface
  val cache: IgniteCache[Int, String] = igniteCluster.ignite().getOrCreateCache[Int, String]("ignite")

  ignite.compute(igniteCluster.forRemotes()).broadcast(new IgniteRunnable {
    override def run(): Unit = println(s"Hello node ${igniteCluster.localNode()}, " +
      s"this message had been send by igniteCompute broadcast ${cache.get(4)}")
  })

}
