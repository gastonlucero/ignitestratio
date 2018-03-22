package com.stratio.ignite

import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteRunnable
import org.apache.ignite.{IgniteCache, Ignition}

import scala.collection.JavaConverters._

object IgniteNodeWithAttr extends App {

  val igniteConfig = new IgniteConfiguration()
  val cacheConfig = new CacheConfiguration("ignite")
  igniteConfig.setUserAttributes(Map[String, Any]("ROLE" -> "MASTER").asJava)
  //igniteConfig.setUserAttributes(Map[String, Any]("ROLE" -> "WORKER").asJava) //Start another instance with this ROLE

  igniteConfig.setCacheConfiguration(cacheConfig)

  val ignite = Ignition.start(igniteConfig)
  val igniteCluster = ignite.cluster() //IgniteCluster interface
  val cache: IgniteCache[Int, String] = igniteCluster.ignite().getOrCreateCache[Int, String]("ignite")

  ignite.compute(igniteCluster.forAttribute("ROLE", "WORKER")).broadcast(new IgniteRunnable {
    override def run(): Unit = println(s"Hello slave node ${igniteCluster.localNode()}," +
      s"this message had been send by ignite master node")
  })


}
