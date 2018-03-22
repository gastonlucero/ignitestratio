package com.stratio.ignite

import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteRunnable
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import org.apache.ignite.{IgniteCache, Ignition}

//Start instances and watch the message on console
//Change multicast ip and pay attention to topology snapshot
object IgniteCluster extends App {

  val igniteConfig = new IgniteConfiguration()
  val cacheConfig = new CacheConfiguration("ignite")
  val spi = new TcpDiscoverySpi()
  val ipFinder = new TcpDiscoveryMulticastIpFinder()
  ipFinder.setMulticastGroup("228.10.10.158")
  spi.setIpFinder(ipFinder)
  igniteConfig.setDiscoverySpi(spi)
  igniteConfig.setCacheConfiguration(cacheConfig)
  val ignite = Ignition.start(igniteConfig)
  val igniteCluster = ignite.cluster() //IgniteCluster interface
  val cache: IgniteCache[Int, String] = igniteCluster.ignite().getOrCreateCache[Int, String]("ignite")

  ignite.compute(igniteCluster).broadcast(new IgniteRunnable {
    override def run(): Unit = println(s"Hello node ${igniteCluster.localNode()}, " +
      s"this message had been send by igniteCompute broadcast ${cache.get(4)}")
  })

}
