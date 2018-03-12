package org.processmining.plugins.kafka;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.XExtensionManager;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XEventImpl;
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

/**
 * In this example, we implement a Kafka Stream pipeline which windows logs by processtime (NOT event time), converts 
 * windows of data into logs and runs alpha miner on logs retuning petri net of recent logs. 
 */
public class KafkaStreamAlphaMiner {
	private static final XFactory factory = new XFactoryNaiveImpl();
	private static final XExtensionManager extensionManager = XExtensionManager.instance();
	private static final Calendar cal = Calendar.getInstance();
	private static final PnmlExportNet exportNet = new PnmlExportNet();
	private static final PluginContext context = new CLIPluginContext(new CLIContext(), "context");
	
    public static void main(String[] args) throws Exception {
        System.out.println("Starting pipe");
        final StreamsBuilder builder = new StreamsBuilder();

        builder.<String, String>stream("logs-input")
        		.groupByKey()
        		.windowedBy(TimeWindows.of(TimeUnit.SECONDS.toMillis(10)).advanceBy(TimeUnit.SECONDS.toMillis(3)))
        		.reduce((one, two) -> {
        			return one + '|' + two;
        		})
        		.toStream((window, entries) -> {
        			return String.format("%s[%d,%d)", window.key(), window.window().start(), window.window().end());
        		})
        		.map((key, combinedEntries) -> {
        			System.out.println(String.format("[%s]: %s analysing %d entries", Calendar.getInstance().getTime().toString(), key, combinedEntries.split("\\|").length));
        			XLog log = parseXLog(key, combinedEntries);
        			AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> miner = AlphaMinerFactory
        					.createAlphaMiner(log, log.getClassifiers().get(0), new AlphaMinerParameters(AlphaVersion.CLASSIC));
        			Pair<Petrinet, Marking> net_and_marking = miner.run();
        			String net_xml = exportNet.exportPetriNetToPNMLOrEPNMLString(context, net_and_marking.getFirst(), Pnml.PnmlType.PNML, true);
        			return new KeyValue<String, String>(key, net_xml);
        		})
        		.to("logs-petri", Produced.with(Serdes.String(), Serdes.String()));

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, kafkaProperties());
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
    
    private static Properties kafkaProperties() {
    		Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 0L);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE);
        return props;
    }
    
    private static XLog parseXLog(String key, String combinedEntries) {
    		Map<String, XTrace> traces = new HashMap<String, XTrace>();
		String[] encodedEntries = combinedEntries.split("\\|");
		for (String encodedEntry: encodedEntries) {
			String[] split = encodedEntry.split(",", 3);
	        String caseId = split[0];
	        long timestamp = Long.parseLong(split[1]);
			String eventId = split[2];
			
			XTrace trace = traces.get(caseId);
			if (trace == null) {
				trace = factory.createTrace();
				trace.getAttributes().put("concept:name", factory.createAttributeLiteral("concept:name", caseId, extensionManager.getByPrefix("concept")));
				traces.put(caseId, trace);
			}
			
			cal.setTimeInMillis(timestamp);
			
			XAttributeMapImpl map = new XAttributeMapImpl();
			map.put("concept:name", new XAttributeLiteralImpl("concept:name", eventId));
			map.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition", "complete"));
			map.put("time:timestamp", new XAttributeTimestampImpl("time:timestamp", cal.getTime()));
			XEvent event = new XEventImpl(map);
			
			trace.add(event);
		}
		XLog log = emptyLog(key);
		log.addAll(traces.values());
		return log;
    }
    
    private static XLog emptyLog(String name) {
    		XFactory factory = new XFactoryNaiveImpl();
    		XExtensionManager extensionManager = XExtensionManager.instance();
		XLog log = factory.createLog();
		log.getAttributes().put("concept:name", factory.createAttributeLiteral("concept:name", name, extensionManager.getByPrefix("concept")));
		log.getAttributes().put("lifecycle:model", factory.createAttributeLiteral("lifecycle:model", "standard", extensionManager.getByPrefix("lifecycle")));
		log.getExtensions().add(extensionManager.getByPrefix("time"));
		log.getExtensions().add(extensionManager.getByPrefix("lifecycle"));
		log.getExtensions().add(extensionManager.getByPrefix("concept"));
		log.getClassifiers().add(new XEventNameClassifier());
		return log;
	}
}
