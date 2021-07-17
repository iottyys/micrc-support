package io.ttyys.micrc.api.fixtures;

import io.ttyys.micrc.api.fixtures.dto.PersonCommand;
import io.ttyys.micrc.api.fixtures.dto.PersonIDto;
import io.ttyys.micrc.api.fixtures.dto.PersonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DemoService {
    private static final Logger logger = LoggerFactory.getLogger(DemoService.class);

    public PersonCommand query(PersonIDto person) {
        logger.info("DemoService.query start ......{}", person);
        person.setId(UUID.randomUUID().toString());
        person.setName("zhangsan-query");
        logger.info("DemoService.query end ......{}", person);
        PersonCommand command = PersonMapper.INSTANCE.dtoToCommand(person);
        return command;
    }

    public PersonCommand execute(PersonCommand person) {
        logger.info("DemoService.exec start ......{}", person);
        person.setId(UUID.randomUUID().toString());
        person.setName("zhangsan");
        logger.info("DemoService.exec end ......{}", person);
        return person;
    }
}
