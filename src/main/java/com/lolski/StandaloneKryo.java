package com.lolski;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.example.GraphOfTheGodsFactory;

import java.util.HashMap;
import java.util.Map;

public class StandaloneKryo {

    public static Pair<Graph, GraphComputer> newStandaloneKryoSparkComputer() {
        Map<String, Object> config = newStandaloneKryoConfigurations();
        Graph graph = GraphFactory.open(config);
        GraphComputer computer = newStandaloneSparkComputer(graph);
        return Pair.of(graph, computer);
    }

    public static Map<String, Object> newStandaloneKryoConfigurations() {
        Map<String, Object> map = new HashMap<>();
        map.put(AppConstants.GREMLIN_GRAPH, AppConstants.GREMLIN_GRAPH_VALUE_HADOOP);
        map.put(Constants.GREMLIN_HADOOP_INPUT_LOCATION, AppConstants.GREMLIN_HADOOP_INPUT_LOCATION_VALUE);
        map.put(Constants.GREMLIN_HADOOP_GRAPH_READER, AppConstants.GREMLIN_HADOOP_GRAPH_READER_VALUE);
        map.put(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, AppConstants.GREMLIN_HADOOP_OUTPUT_LOCATION_VALUE);
        map.put(Constants.GREMLIN_HADOOP_GRAPH_WRITER, AppConstants.GREMLIN_HADOOP_GRAPH_WRITER_VALUE);

        return map;
    }

    public static GraphComputer newStandaloneSparkComputer(Graph graph) {
        SparkGraphComputer computer = graph.compute(SparkGraphComputer.class);
        computer.configure(AppConstants.SPARK_MASTER, AppConstants.SPARK_MASTER_VALUE_STANDALONE);
        computer.configure(AppConstants.SPARK_SERIALIZER, AppConstants.SPARK_SERIALIZER_VALUE);

        computer.configure(Constants.GREMLIN_HADOOP_INPUT_LOCATION, AppConstants.GREMLIN_HADOOP_INPUT_LOCATION_VALUE);
        computer.configure(Constants.GREMLIN_HADOOP_GRAPH_READER, AppConstants.GREMLIN_HADOOP_GRAPH_READER_VALUE);
        computer.configure(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, AppConstants.GREMLIN_HADOOP_OUTPUT_LOCATION_VALUE);
        computer.configure(Constants.GREMLIN_HADOOP_GRAPH_WRITER, AppConstants.GREMLIN_HADOOP_GRAPH_WRITER_VALUE);
//        computer.configure(Constants.GREMLIN_HADOOP_JARS_IN_DISTRIBUTED_CACHE, configurations.get(Constants.GREMLIN_HADOOP_JARS_IN_DISTRIBUTED_CACHE));
//        computer.configure(Constants.GREMLIN_HADOOP_DEFAULT_GRAPH_COMPUTER, configurations.get(Constants.GREMLIN_HADOOP_DEFAULT_GRAPH_COMPUTER));

        return computer;
    }

//    public static JanusGraph loadGraphOfTheGodsGraph() {
//        JanusGraph graph = JanusGraphFactory.open(CONFIG_PROPERTIES_PATH);
//        GraphOfTheGodsFactory.loadWithoutMixedIndex(graph, false);
//        return graph;
//    }
//
//    public static HadoopGraph loadFromJanus() {
//        Map<String, Object> config = newStandaloneKryoConfigurations();
//        String KEYSPACE = loadConfigProperties(CONFIG_PROPERTIES_PATH).getProperty(AppConstants.STORAGE_CASSANDRA_KEYSPACE);
//
//        config.put(AppConstants.JANUSMR_IOFORMAT_CONF_STORAGE_CASSANDRA_KEYSPACE, KEYSPACE);
//        Graph hadoopGraph = GraphFactory.open(config);
//        return (HadoopGraph) hadoopGraph;
//    }

}
