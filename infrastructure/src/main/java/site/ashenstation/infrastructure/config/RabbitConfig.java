package site.ashenstation.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // ---------- 队列 ----------
    @Bean
    public Queue videoConversion() {
        return new Queue("video.conversion", true);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue("payment.queue", true);
    }


    // ---------- 交换机 ----------
    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange("business.exchange", true, false);
    }

    // ---------- 绑定 ----------
    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(videoConversion())
                .to(businessExchange())
                .with("video.conversion");
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(businessExchange())
                .with("payment.pay");
    }
}
