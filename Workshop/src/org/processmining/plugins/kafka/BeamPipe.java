package org.processmining.plugins.kafka;

import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class BeamPipe {
	public static void main(String[] args) throws Exception {
        System.out.println("Starting beam");
		PipelineOptions options = PipelineOptionsFactory.create();
		options.setRunner(DirectRunner.class);
		Pipeline pipeline = Pipeline.create(options);
		
		pipeline.apply(KafkaIO.<String, String>read()
			       .withBootstrapServers("localhost:9092")
			       .withTopic("logs-input")
			       .withKeyDeserializer(StringDeserializer.class)
			       .withValueDeserializer(StringDeserializer.class))
				.apply(MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
						.via((record) -> record.getKV()))
				.apply(KafkaIO.<String, String>write()
				.withBootstrapServers("localhost:9092")
				.withTopic("logs-petri")
				.withKeySerializer(StringSerializer.class)
				.withValueSerializer(StringSerializer.class));
		
		PipelineResult result = pipeline.run();
	    try {
	    		result.waitUntilFinish();
	    } catch (Exception exc) {
	    		result.cancel();
	    }
	}
}
