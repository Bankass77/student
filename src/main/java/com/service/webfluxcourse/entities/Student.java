package com.service.webflux.entities;


import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Table("students")
@Data
public class Student {

    private Long id;
    private String name;
    private double gpa;

    public Student() {

    }

    public Student(String name, double gpa) {
        this.name = name;
        this.gpa = gpa;
    }

}
