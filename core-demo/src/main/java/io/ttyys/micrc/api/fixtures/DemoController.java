package io.ttyys.micrc.api.fixtures;

import io.ttyys.micrc.annotations.runtime.ApiLogic;
import io.ttyys.micrc.annotations.runtime.ApiQuery;
import io.ttyys.micrc.api.common.dto.Result;
import io.ttyys.micrc.api.fixtures.dto.PersonCommand;
import io.ttyys.micrc.api.fixtures.dto.PersonIDto;
import io.ttyys.micrc.api.fixtures.dto.PersonMapper;
import io.ttyys.micrc.api.fixtures.dto.PersonODto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("demo")
public class DemoController {
    @Autowired
    private DemoService demoService;

    @ApiQuery(serviceName = "demoService", mappingBean = "personMapperImpl")
    @GetMapping("getById")
    public Result<PersonODto> getById(PersonIDto person) {
        PersonCommand command = demoService.query(person);
        PersonODto out = PersonMapper.INSTANCE.toData(command);
        out.setAllName(person.getName() + "1");
        return Result.OK(out);
    }

    @ApiLogic(serviceName = "demoService", mappingBean = "personMapperImpl")
    @GetMapping("save")
    public Result<?> save(PersonIDto person) {
        demoService.execute(PersonMapper.INSTANCE.dtoToCommand(person));
        return Result.OK();
    }
}
