package io.ttyys.demo.system.application.dto;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;

public class PersonIDto {
    @NotBlank(message = "id不能为空")
    @Length(min = 1, max = 10, message = "长度应该在1-10之间")
    private String id;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PersonDto{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
