package org.processmining.plugins.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(name = "Kafka Producer",
parameterLabels = { "bootstrap servers", "client_id" },
returnLabels = { "Adaper handing publishing to kafka" },
returnTypes = { KafkaProducerAdapter.class })
public class KafkaProducerPlugin {
	
	@UITopiaVariant(affiliation = "University of Warsaw",
            author = "Tomasz Pawlowski",
            email = "tomasz.pawlowski@mimuw.edu.pl",
            uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(requiredParameterLabels = { 0, 1 })
	public static KafkaProducerAdapter adapter(final PluginContext context, final String servers, final String client_id) {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, client_id);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		
		return new KafkaProducerAdapter(context, props);
	}
	
	@UITopiaVariant(affiliation = "University of Warsaw",
            author = "Tomasz Pawlowski",
            email = "tomasz.pawlowski@mimuw.edu.pl",
            uiLabel = UITopiaVariant.USEPLUGIN)
	@PluginVariant(requiredParameterLabels = { 0 })
	public static KafkaProducerAdapter adapter(final PluginContext context, final String servers) {
		return adapter(context, servers, "producer0001");
	}
}
