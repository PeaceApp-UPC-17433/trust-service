package pe.upc.peaceapp.trust.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String INCIDENT_CREATED_QUEUE = "trust.incident-created";

    @Bean
    TopicExchange eventsExchange(@Value("${trust.messaging.exchange}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange(@Value("${trust.messaging.exchange}") String name) {
        return new DirectExchange(name + ".dlx", true, false);
    }

    @Bean
    Declarables incidentCreatedTopology(TopicExchange eventsExchange, DirectExchange deadLetterExchange,
                                        @Value("${trust.messaging.exchange}") String exchange) {
        var queue = QueueBuilder.durable(INCIDENT_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", exchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", INCIDENT_CREATED_QUEUE + ".dlq").build();
        var dlq = QueueBuilder.durable(INCIDENT_CREATED_QUEUE + ".dlq").build();
        return new Declarables(queue, dlq, BindingBuilder.bind(queue).to(eventsExchange).with("incident.created"),
                BindingBuilder.bind(dlq).to(deadLetterExchange).with(INCIDENT_CREATED_QUEUE + ".dlq"));
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

