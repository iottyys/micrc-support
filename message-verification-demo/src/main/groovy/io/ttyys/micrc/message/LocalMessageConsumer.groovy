package io.ttyys.micrc.message

import groovy.util.logging.Slf4j
import io.ttyys.micrc.annotations.technology.LocalTransferConsumer
import org.springframework.stereotype.Component

@LocalTransferConsumer(endpoint = "test", adapterClassName = "LocalMessageConsumer")
interface LocalMessageConsumer {

    String getUserName(String userId)

    String setUserPassword(Integer password)
}

@LocalTransferConsumer(endpoint = "test1", adapterClassName = "LocalMessageConsumer1")
interface LocalMessageConsumer1 {

    String getAge();

    User setUser(User user)
}

@Component
@Slf4j
class LocalMessageConsumerImpl implements LocalMessageConsumer{

    @Override
    String getUserName(String userId) {
        log.debug("调用成功-data is {}", userId)
        return userId
    }

    @Override
    String setUserPassword(Integer password) {
        log.debug("调用成功-data is {}", password)
        return password
    }
}

@Component
@Slf4j
class LocalMessageConsumer1Impl implements LocalMessageConsumer1{

    @Override
    String getAge() {
        log.debug("调用成功-data is {}")
        return "调用成功-data is {}"
    }

    @Override
    User setUser(User user) {
        log.debug("调用成功-data is {}", user)
        return user
    }
}

class User {
    String userName

    Integer age
}