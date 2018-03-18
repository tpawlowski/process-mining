package org.processmining.plugins.kafka;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

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

public class XLogs {
	private static final XFactory factory = new XFactoryNaiveImpl();
	private static final XExtensionManager extensionManager = XExtensionManager.instance();
	private static final Calendar cal = Calendar.getInstance();
	
	static XLog parse(String name, Iterable<String> encodedEntries) {
		Map<String, XTrace> traces = new HashMap<String, XTrace>();
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
		XLog log = emptyLog(name);
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
