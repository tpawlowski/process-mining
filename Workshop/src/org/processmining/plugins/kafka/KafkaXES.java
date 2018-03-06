package org.processmining.plugins.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Kafka XES",
	parameterLabels = { "kafka topic" },
	returnLabels = { "Enumeration of XLog created from the topic and windowed by day" },
	returnTypes = { XLogEnumeration.class })
public class KafkaXES {
	
	@UITopiaVariant(affiliation = "University of Warsaw",
            author = "Tomasz Pawlowski",
            email = "tomasz.pawlowski@mimuw.edu.pl",
            uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(requiredParameterLabels = { 0 })
	public static XLogEnumeration itearator(final PluginContext context, final String topic) {
		return new XLogEnumeration(topic, kafkaConfig(), 10000L);
	}
	
	private static Map<String, Object> kafkaConfig() {
		Map<String, Object> consumerConfig = new HashMap<>();
		consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "test_group");
		consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return consumerConfig;
	}
}
