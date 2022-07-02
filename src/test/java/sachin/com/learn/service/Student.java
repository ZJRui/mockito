package sachin.com.learn.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Student {

    private String name;

    public Student(String name) {
        this.name = name;
    }

    public Student() {
    }
}
