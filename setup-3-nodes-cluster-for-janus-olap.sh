#!/bin/bash

# ====================================================================================
# configurations
# ====================================================================================
grakn_tar_fullpath=/Users/lolski/grakn.ai/grakn/grakn-dist/target/grakn-dist-1.0.0-SNAPSHOT.tar.gz
spark_tar_fullpath=/Users/lolski/Downloads/spark-1.6.3-bin-hadoop2.6.tgz
janus_poc_jar=/Users/lolski/Playground/janusgraph/target/janusgraph-1.0-SNAPSHOT.jar
janus_poc_lib=/Users/lolski/Playground/janusgraph/target/lib
hadoop_dir=/Users/lolski/Downloads/hadoop-2.6.5
await_cluster_ready_second=5
node1="cassandra-node1"
node2="cassandra-node2"
node3="cassandra-node3"

# ====================================================================================
# Docker helpers
# ====================================================================================
docker_inspect_get_ip() {
  docker inspect --format '{{ .NetworkSettings.IPAddress }}' $1
}

docker_run() {
  local NODE=$1
  local image=$2
  local additional_arguments=$3
  echo docker run --rm --detach --name $NODE -h $NODE $additional_arguments grakn/oracle-java-8
  docker run --rm --detach --name $NODE -h $NODE $additional_arguments grakn/oracle-java-8
  return $?
}

# ====================================================================================
# Grakn helpers
# ====================================================================================
setup_ssh() {
  local NODE=$1

  echo docker exec $NODE /bin/bash -c "ssh-keygen -f /root/.ssh/id_rsa -t rsa -N ''"
  docker exec $NODE /bin/bash -c "ssh-keygen -f /root/.ssh/id_rsa -t rsa -N ''"
  echo docker exec $NODE /bin/bash -c "cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys"
  docker exec $NODE /bin/bash -c "cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys"
  echo docker exec $NODE /bin/bash -c "dpkg-reconfigure openssh-server"
  docker exec $NODE /bin/bash -c "dpkg-reconfigure openssh-server"
  echo docker exec $NODE /bin/bash -c "service ssh start"
  docker exec $NODE /bin/bash -c "service ssh start"
}

grakn_cluster_join_and_restart() {
  local NODE=$1
  local cluster=$2
  local local_ip=$3
  local spark_node_type=$4

  echo docker exec $NODE /bin/bash -c "cd grakn-dist-1.0.0-SNAPSHOT && ./grakn cluster configure $cluster $local_ip"
  docker exec $NODE /bin/bash -c "cd grakn-dist-1.0.0-SNAPSHOT && ./grakn cluster configure $cluster $local_ip"

  echo docker exec $NODE /bin/bash -c "cd grakn-dist-1.0.0-SNAPSHOT && ./grakn server start storage"
  docker exec $NODE /bin/bash -c "cd grakn-dist-1.0.0-SNAPSHOT && ./grakn server start storage"

  local start_spark_cmd=
  if [ "$spark_node_type" == 'spark-master-node' ]; then
    start_spark_cmd="./sbin/start-master.sh -h $local_ip -p 5678"
  else
    start_spark_cmd="./sbin/start-slave.sh spark://$cluster:5678"
  fi
  echo docker exec $NODE /bin/bash -c "cd spark-1.6.3-bin-hadoop2.6 && $start_spark_cmd"
  docker exec $NODE /bin/bash -c "cd spark-1.6.3-bin-hadoop2.6 && $start_spark_cmd"

  echo docker exec $NODE /bin/bash -c "mkdir /hadoop-tmp"
  docker exec $NODE /bin/bash -c "mkdir /hadoop-tmp"
  local hadoop_conf_dir=
  if [ "$spark_node_type" == 'spark-master-node' ]; then
    hadoop_conf_dir='hadoop-conf/master'
  else
    hadoop_conf_dir='hadoop-conf/master'
  fi
  echo docker cp $hadoop_conf_dir $NODE:/tmp/hadoop-conf
  docker cp $hadoop_conf_dir $NODE:/tmp/hadoop-conf
  echo docker exec $NODE /bin/bash -c "mv /tmp/hadoop-conf/* /hadoop/etc/hadoop/"
  docker exec $NODE /bin/bash -c "mv /tmp/hadoop-conf/* /hadoop/etc/hadoop/"

  echo docker exec $NODE /bin/bash -c "cd /hadoop && ./bin/hadoop namenode -format"
  docker exec $NODE /bin/bash -c "cd /hadoop && ./bin/hadoop namenode -format"
}

grakn_cluster_status() {
  local NODE=$1
  local local_ip=`docker_inspect_get_ip $NODE`

  local result=`docker exec $NODE /bin/bash -c "cd grakn-dist-1.0.0-SNAPSHOT && ./services/cassandra/nodetool status | grep $local_ip"`

  local node_status=`echo $result | awk '{print $1}'`
  echo $node_status
}

