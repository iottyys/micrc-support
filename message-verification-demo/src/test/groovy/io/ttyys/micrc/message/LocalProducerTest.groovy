package io.ttyys.micrc.message

import io.ttyys.micrc.integration.local.springboot.EnableLocalMessageSupport
import io.ttyys.micrc.message.LocalMessageProducer
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableLocalMessageSupport
class LocalProducerTest {

    @Autowired
    LocalMessageProducer localMessageProducer

    @Autowired
    LocalMessageProducer1 localMessageProducer1

    @Autowired
    LocalMessageConsumer localMessageConsumer

    @Autowired
    LocalMessageConsumer1 localMessageConsumer1

    @Test
    void producer() {
        localMessageProducer.setUserPassword(123456)
        // localMessageProducer1.setUser(new io.ttyys.micrc.model.User(userName: "tengwang", age: 999))
    }
}
