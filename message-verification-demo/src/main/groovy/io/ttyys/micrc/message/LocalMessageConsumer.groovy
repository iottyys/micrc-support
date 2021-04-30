package io.ttyys.micrc.message

import io.ttyys.micrc.annotations.technology.LocalTransferConsumer

@LocalTransferConsumer(endpoint = "test", adapterClassName = "LocalMessageConsumer")
interface LocalMessageConsumer {

    String getUserName(String userId)

    String setUserPassword(Integer password)
}

@LocalTransferConsumer(endpoint = "localMessageConsumer1", adapterClassName = "LocalMessageConsumer1")
interface LocalMessageConsumer1 {

    String getAge();

    User setUser(User user)
}

class User {
    String userName

    Integer age
}