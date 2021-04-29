package io.ttyys.micrc.message

import io.ttyys.micrc.annotations.technology.LocalTransferConsumer

@LocalTransferConsumer(endpoint = "test", adapterClassName = "localMessageConsumer")
interface LocalMessageConsumer {

    String getUserName();
}
