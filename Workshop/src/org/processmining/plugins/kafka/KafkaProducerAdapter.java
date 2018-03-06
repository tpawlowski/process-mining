package org.processmining.plugins.kafka;

import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.pnml.base.Pnml;
import org.processmining.plugins.pnml.exporting.PnmlExportNet;

public class KafkaProducerAdapter {
	private Producer<String, String> petrinetProducer;
	private PnmlExportNet exportNet;
	
	private Producer<String, byte[]> imageProducer;
	
	private Properties defaultProperties;
	private PluginContext context;
	
	
	KafkaProducerAdapter(final PluginContext context, final Properties properties) {
		this.defaultProperties = properties;
		this.context = context;
	}
	
	public boolean publish(final String topic, final String key, final Petrinet net) {
		System.out.println("publishing petrinet " + key + " to topic " + topic);
		if (petrinetProducer == null) {
			Properties props = new Properties();
			props.putAll(defaultProperties);
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
			petrinetProducer = new KafkaProducer<>(props);
			exportNet = new PnmlExportNet();
		}
		
		try {
			String netString = exportNet.exportPetriNetToPNMLOrEPNMLString(context, net, Pnml.PnmlType.PNML, true);
			System.out.println("serialized petrinet to xml");
            final ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, netString);
            petrinetProducer.send(record).get();
            return true;
	    } catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean publish(final String topic, final String key, final RenderedImage image) {
		System.out.println("publishing image " + key + " to topic " + topic);
		if (imageProducer == null) {
			Properties props = new Properties();
			props.putAll(defaultProperties);
			props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
			imageProducer = new KafkaProducer<>(props);
		}
		
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ImageIO.write(image, "png", outputStream);
			byte[] data = outputStream.toByteArray();
			System.out.println("serialized image to byte array of size " + data.length);
            final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, data);
            imageProducer.send(record).get();
            return true;
	    } catch (InterruptedException | ExecutionException | IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void flush() {
		if (petrinetProducer != null) {
			petrinetProducer.flush();
		}
		if (imageProducer != null) {
			imageProducer.flush();
		}
	}
	
	public void close() {
		if (petrinetProducer != null) {
			petrinetProducer.close();
		}
		if (imageProducer != null) {
			imageProducer.close();
		}
	}
	
}
