package org.processmining.plugins.kafka;

import java.util.Calendar;

import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptors;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.joda.time.Duration;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.exporting.PnmlExportNet;

public class BeamAlpha {
	private static final PnmlExportNet exportNet = new PnmlExportNet();
	private static final PluginContext context = new CLIPluginContext(new CLIContext(), "context");
	
	public static void main(String[] args) throws Exception {
        System.out.println("Starting beam");
		PipelineOptions options = PipelineOptionsFactory.create();
		options.setRunner(DirectRunner.class);
		Pipeline pipeline = Pipeline.create(options);
		
		PCollection<KV<String, String>> windowedData = pipeline.apply(KafkaIO.<String, String>read()
			       .withBootstrapServers("localhost:9092")
			       .withTopic("logs-input")
			       .withKeyDeserializer(StringDeserializer.class)
			       .withValueDeserializer(StringDeserializer.class))
				.apply(Window.<KafkaRecord<String, String>>into(FixedWindows.of(Duration.standardSeconds(10))))
				.apply(MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
						.via((record) -> record.getKV()));
		
		PCollection<KV<String, Iterable<String>>> grouppedData = windowedData.apply(GroupByKey.<String, String>create());
		
		PCollection<KV<String, String>> petriXml = grouppedData
				.apply(MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), TypeDescriptors.strings()))
					.via((kv) -> {
						System.out.println(String.format("[%s]: %s analysing entries", Calendar.getInstance().getTime().toString(), kv.getKey()));
		        			XLog log = XLogs.parse(kv.getKey(), kv.getValue());
		        			AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner = AlphaMinerFactory
		        					.createAlphaMiner(log, log.getClassifiers().get(0), new AlphaMinerParameters(AlphaVersion.CLASSIC));
		        			Pair<Petrinet, Marking> net_and_marking = miner.run();
		        			String net_xml = exportNet.exportPetriNetToPNMLOrEPNMLString(context, net_and_marking.getFirst(), Pnml.PnmlType.PNML, true);
						return KV.of(kv.getKey(), net_xml);
					}));
		
		petriXml.apply(KafkaIO.<String, String>write()
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
