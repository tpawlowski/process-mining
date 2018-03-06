package org.processmining.plugins.kafka;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

public class XLogEnumeration implements Enumeration<XLog>{
	private Consumer<String, String> consumer;
	private long timeout_ms;
	private Queue<XLog> queue;
	
	private XFactory factory = new XFactoryNaiveImpl();
	private XExtensionManager extensionManager = XExtensionManager.instance();
	private SimpleDateFormat log_name_format = new SimpleDateFormat("yyyy-MM-dd");
	
	private XLog log = null;
	private Map<String, XTrace> traces;
	private Calendar cal = Calendar.getInstance();

	public XLogEnumeration(String topic, Map<String, Object> kafkaConfig, long timeout_ms) {
		this.consumer = new KafkaConsumer<>(kafkaConfig);
		this.consumer.subscribe(Arrays.asList(topic));
		this.timeout_ms = timeout_ms;
		this.queue = new LinkedList<XLog>();
		
		this.traces = new HashMap<String, XTrace>();
	}
	
	@Override
	public boolean hasMoreElements() {
		if (!queue.isEmpty()) {
			return true;
		}
		loadQueue();
		return !queue.isEmpty();
	}
	
	@Override 
	public XLog nextElement() {
		if (queue.isEmpty()) {
			throw new NoSuchElementException();
		}
		return queue.poll();
	}
	
	private void loadQueue() {
		while (queue.isEmpty()) {
			ConsumerRecords<String, String> records = consumer.poll(timeout_ms);
			if (records.isEmpty()) {
				if (!traces.isEmpty()) {
					System.out.println("Returning log for day " + log.getAttributes().get("concept:name").toString() + " timeout");
					log.addAll(traces.values());
					queue.add(log);
					
					traces.clear();
					log = null;
				}
				break;
			} else {
			    for (ConsumerRecord<String, String> record : records) {
			        long timestamp = record.timestamp();
			        String case_id = record.key();
			        String event_id = record.value();
			        
			        // Note: this is needed because simulated timestamp is passed as part of value.
			        String[] split = event_id.split(",", 2);
			        timestamp = Long.parseLong(split[0]);
			        event_id = split[1];
					cal.setTimeInMillis(timestamp);
					String name = log_name_format.format(cal.getTime());
					
					if (log == null) {
						log = emptyLog(name);
						System.out.println("Started collecting log for day " + name);
					} else if (!name.equals(log.getAttributes().get("concept:name").toString())) {
						System.out.println("Returning log for day " + log.getAttributes().get("concept:name").toString() + " next day started");
						log.addAll(traces.values());
						queue.add(log);

						traces.clear();
						log = emptyLog(name);
						System.out.println("Started collecting log for day " + name);
					}
					
					XTrace trace = traces.get(case_id);
					if (trace == null) {
						trace = factory.createTrace();
						trace.getAttributes().put("concept:name", factory.createAttributeLiteral("concept:name", case_id, extensionManager.getByPrefix("concept")));
						traces.put(case_id, trace);
					}
					
					XAttributeMapImpl map = new XAttributeMapImpl();
					map.put("concept:name", new XAttributeLiteralImpl("concept:name", event_id));
					map.put("lifecycle:transition", new XAttributeLiteralImpl("lifecycle:transition", "complete"));
					map.put("time:timestamp", new XAttributeTimestampImpl("time:timestamp", cal.getTime()));
					XEvent event = new XEventImpl(map);
					
					trace.add(event);
			    }
			}
		}
	}
	
	private XLog emptyLog(String name) {
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
