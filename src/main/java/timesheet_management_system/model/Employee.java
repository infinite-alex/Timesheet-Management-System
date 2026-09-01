package timesheet_management_system.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Employee{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String name;

    public Employee(String name){
        this.name=name;
    }
    protected Employee(){

    }

    public void setName(String name){
        this.name=name;
    }
    public String getName(){
        return name;
    }
    public Long getId(){
        return id;
    }


}
