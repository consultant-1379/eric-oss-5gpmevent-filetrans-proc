/*******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.adc.config;

import static com.ericsson.oss.adc.config.kafka.AdminConfiguration.ADMIN_CONFIG;
import static com.ericsson.oss.adc.config.kafka.ConsumerConfiguration.CONSUMER_CONFIG;
import static com.ericsson.oss.adc.config.kafka.ProducerConfiguration.PRODUCER_CONFIG;
import static com.ericsson.oss.adc.config.kafka.SubscriptionConsumerConfiguration.SUBSCRIPTION_CONSUMER_CONFIG;

import java.util.Map;
import java.util.UUID;

import com.ericsson.oss.adc.config.kafka.BootStrapServerConfigurationSupplier;
import com.ericsson.oss.adc.models.InputMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.util.backoff.ExponentialBackOff;

import com.ericsson.pm_event.PmEventOuterClass;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@EnableKafka
public class KafkaConfiguration implements ApplicationContextAware {

    private static final String TX_PREFIX = UUID.randomUUID().toString();
    private static final int SUBSCRIPTION_CONSUMER_CONCURRENCY = 1;

    private static final long INITIAL_BACK_OFF_MS = 500L;

    @Autowired
    private Environment environment;

    @Autowired
    private BootStrapServerConfigurationSupplier bootStrapServerConfigurationSupplier;

    private ApplicationContext applicationContext = null;

    private MeterRegistry meterRegistry;

    public KafkaConfiguration(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> adminConfig = applicationContext.getBean(ADMIN_CONFIG, Map.class);
        KafkaAdmin kafkaAdmin = new KafkaAdmin(adminConfig);
        kafkaAdmin.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return kafkaAdmin;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> subscriptionConsumerKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(subscriptionConsumerInputFactory());
        factory.setConcurrency(SUBSCRIPTION_CONSUMER_CONCURRENCY);

        //Retries at 500ms then multiplies backoff by 1.5 and caps at 30s
        AfterRollbackProcessor<String, String> rollbackProcessor = new DefaultAfterRollbackProcessor<>(new ExponentialBackOff(INITIAL_BACK_OFF_MS, ExponentialBackOff.DEFAULT_MULTIPLIER));
        factory.setAfterRollbackProcessor(rollbackProcessor);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InputMessage> consumerKafkaListenerContainerFactory(@Autowired final RecordFilterStrategy<String,
            InputMessage> recordFilterStrategy) {
        ConcurrentKafkaListenerContainerFactory<String, InputMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerInputFactory());

        factory.getContainerProperties().setEosMode(ContainerProperties.EOSMode.V2);
        factory.getContainerProperties().setTransactionManager(transactionManager()); //set the consumer factory to use same trans manager as the producer factory
        factory.setBatchListener(true);

        factory.setRecordFilterStrategy(recordFilterStrategy);

        //Retries at 500ms then multiplies backoff by 1.5 and caps at 30s
        AfterRollbackProcessor<String, InputMessage> rollbackProcessor = new DefaultAfterRollbackProcessor<>(new ExponentialBackOff(INITIAL_BACK_OFF_MS, ExponentialBackOff.DEFAULT_MULTIPLIER));
        factory.setAfterRollbackProcessor(rollbackProcessor);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> subscriptionConsumerInputFactory() {
        Map<String, Object> subscriptionConsumerConfig = applicationContext.getBean(SUBSCRIPTION_CONSUMER_CONFIG, Map.class);

        DefaultKafkaConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(subscriptionConsumerConfig);
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        factory.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, InputMessage> consumerInputFactory() {
        Map<String, Object> consumerConfig = applicationContext.getBean(CONSUMER_CONFIG, Map.class);

        DefaultKafkaConsumerFactory<String, InputMessage> factory = new DefaultKafkaConsumerFactory<>(consumerConfig);
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        factory.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return factory;
    }

    @Bean
    public KafkaTemplate<String, PmEventOuterClass.PmEvent> kafkaOutputTemplate() {
        return new KafkaTemplate<>(producerOutputFactory(), false);
    }

    @Bean
    public ProducerFactory<String, PmEventOuterClass.PmEvent> producerOutputFactory() {

        Map<String, Object> producerConfig = applicationContext.getBean(PRODUCER_CONFIG, Map.class);
        DefaultKafkaProducerFactory<String, PmEventOuterClass.PmEvent> factory = new DefaultKafkaProducerFactory<>(producerConfig);
        factory.addListener(new MicrometerProducerListener<>(meterRegistry));
        factory.setTransactionIdPrefix(TX_PREFIX); //still have to give producer factory an id
        factory.setBootstrapServersSupplier(bootStrapServerConfigurationSupplier);

        return factory;
    }

    @Bean
    public KafkaAwareTransactionManager<String, PmEventOuterClass.PmEvent> transactionManager() {
        KafkaTransactionManager<String, PmEventOuterClass.PmEvent> transactionManager = new KafkaTransactionManager<>(producerOutputFactory()); // provide transaction manager with producer factory
        transactionManager.setTransactionIdPrefix(TX_PREFIX); // "override the transaction id prefix"
        return transactionManager;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
