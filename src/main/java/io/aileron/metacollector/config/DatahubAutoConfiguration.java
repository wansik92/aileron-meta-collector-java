package io.aileron.metacollector.config;

import io.aileron.metacollector.aspect.DatahubJobAspect;
import io.aileron.metacollector.emitter.DatahubEmitter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(DatahubProperties.class)
public class DatahubAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DatahubEmitter datahubEmitter(DatahubProperties props) {
        return new DatahubEmitter(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatahubJobAspect datahubJobAspect(DatahubEmitter emitter) {
        return new DatahubJobAspect(emitter);
    }
}
