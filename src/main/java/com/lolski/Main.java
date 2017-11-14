package com.lolski;

import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.process.computer.ComputerResult;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.clustering.peerpressure.ClusterPopulationMapReduce;
import org.apache.tinkerpop.gremlin.process.computer.clustering.peerpressure.PeerPressureVertexProgram;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.example.GraphOfTheGodsFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Hello world with JanusGraph & Spark Standalone!
 * This PoC works with Spark in both in memory mode (e.g. local[1]) and standalone mode (e.g. spark://127.0.0.1:5678)
 * Needs cassandra to run
 *
 */
public class Main {
    private static final String CONFIG_PROPERTIES_PATH = "config.properties";
    private static final String SPARK_MASTER = "local[1]"; // spark://127.0.0.1:5678

    private static final Map<String, Object> configurations = newDefaultConfigurations();

    public static void main( String[] args ) throws InterruptedException, ExecutionException {
//        printClasspath();
        JanusGraph janusGraph = loadGraphOfTheGodsGraph();
        janusGraph.newTransaction().commit();
        Graph hadoopGraph = loadFromJanus();
        GraphComputer computer = getConfiguredSparkGraphComputer(hadoopGraph);

        p("--- PROGRAM STARTING --- ");
        computer.program(PeerPressureVertexProgram.build().create(hadoopGraph)).mapReduce(ClusterPopulationMapReduce.build().create());
        Future<ComputerResult> work = computer.submit();

        p(" result =  " + work.get().memory().get("clusterPopulation"));
        p("--- PROGRAM ENDED --- ");
    }

    public static JanusGraph loadGraphOfTheGodsGraph() {
        JanusGraph graph = JanusGraphFactory.open(CONFIG_PROPERTIES_PATH);
        GraphOfTheGodsFactory.loadWithoutMixedIndex(graph, false);
        return graph;
    }

    public static HadoopGraph loadFromJanus() {
        Map<String, Object> config = newDefaultConfigurations();
        String KEYSPACE = loadConfigProperties(CONFIG_PROPERTIES_PATH).getProperty(AppConstants.STORAGE_CASSANDRA_KEYSPACE);

        config.put(AppConstants.JANUSMR_IOFORMAT_CONF_STORAGE_CASSANDRA_KEYSPACE, KEYSPACE);
        Graph hadoopGraph = GraphFactory.open(config);
        return (HadoopGraph) hadoopGraph;
    }

    public static HadoopGraph loadTinkerpopKyro() {
        Map<String, Object> config = newDefaultConfigurations();
        config.put(Constants.GREMLIN_HADOOP_INPUT_LOCATION, "./g-in/tinkerpop-modern.kryo");
        Graph hadoopGraph = GraphFactory.open(config);
        return (HadoopGraph) hadoopGraph;
    }

    public static Map<String, Object> newDefaultConfigurations() {
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.SPARK_MASTER, Main.SPARK_MASTER);
//        map.put("spark.executor.memory", "1g");
//        map.put("spark.serializer", "org.apache.spark.serializer.KryoSerializer");

        map.put(Constants.GREMLIN_HADOOP_GRAPH_READER, "org.apache.tinkerpop.gremlin.hadoop.structure.io.gryo.GryoInputFormat");
        map.put(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, "./g-out");
        map.put(Constants.GREMLIN_HADOOP_GRAPH_WRITER, "org.apache.tinkerpop.gremlin.hadoop.structure.io.gryo.GryoOutputFormat");
        map.put(AppConstants.GREMLIN_GRAPH, "org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph");
        return map;
    }

    public static void p(String print) {
        System.out.println(print);
    }

    public static void printClasspath() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
            System.out.println(url.getFile());
        }
    }

    public static GraphComputer getConfiguredSparkGraphComputer(Graph graph) {
        SparkGraphComputer computer = graph.compute(SparkGraphComputer.class);
        computer.configure(AppConstants.SPARK_MASTER, configurations.get(AppConstants.SPARK_MASTER));
//        computer.configure("spark.executor.memory", configurations.get("spark.executor.memory"));
//        computer.configure("spark.serializer", configurations.get("spark.serializer"));

        computer.configure(Constants.GREMLIN_HADOOP_INPUT_LOCATION, configurations.get(Constants.GREMLIN_HADOOP_INPUT_LOCATION));
        computer.configure(Constants.GREMLIN_HADOOP_GRAPH_READER, configurations.get(Constants.GREMLIN_HADOOP_GRAPH_READER));
        computer.configure(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, configurations.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION));
        computer.configure(Constants.GREMLIN_HADOOP_GRAPH_WRITER, configurations.get(Constants.GREMLIN_HADOOP_GRAPH_WRITER));
//        computer.configure(Constants.GREMLIN_HADOOP_JARS_IN_DISTRIBUTED_CACHE, configurations.get(Constants.GREMLIN_HADOOP_JARS_IN_DISTRIBUTED_CACHE));
//        computer.configure(Constants.GREMLIN_HADOOP_DEFAULT_GRAPH_COMPUTER, configurations.get(Constants.GREMLIN_HADOOP_DEFAULT_GRAPH_COMPUTER));

        return computer;
    }

    public static Properties loadConfigProperties(String configPath) {
        try {
            FileInputStream inputStream = new FileInputStream(configPath);
            Properties config = new Properties();
            config.load(inputStream);
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class AppConstants {
    public static final String SPARK_MASTER = "spark.master";
    public static final String GREMLIN_GRAPH = "gremlin.graph";
    public static final String STORAGE_CASSANDRA_KEYSPACE = "storage.cassandra.keyspace";
    public static final String JANUSMR_IOFORMAT_CONF_STORAGE_CASSANDRA_KEYSPACE = "janusgraphmr.ioformat.conf.storage.cassandra.keyspace";
}