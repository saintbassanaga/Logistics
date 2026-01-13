package tech.bytesmind.logistics.shared.event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Configuration class for setting up event bus-related components.
 * This class provides the beans and configurations necessary for event handling
 * in a Spring application.
 */
@Configuration
public class EventBusConfig {

    /**
     * Creates and configures an {@link ApplicationEventMulticaster} instance.
     * The returned event multicaster is set up with an asynchronous task executor to handle event processing.
     *
     * @return a configured {@link ApplicationEventMulticaster} that supports asynchronous event handling
     */
    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}