# ====================================================================================
# Install distribution helpers
# ====================================================================================
copy_distribution_into_docker_container() {
  local NODE=$1
  local grakn_tar_fullpath=$2
  local spark_tar_fullpath=$3

  local grakn_dir="/grakn-dist-1.0.0-SNAPSHOT"

  echo docker cp $grakn_tar_fullpath $NODE:/
  docker cp $grakn_tar_fullpath $NODE:/
  echo docker exec $NODE tar -xf /`basename $grakn_tar_fullpath`
  docker exec $NODE tar -xf /`basename $grakn_tar_fullpath`

  echo docker cp $spark_tar_fullpath $NODE:/
  docker cp $spark_tar_fullpath $NODE:/
  echo docker exec $NODE tar -xf /`basename $spark_tar_fullpath`
  docker exec $NODE tar -xf /`basename $spark_tar_fullpath`

  echo docker cp $janus_poc_jar $NODE:/
  docker cp $janus_poc_jar $NODE:/

  echo docker cp $janus_poc_lib $NODE:/janusgraph-lib
  docker cp $janus_poc_lib $NODE:/janusgraph-lib

  echo docker cp $hadoop_dir $NODE:/hadoop
  docker cp $hadoop_dir $NODE:/hadoop
}

spawn_container_and_install_distribution() {
  local NODE="$1"
  local grakn_tar_fullpath="$2"
  local spark_tar_fullpath="$3"
  local additional_arguments="$4"
  echo "spawning '$NODE'..."
  docker_run "$NODE" "grakn/oracle-java-8" "$additional_arguments"
  echo "copy distribution into '$NODE'"
  copy_distribution_into_docker_container "$NODE" "$grakn_tar_fullpath" "$spark_tar_fullpath"
}

# ====================================================================================
# Test init and cleanup
# ====================================================================================

test_init() {
  echo "--------------------------- test - init ---------------------------"
  spawn_container_and_install_distribution $node1 $grakn_tar_fullpath $spark_tar_fullpath "-p8080:8080 -p5005:5005"
  spawn_container_and_install_distribution $node2 $grakn_tar_fullpath $spark_tar_fullpath '-p8081:8080'
  spawn_container_and_install_distribution $node3 $grakn_tar_fullpath $spark_tar_fullpath '-p8082:8080'
  echo "--------------------------- end init ---------------------------"
  echo ""
  echo ""
}

test_cleanup() {
  echo "--------------------------- test - cleanup ---------------------------"
  docker kill $node1
  docker kill $node2
  docker kill $node3
  echo "--------------------------- end cleanup ---------------------------"
  echo ""
  echo ""
}

# ====================================================================================
# Main routine: test orchestration methods
# ====================================================================================
test_initiate_cluster_join() {
  echo "--------------------------- test - initiate cluster join ---------------------------"
  setup_ssh $node1
  local master_node_ip=`docker_inspect_get_ip $node1`
  grakn_cluster_join_and_restart $node1 $master_node_ip $master_node_ip 'spark-master-node'

  setup_ssh $node2
  local node2_ip=`docker_inspect_get_ip $node2`
  grakn_cluster_join_and_restart $node2 $master_node_ip $node2_ip 'spark-slave-node'

  setup_ssh $node3
  local node3_ip=`docker_inspect_get_ip $node3`
  grakn_cluster_join_and_restart $node3 $master_node_ip $node3_ip 'spark-slave-node'
  echo "--------------------------- end initiate cluster join ---------------------------"
  echo ""
  echo ""
}

test_insert_test_data_and_check_cluster_join() {
  local return_value=

  echo "--------------------------- test - check cluster join ---------------------------"
  local master_node_ip=`docker_inspect_get_ip $node1`
  local hadoop_gremlin_libs="/grakn-dist-1.0.0-SNAPSHOT/services/lib/"

  echo docker exec $node1 /bin/bash -c "java -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -cp '/janusgraph-1.0-SNAPSHOT.jar:/janusgraph-lib/*:/hadoop/etc/hadoop/core-site.xml:/hadoop/etc/hadoop/mapred-site.xml:/hadoop/etc/hadoop/hdfs-site.xml' -Dspark_output_location_root=/out -Dhadoop_gremlin_libs=$hadoop_gremlin_libs -Dspark.master=spark://$master_node_ip:5678 -Dstorage.hostname=$master_node_ip -Dpre_initialize_graph com.lolski.Main"
  docker exec $node1 /bin/bash -c "java -Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 -cp '/janusgraph-1.0-SNAPSHOT.jar:/janusgraph-lib/*:/hadoop/etc/hadoop/core-site.xml:/hadoop/etc/hadoop/mapred-site.xml:/hadoop/etc/hadoop/hdfs-site.xml' -Dspark_output_location_root=/out -Dhadoop_gremlin_libs=$hadoop_gremlin_libs -Dspark.master=spark://$master_node_ip:5678 -Dstorage.hostname=$master_node_ip -Dpre_initialize_graph com.lolski.Main"
  echo "--------------------------- end check cluster join ---------------------------"
  echo ""
  echo ""

  return $return_value
}

# ====================================================================================
# Main routine
# ====================================================================================
set -e

test_init
test_initiate_cluster_join
# sleep $await_cluster_ready_second

test_insert_test_data_and_check_cluster_join
