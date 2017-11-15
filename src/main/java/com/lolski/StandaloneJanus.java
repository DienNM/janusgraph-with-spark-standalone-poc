package com.lolski;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;

import java.util.HashMap;
import java.util.Map;

/*
 * The StandaloneJanus class creates a setup which enables OLAP query on Apache Spark to be performed on our graph.
 *
 * A simple JanusGraph instance will be initialized for creating and storing a simple graph in Cassandra.
 * Plain ol' JanusGraph doesn't support running OLAP queries on Spark and this is where HadoopGraph comes into play.
 *
 * We have configured our application in such a way where a HadoopGraph instance will be created which pipe data directly out of Cassandra,
 * which already has the data coming from the JanusGraph instance.
 * HadoopGraph supports computation with SparkGraphComputer, which utilizes Apache Spark running in standalone mode.
 *
 * Needs:
 * 1. A running Cassandra instance at localhost (e.g. download grakn and run 'grakn server start storage')
 * 2. A running Spark Standalone 1.6.3 (must be exactly version 1.6.3!!!!!!!!!) (1 master and slave) at spark://127.0.0.1:5678
 *    e.g., download Spark 1.6.3, and run './sbin/start-master.sh -h 127.0.0.1 -p 5678 && ./sbin/start-slave.sh spark://127.0.0.1:5678'
 */
public class StandaloneJanus {
    /*
     * Cassandra and Spark configurations
     */
    public static final String cassandraAddress = "localhost";
    public static final String keyspace = "janusgraph";
    public static final String sparkAddress = AppConstants.SPARK_MASTER_VALUE_STANDALONE;
    // make sure to set the path as an absolute path. Spark standalone is most likely started from a different path from this program, which in that case relative path will be a problem
    public static final String sparkOutputLocation = "/Users/lolski/Playground/janusgraph/g-out/" + System.currentTimeMillis();

    /*
     * Initialize a simple JanusGraph instance and persist it in Cassandra.
     * Create HadoopGraph, which supports OLAP execution with Apache Spark
     * Create SparkGraphComputer for actually performing the OLAP execution
     * Return both the HadoopGraph and SparkGraphComputer
     */
    public static Pair<Graph, GraphComputer> newStandaloneJanusSparkComputer(boolean initialize) {
        Map<String, Object> janusConfig = newStandaloneJanusConfigurations();

        if (initialize) {
            newJanusGraph_initialiseSimpleGraphAndCommit(janusConfig);
        }

        HadoopGraph hadoopGraph = newHadoopGraph(janusConfig);

        GraphComputer computer = newStandaloneJanusSparkComputer(hadoopGraph);
        return Pair.of(hadoopGraph, computer);
    }

    /*
     * Create a configuration which supports Janus setup with Cassandra and Apache Spark
     * These configurations are quite lengthy and mostly undocumented
     */
    public static Map<String, Object> newStandaloneJanusConfigurations() {
        Map<String, Object> map = new HashMap<>();

        map.put("cassandra.input.keyspace", keyspace);
        map.put("janusgraphmr.ioformat.conf.storage.hostname", cassandraAddress);
        map.put("janusgraphmr.ioformat.conf.storage.cassandra.keyspace", keyspace);
        map.put("cassandra.input.partitioner.class", "org.apache.cassandra.dht.Murmur3Partitioner");
        map.put("gremlin.hadoop.outputLocation", sparkOutputLocation);
        map.put("janusmr.ioformat.conf.storage.backend", "cassandra");
        map.put("janusmr.ioformat.cf-name", "edgestore");
        map.put("cassandra.input.columnfamily", "edgestore");
        map.put("janusmr.ioformat.conf.storage.cassandra.keyspace", keyspace);
        map.put("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        map.put("gremlin.graph", "org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph");
        map.put("gremlin.hadoop.graphWriter", "org.apache.tinkerpop.gremlin.hadoop.structure.io.gryo.GryoOutputFormat");
        map.put("storage.hostname", cassandraAddress);
        map.put("janusgraphmr.ioformat.conf.storage.backend", "cassandra");
        map.put("janusmr.ioformat.conf.storage.hostname", cassandraAddress);
        map.put("gremlin.hadoop.graphReader", "org.janusgraph.hadoop.formats.cassandra.CassandraInputFormat");
        map.put("gremlin.hadoop.inputLocation", "none");
        map.put("storage.backend", "cassandra");

        return map;
    }

    /*
     * Create a SparkGraphComputer and configure it.
     */
    public static GraphComputer newStandaloneJanusSparkComputer(Graph graph) {
        SparkGraphComputer computer = graph.compute(SparkGraphComputer.class);

        computer.configure("spark.master", sparkAddress);

        computer.configure("cassandra.input.keyspace", keyspace);
        computer.configure("janusgraphmr.ioformat.conf.storage.hostname", cassandraAddress);
        computer.configure("janusgraphmr.ioformat.conf.storage.cassandra.keyspace", keyspace);
        computer.configure("cassandra.input.partitioner.class", "org.apache.cassandra.dht.Murmur3Partitioner");
        computer.configure("gremlin.hadoop.outputLocation", sparkOutputLocation);
        computer.configure("janusmr.ioformat.conf.storage.backend", "cassandra");
        computer.configure("janusmr.ioformat.cf-name", "edgestore");
        computer.configure("cassandra.input.columnfamily", "edgestore");
        computer.configure("janusmr.ioformat.conf.storage.cassandra.keyspace", keyspace);
        computer.configure("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        computer.configure("gremlin.graph", "org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph");
        computer.configure("gremlin.hadoop.graphWriter", "org.apache.tinkerpop.gremlin.hadoop.structure.io.gryo.GryoOutputFormat");
        computer.configure("storage.hostname", cassandraAddress);
        computer.configure("janusgraphmr.ioformat.conf.storage.backend", "cassandra");
        computer.configure("janusmr.ioformat.conf.storage.hostname", cassandraAddress);
        computer.configure("gremlin.hadoop.graphReader", "org.janusgraph.hadoop.formats.cassandra.CassandraInputFormat");
        computer.configure("gremlin.hadoop.inputLocation", "none");
        computer.configure("storage.backend", "cassandra");

        return computer;
    }

    /*
     * Initialise a simple graph and persist it in Cassandra
     */
    public static Graph newJanusGraph_initialiseSimpleGraphAndCommit(Map<String, Object> configuration) {
        Configuration config = new MapConfiguration(configuration);
        JanusGraph graph = JanusGraphFactory.open(config);

        JanusGraphTransaction tx = graph.newTransaction();
        Vertex wlz = tx.addVertex(T.label, "person", "name", "wong liang zan");
        Vertex ak = tx.addVertex(T.label, "person", "name", "angkur");
        Vertex ngy = tx.addVertex(T.label, "person", "name", "naq gynes");
        Vertex crl = tx.addVertex(T.label, "person", "name", "curl");
        wlz.addEdge("boss_of", ak);
        wlz.addEdge("boss_of", ngy);
        wlz.addEdge("boss_of", ngy);
        wlz.addEdge("boss_of", crl);
        tx.commit();

        return graph;
    }

    /*
     * Open Hadoop Graph
     */
    public static HadoopGraph newHadoopGraph(Map<String, Object> configuration) {
        Graph hadoopGraph = GraphFactory.open(configuration);
        return (HadoopGraph) hadoopGraph;
    }
}
