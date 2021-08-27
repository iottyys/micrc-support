package io.ttyys.demo.system.infrastructure.api.rpc;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.ttyys.demo.system.application.dto.PersonIDto;
import io.ttyys.demo.system.application.dto.PersonMapper;
import io.ttyys.demo.system.application.dto.PersonODto;
import io.ttyys.demo.system.application.service.PersonService;
import io.ttyys.demo.system.domain.command.PersonCommand;
import io.ttyys.micrc.annotations.runtime.ApiLogic;
import io.ttyys.micrc.annotations.runtime.ApiQuery;
import io.ttyys.micrc.api.common.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("人员")
@RestController
@RequestMapping("demo")
public class PersonController {
    @Autowired
    private PersonService personService;

    @ApiOperation("按id查询")
    @ApiQuery(serviceName = "demoService", mappingBean = "personMapperImpl")
    @GetMapping("getById")
    public Result<PersonODto> getById(PersonIDto person) {
        PersonCommand command = personService.query(person);
        PersonODto out = PersonMapper.INSTANCE.toData(command);
        out.setAllName(person.getName() + "1");
        return Result.OK(out);
    }

    @ApiOperation("保存")
    @ApiLogic(serviceName = "demoService", mappingBean = "personMapperImpl")
    @PostMapping("save")
    public Result<?> save(PersonIDto person) {
        personService.execute(PersonMapper.INSTANCE.dtoToCommand(person));
        return Result.OK();
    }
}
