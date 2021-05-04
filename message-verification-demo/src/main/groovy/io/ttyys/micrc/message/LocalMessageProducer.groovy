package io.ttyys.micrc.message


import io.ttyys.micrc.annotations.technology.LocalTransferProducer

@LocalTransferProducer(endpoint = "test", adapterClassName = "LocalMessageProducer")
interface LocalMessageProducer {

    String setUserPassword(Integer password)
}

@LocalTransferProducer(endpoint = "test1", adapterClassName = "LocalMessageProducer1")
interface LocalMessageProducer1 {

    String getAge();

    User setUser(io.ttyys.micrc.model.User user)
}
