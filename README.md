k8hadoop0202
============

This is a tool for transform data from kafka2.9.2-0.8.1 to hadoop-0.20.2 , 
it's based on k8hadoop( https://github.com/javayoyobj/k8hadoop ) , I just modified some code to suit new kafka api and old hadoop api. 


how to run?
1.import this maven project into eclipse
2.update maven dependencies.
3.run HadoopConsumer with params.  like: -zk-connect <zookeeper> -topic <topic> target_hdfs_path
