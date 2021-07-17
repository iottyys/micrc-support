package io.ttyys.micrc.api.fixtures.dto;

import javax.validation.constraints.NotBlank;

public class PersonODto {
    private String id;
    @NotBlank(message = "名称不能为空")
    private String allName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAllName() {
        return allName;
    }

    public void setAllName(String allName) {
        this.allName = allName;
    }
}
