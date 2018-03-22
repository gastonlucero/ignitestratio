package com.stratio.ignite

import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.query.annotations.{QuerySqlField, QueryTextField}
import org.apache.ignite.cache.query.{ScanQuery, SqlQuery, TextQuery}
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteBiPredicate
import org.apache.ignite.{IgniteCache, Ignition}

import scala.annotation.meta.field
import scala.collection.JavaConverters._

object IgniteSql extends App {
  val igniteConfig = new IgniteConfiguration()
  val cacheConfig = new CacheConfiguration("ignite")
  cacheConfig.setCacheMode(CacheMode.REPLICATED)
  cacheConfig.setIndexedTypes(Seq(classOf[String], classOf[IotDevice]): _*)
  igniteConfig.setCacheConfiguration(cacheConfig)
  val ignite = Ignition.start(igniteConfig)
  val cacheIot: IgniteCache[String, IotDevice] = ignite.getOrCreateCache[String, IotDevice]("ignite")

  val temp1 = IotDevice(name = "temp1", gpio = "123ASD", sensorType = "temperature", model = "testTemp")
  cacheIot.put(temp1.gpio, temp1)

  val temp2 = IotDevice(name = "temp2", gpio = "456ASD", sensorType = "temperature", model = "testTemp")
  cacheIot.put(temp2.gpio, temp2)

  val sqlText = s"sensorType = 'temperature'"
  val sql = new SqlQuery[String, IotDevice](classOf[IotDevice], sqlText)
  val temperatureQueryResult = cacheIot.query(sql).getAll.asScala.map(_.getValue)
  println(s"SqlQuery = $temperatureQueryResult")

  val cursor = cacheIot.query(new ScanQuery(new IgniteBiPredicate[String, IotDevice] {
    override def apply(key: String, entryValue: IotDevice): Boolean = entryValue.sensorType == "temperature"
  }
  ))
  val temperatureScanResult = cursor.getAll.asScala
  println(s"ScanQuery = $temperatureScanResult")

  val textQuery = new TextQuery[IotDevice, String](classOf[IotDevice], "temperature")
  val temperatureTextResult = cacheIot.query(textQuery).getAll.asScala
  println(s"TextQuery = $temperatureTextResult") //all devices with sensorType = temperature

}


case class IotDevice(@(QuerySqlField@field)(index = true) name: String,
                     @(QuerySqlField@field)(index = true) gpio: String,
                     @(QueryTextField@field) sensorType: String,
                     @(QueryTextField@field) model: String)