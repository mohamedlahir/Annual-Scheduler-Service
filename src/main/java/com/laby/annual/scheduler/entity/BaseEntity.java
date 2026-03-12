package com.laby.annual.scheduler.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.Date;

@Getter
@Setter
@ToString
public class BaseEntity {


    private String createdBy;
    private String updatedBy;
    private Date updatedAt;
    private Date createdAt;

}
