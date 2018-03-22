![](https://lh4.googleusercontent.com/xeszTFxMvQZCGpWc-_h6LIZpC8eKrHyU3mq46wUYGuuPV9kM_m7-o1-beYUl-8xHFpaEoId-Mt2NkJZxG04AmMvt44XLX0DWLOB86f-Rqw9IUeTrPjVUP-pE5X54cQDE_lPlzOJx)

# Apache Ignite : More than a simple cache


Let suppose we start to develop a webServer for our IOT App, with a few endpoints, like POST for receive events , GET devicesBySensorType, GET all, and PUT for update device metadata, etc.

At this time, a cache for common data could be secondary, but if we start to thinking in log term ,on improve performance or increase response times ( e.g when a service retrieve data from database) , the term cache is needed.

First solution, the always loyal HashMap class (or ConcurrentHashMap after the first ConcurrentModificationException) : this is the first we do, caching objects in different maps in memory , that give us quick benefits.

## Hello Data Grid

With **Ignite**, this is simple, only add the dependency on your scala/java project :

> sbt 

        "org.apache.ignite" % "ignite-core" % "2.4.0"

> maven

	     <dependency>
		    <groupId>org.apache.ignite</groupId>  
			<artifactId>ignite-core</artifactId>  
			<version>2.4.0</version>  
		 </dependency>
 
 Scala code for the example :
 
	object IgniteSimpleDataGrid extends App {
		val igniteConfig = new IgniteConfiguration()
		val cacheConfig = new CacheConfiguration("ignite")
		igniteConfig.setCacheConfiguration(cacheConfig)
		val ignite = Ignition.start(igniteConfig)
		val cache: IgniteCache[Int, String] = ignite.getOrCreateCache\[Int, String\]("ignite")
		for (i <- 1 to 10) {
			cache.put(i, s"value-$i")
		}
		println(s"From cache 7 -> ${cache.get(7)}") 
		println(s"Not exists 20 -> ${cache.get(20)}")
	}

  Run the class and see the output 
  **(Note: ONLY one running Ignite instance per JVM)**

![](https://lh4.googleusercontent.com/q6-X_UDx9h2whYnhN1X_5tYSj2mFfIZQITKde5KZmHyE2SY5uCShiGwUd56iqdNFi7COQenFhBQWD83-Gy8CtYqKtNAAOB2GeW9Wxdg9KKo10Le7DIIDuJLQlzw5FOXl1fphjBUm)

Perfect, we have our first single version of Ignite Data Grid, but what if, we need HA, and deploy another instance of the app? or imagine that metadata endpoint is related to another service, and uses the same information, we fall in the need of share data between the apps, sadly the maps cache schema, does not give us the solution, sounds familiar? , but, if I tell you, that with **in-memory data grid**, data could be distributed, and every node in the cluster (the nodes forms a ring topology) can access and maintain the shared information, besides of compute remote functions or custom predicates, it is possible?? The answer is absolutely yes,  and here is where Ignite distinguishes among other solutions.

Let test `IgniteSimpleDataGrid` class , with two instances(nodes), node1 is already running , so before start the app again (node2) , comment for each block, because node1, already populate the cache with values, this means, node2 only read from cache.
   
    /* for (i <- 1 to 10) {
     cache.put(i, s"value-$i")
     } */
    println(s"From cache 7 -> ${cache.get(7)}")
    println(s"Not exists 20 -> ${cache.get(20)}")


Pay attention the output in node2, console prints almost the same than node1 in first example (look the id, is different):
![](https://lh5.googleusercontent.com/Ou_1VToeGZPz94SIzwG17KWyL0oTPOyI5yO5U62aI28A1rQXJ6_GsLUUJlGj-jLUOcTKh4m2BhMNPdu_Kiyh_aMsLIlNDYfRfPX38fwgQcQHJqxB02OdxNJd5tna2FIJhv47CTS3)

An output in node1:
![](https://lh4.googleusercontent.com/MmtN_vC1EofS6rlt1toRFU2VWdkAruENC2OXGlUfPUIENit3BMnzw5YB7p-hb1WlO-mi9WaJGpeSfy1chGQ_eeSdVyAV11J2yMSEHzWIDAJg9M1l4SObBIrUMyFbXguyDop71-h4)

Both nodes prints this line
![](https://lh4.googleusercontent.com/6PvEK0tU1Hf4gVr8XaDvbn7BwDKTxNw2wO6BlumgUTkmZmQSdb7M5gN-_kbmX7HmaFWDHxjEbx_UvrOGUiaOIwQEwDkrFSNTVnx5FpkYCeuBm0NidS9dtDmKl33w8GpTuaW4f604)
Great!! , that means that we have our Ignite in-memory distributed key-value store cluster !!(more than a simple cache, and we see why). 
Nodes have been autodiscovered!! and they see the same data.
If we start more nodes, the topology will growing up, (each node with a unique id), and increasing servers numbers , servers =3, servers=4 ,...
![](https://lh4.googleusercontent.com/yRr3HodTy7y-B8r6EI1xuR-0HpehCy5ml7ktP9MKjMXBKKeBqYOZySIa1x6RJRqWpNh4QqM_HKxuH6nla0iMDhcazWQiNwTRYexZlNvtuihinqBp-aqPy3KX2-bFd7MQavxBG5ja)
If we stop, node1 , topology decreases, and the rest of running nodes print this change, because they are listening topology changes events.
![](https://lh4.googleusercontent.com/OnTLXkeeNWd6Nhgtbu8xQ4x4-ttBLvLA7p6NBEdPkgYjz35CloLZ277Ox4OAfy5LDV8bLMso9iDq6-LcoaL6yUYO2VHJkQg2n7OVm8PAfaQEBAduHwpmp3ZD9tknrSj_f4lUvFvF)
Ignite implements high availability, if a node does down, the data is then rebalanced around the cluster without any operation involved.

Let´s clarify this point.

>My definition of Ignite is that it's a distributed in-memory cache, query and processing platform for working with large-scale data sets in real-time (leaving aside, streaming processing, Spark integration, Machine learning grid, Ignite FileSystem, persistence, transactions,..)

How Ignite automagically create a cluster? well, it provides TcpDiscoverySpi as a default implementation of DiscoverySpi that uses TCP/IP for node discovery. Discovery SPI can be configured for Multicast and Static IP based node discovery. (Spi = especially Ip Finder)
It means nodes uses multicast to find each other:

	val igniteConfig = new IgniteConfiguration()
	val spi = new TcpDiscoverySpi()
	val ipFinder = new TcpDiscoveryMulticastIpFinder()
	ipFinder.setMulticastGroup("228.10.10.157") // Default value = DFLT_MCAST_GROUP = "228.1.2.4";
	spi.setIpFinder(ipFinder)
	cfg.setDiscoverySpi(spi)
	val ignite = Ignition.start(igniteConfig)
	val cache: IgniteCache\[Int, String\] = ignite.getOrCreateCache\[Int, String\]("ignite")

> [Multicast in Ignite](https://apacheignite.readme.io/docs/cluster-config#multicast-based-discovery)
[Wikipedia  - IP_Multicast](https://en.wikipedia.org/wiki/IP_multicast)

##  Ignite Ring Topology

![](https://lh3.googleusercontent.com/1QKmMiVgikzrG3Fq7F1ypxZq2ousfxk53R6cv88kHE-RlqLrpH4DFukVIb06rfOLGmu837ElaAzW6nmP0YTONtf4MSSzJIs9f0lwysbRSnq8H5G7djvLSb8B7hRjA3kIQ5ah07HA)

In the image, the nodes form a ring of *server* nodes, but in Ignite cluster nodes can have roles or belong to a cluster group, or could be *client* nodes (this means "outside" of the ring, and without the power of maintain data in cache, useful for external application).
Nodes can broadcast messages to other nodes in a particular group :

	val igniteCluster = ignite.cluster()
	//Send to cluster remotes nodes (ignoring this) this Code
	ignite.compute(igniteCluster.forRemotes()).broadcast(
			new IgniteRunnable {
	override def run(): Unit = println(s"Hello node ${igniteCluster.localNode()}, 
	this message had been send by igniteCompute broadcast")
	})

This example shows, when node starts, it send a message to cluster group **forRemotes**, to all other nodes, configured in *mode=server*, except this node, and you will see an exception on the first node the first time until a cluster had been created, don`t worry its normal).

Furthermore it is possible define nodes with your custom attributes:

	igniteConfig.setUserAttributes(Map[String,Any]("ROLE" -> "MASTER").asJava)
	ignite.compute(igniteCluster.forAttribute("ROLE","WORKER")).broadcast(new IgniteRunnable {
	override def run(): Unit = println(s"Hello worker node ${igniteCluster.localNode()}," +
	s"this message had been send by ignite master node")
	})
Here the node has attribute *ROLE = MASTER*, and broadcast only to nodes with *ROLE = WORKER*

Nodes with `igniteConfig.setUserAttributes(Map[String,Any]("ROLE" -> "WORKER").asJava)` will receive the message

>As long as your cluster is alive, Ignite will guarantee that the data between different cluster nodes will always remain consistent regardless of crashes or topology changes.

Ok until now, we have seen, nodes, topology, cluster groups, broadcast, lets look forward to one of the best feature of Ignite (durable memory, persistence, spark RDD, streaming, transactions, the next posts XD): ** SQL Queries ** !!, yes, SQLQueries! over your data!
Ignite is fully ANSI-99 compliant, supports Text queries and Predicate-based Scan Queries.

![](https://lh4.googleusercontent.com/lfSvocsIRt353n4HGSc2OtlKjueBkJMoiCfzMy6K_7dkBa5VUFX9wj_UtmkP1nj5qK_7wmAjaTlEpIHwaqKmH9EpJ6tLFB2ZfDRpQP0dGOgPzJFtrpyJcu1YtnF5fwzNJ8GvcwZu)

## Cache Queries
Before play with code, few considerations:

#### Add dependency
   
      sbt = "org.apache.ignite" % "ignite-indexing" % "2.4.0"

	  maven = <dependency>  
		        <groupId>org.apache.ignite</groupId>  
				<artifactId>ignite-indexing</artifactId>  
				<version>2.4.0</version>  
			  </dependency>
			  
#### Tell to Ignite, which entities are allowed to be used on queries, is easy, only adding annotations to classes:

	case class IotDevice(
					@(QuerySqlField@field)(index = true) name: String,
					@(QuerySqlField@field)(index = true) gpio: String,
					@(QueryTextField@field) sensorType: String,
					@(QueryTextField@field) model: String)
>Here we said, that all fields are available for use in queries, and add indexes over name and gpio attributes (like in any sql database)

#### After indexed and queryable fields are defined, they have to be registered in the SQL engine along with the object types they belong to.
	
	val igniteConfig = new IgniteConfiguration()
	val cacheConfig = new CacheConfiguration("ignite")
	cacheConfig.setIndexedTypes(Seq(classOf[String], classOf[IotDevice]): _*)
	igniteConfig.setCacheConfiguration(cacheConfig)
	val ignite = Ignition.start(igniteConfig)
	val cacheIot: IgniteCache[String, IotDevice] = ignite.getOrCreateCache[Int, String]("ignite")

Following the idea of IoT WebServer ,for example we develop the web server and put data to cacheIot defined above 

	val temp1 = IotDevice(name = "temp1", gpio = “123ASD", sensorType = "temperature", model = "test")
	cacheIot.put(temp1.gpio,temp1)
	val temp2 = IotDevice(name = "temp2", gpio = “456ASD", sensorType = "temperature", model = "test")
	cacheIot.put(temp2.gpio,temp2)

Now user call method: *GET/devicesBySensorType?sensor=temperature*

In our IotDevice case class *sensorType* is valid for queries thus, we can execute this query in three ways in Ignite :

>Simple sql :

	val sqlText = s"sensorType = 'temperature'"
	val sql = new SqlQuery[String, IotDevice](classOf[IotDevice], sqlText)
	val temperatureQueryResult = cacheIot.query(sql).getAll.asScala.map(_.getValue)
	println(s"SqlQuery = $temperatureQueryResult")

>ScanQuery :

	val cursor = cacheIot.query(new ScanQuery(new IgniteBiPredicate[String, IotDevice] {
	override def apply(key: String, entryValue: IotDevice) : Boolean = 	 entryValue.sensorType == "temperature"
	}))
	val temperatureScanResult = cursor.getAll.asScala
	println(s"ScanQuery = $temperatureScanResult")

>Text-based queries based on Lucene indexing, here find all IotDevices where sensorType == "temperature" (the annotation on model attribute **QueryTextField** allow this query, if you want, Ignite supports more than one QueryTextField)

	val textQuery = new TextQuery[IotDevice, String](classOf[IotDevice], "temperature")
	val temperatureTextResult = cacheIot.query(textQuery).getAll.asScala
	println(s"TextQuery = $temperatureTextResult") //all devices with sensorType = temperature

The result of queries:

![](https://lh4.googleusercontent.com/ELiPifexwDs5vbdkJDapHofE12qWWZzE25ujLH8T9r_GKLugvDAkGZ4TLJ4ho1dqHlLWwT74-_VfxCZwMUQ2QtrQfd7EpxlULfY8m9jtrfuloLKEBNl0-gt6AOeozoe_vC2qkYeC)


The full code for the example
	
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
		cacheConfig.setIndexedTypes(Seq(classOf[String], classOf\[IotDevice]): _*)
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
		}))
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

  ## Partition and Replication
  
Besides of having our cluster and execute queries, where really is the data??
Ignite provides three different modes of cache operation: PARTITIONED, REPLICATED, and LOCAL
-   **Partitioned** : this mode is the most scalable distributed cache mode. In this mode the overall data set is divided equally into partitions and all partitions are split equally between participating nodes, are ideal when working with large data sets and updates are frequent. In this mode also you can optionally configure any number of backup nodes for cached data.

		cacheCfg.setCacheMode(CacheMode.PARTITIONED)
		cacheCfg.setBackups(1);
 
     ![ ](https://lh4.googleusercontent.com/MDKOM80GfQosA6-OhAru4Cip5YwPenkGGH2vhLLYxuDYS3GB9tMqn1KfR7WtGAiSBMMKVgtF-aEgeEAKEE5OXgaGxt0jebjf5JDmhSjT_2AHm7gEysrAokwDr9Z-QTXpW6D3yqjF)

-   **Replicated** : this mode is expensive, all the data is replicated to every node in the cluster, every data updates must be propagated to all other nodes which can have an impact on performance and scalability, are ideal when working with small dataset and updates are infrequent.
    
		cacheCfg.setCacheMode(CacheMode.REPLICATED)


![](https://lh5.googleusercontent.com/QmMpm8SDxK7Rnr4wi4AwTGsrhMsx-x0d9JFMJOM4HE8obicau3q70HZahU4LGVmC74LQWltd8FAfMgGMIrViJJ7Q4RMr2tlndQWYlTHp7s8u641d2tMQJHSTFqpzGt93ZL1f0zQ6)

-   **Local**: this mode is the most light weight mode of cache operation, as no data is distributed to other cache nodes.
    
### In-Memory features

Since Ignite architecture is *memory friendly*, the RAM is always treated as the first memory tier, where all the processing happens. Benefits of this :

-   ***Off-heap Based*** : Data and indexes are stored outside Java heap, so only app code is the only source for Garbage collection stop the world pauses.
-   ***Predictable memory usage***: It is possible set up memory utilization.
-   ***Automatic memory defragmentation***: Apache Ignite uses the memory as efficiently as possible and executes defragmentation routines in the background avoiding fragmentation.
-   ***Improved performance and memory utilization***: All the data and indexes are stored in paged format with similar representation in memory and on disk, which removes any need for serializing or deserializing of data.

## Conclusion 

Ignite could be help your distributed architectures?  It would be costly integrate Ignite in your already productive apps?

There is lot more to cover and discuss, and a lot of code to try out!, we only scratched the surface of this great tool.

![](https://lh3.googleusercontent.com/owugqSPdHt3fixjuYuG-8ZUt8_8cehjm9q4qtXgmPIv9PRzppV7_ONsvM15FRJSjIpHz2jISPJc9y-kyDfsDEeoydmMZRwaNXC54FQl77VFvp4EOGmIMtB5AXPi69V6GA8H7eq1G)

For more information, documentation, and screencasts, visit:
[https://ignite.apache.org/index.html](https://ignite.apache.org/index.html)

[https://github.com/apache/ignite](https://github.com/apache/ignite)

[@ApacheIgnite](https://twitter.com/ApacheIgnite)

[https://www.gridgain.com/technology/apache-ignite](https://www.gridgain.com/technology/apache-ignite)

https://github.com/gastonlucero/ignitestratio

