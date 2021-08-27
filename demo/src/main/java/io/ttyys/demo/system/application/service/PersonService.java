package io.ttyys.demo.system.application.service;

import io.ttyys.demo.system.application.dto.PersonIDto;
import io.ttyys.demo.system.application.dto.PersonMapper;
import io.ttyys.demo.system.domain.command.PersonCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PersonService {
    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

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
