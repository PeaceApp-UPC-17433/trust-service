package pe.upc.peaceapp.trust.messaging;

public interface IntegrationEventPublisher {
    void publish(String routingKey, Object payload);
}

