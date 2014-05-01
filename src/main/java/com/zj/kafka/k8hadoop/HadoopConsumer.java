package com.zj.kafka.k8hadoop;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HadoopConsumer extends Configured implements Tool {
	private static Logger LOG = LoggerFactory.getLogger(HadoopConsumer.class);
	
    public static class KafkaMapper extends Mapper<LongWritable, BytesWritable, LongWritable, Text> {
        @Override
        public void map(LongWritable key, BytesWritable value, Context context) throws IOException {
            Text out = new Text();
            try {
                out.set(value.getBytes(),0, value.getLength());
                context.write(key, out);
            } catch (InterruptedException e) {
                
            }
        }
        
    }
	
	@Override
	public int run(String[] args) throws Exception {
        CommandLineParser parser = new PosixParser();
        Options options = buildOptions();
        
        CommandLine cmd = parser.parse(options, args);
        
        Configuration conf = new Configuration();
        
        conf.set("kafka.topic", cmd.getOptionValue("topic", "test3"));
        
        conf.set("kafka.groupid", cmd.getOptionValue("consumer-group", "test_group"));
        
        conf.set("kafka.zk.connect", cmd.getOptionValue("zk-connect", "localhost:2181"));
        
        if (cmd.getOptionValue("autooffset-reset") != null)
            conf.set("kafka.autooffset.reset", cmd.getOptionValue("autooffset-reset"));
        conf.setInt("kafka.limit", Integer.valueOf(cmd.getOptionValue("limit", "-1")));
        
        conf.setBoolean("mapred.map.tasks.speculative.execution", false);
        
        Job job = new Job(conf, "Kafka.Consumer");
        job.setJarByClass(getClass());
        job.setMapperClass(KafkaMapper.class);
        // input
        job.setInputFormatClass(KafkaInputFormat.class);
        // output
        job.setOutputKeyClass(LongWritable.class);
        job.setOutputValueClass(Text.class);
        job.setOutputFormatClass(KafkaOutputFormat.class);
        
        job.setNumReduceTasks(0);
        
        KafkaOutputFormat.setOutputPath(job, new Path(cmd.getArgs()[0]));
        
        boolean success = job.waitForCompletion(true);
        if (success) {
            commit(conf);
        }
        return success ? 0: -1;
	}
	
    private void commit(Configuration conf) throws IOException {
        ZkUtils zk = new ZkUtils(conf);
        try {
            String topic = conf.get("kafka.topic");
            String group = conf.get("kafka.groupid");
            zk.commit(group, topic);
        } catch (Exception e) {

        } finally {
            zk.close();
        }
    }

	@SuppressWarnings({ "unused", "static-access" })
	private Options buildOptions() {
		Options options = new Options();

		OptionBuilder.withArgName("topic").withLongOpt("topic");
		options.addOption(OptionBuilder.hasArg().withDescription("kafka topic")
				.create("t"));
		options.addOption(OptionBuilder.withArgName("groupid")
				.withLongOpt("consumer-group").hasArg()
				.withDescription("kafka consumer groupid").create("g"));
		options.addOption(OptionBuilder.withArgName("zk")
				.withLongOpt("zk-connect").hasArg()
				.withDescription("ZooKeeper connection String").create("z"));

		options.addOption(OptionBuilder.withArgName("offset")
				.withLongOpt("autooffset-reset").hasArg()
				.withDescription("Offset reset").create("o"));

		options.addOption(OptionBuilder.withArgName("limit")
				.withLongOpt("limit").hasArg().withDescription("kafka limit")
				.create("l"));

		return options;
	}
	
    public static void main(String[] args) throws Exception {
    	args = new String[]{"hdfs://localhost:9000/test/"+System.currentTimeMillis()};
        int exitCode = ToolRunner.run(new HadoopConsumer(), args);
        System.exit(exitCode);
    }

}
