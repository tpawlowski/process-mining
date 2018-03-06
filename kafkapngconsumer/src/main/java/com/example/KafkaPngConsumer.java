package com.example;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class KafkaPngConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "consumer-tutorial");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", ByteArrayDeserializer.class.getName());
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);

        if (args.length != 3) {
            System.out.println("Parameter assertion. Requires topic, path to output directory and timeout_ms");
            return;
        }

        System.out.println("Subscribing to topic " + args[0]);

        consumer.subscribe(Arrays.asList(args[0]));

        File dir = new File(args[1]);
        dir.mkdirs();
        long timeout_ms = Long.parseLong(args[2]);

        try {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(timeout_ms);
                if (records.isEmpty()) {
                    System.out.println("Timeout reached - closing");
                    break;
                } else {
                    for (ConsumerRecord<String, byte[]> record : records) {
                        File outputFile = new File(dir, record.key() + ".png");
                        try (FileOutputStream stream = new FileOutputStream(outputFile.getPath())) {
                            stream.write(record.value());
                            stream.close();
                        }

                        System.out.println("Wrote png image to file " + outputFile.getPath());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
