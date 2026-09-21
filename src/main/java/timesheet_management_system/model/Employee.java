package timesheet_management_system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Employee{
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String name;

    @Column(unique=true, nullable=false)
    private String username;
    private String password;
    @Enumerated (EnumType.STRING)
    private Role role;
    private Boolean active = true;

    public Employee(String name, String username, String password, Role role ){
        this.name=name;
        this.username = username;
        this.password = password;
        this.role = role;
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
        public void setUsername(String username){
        this.username=username;
    }
    public String getUsername(){
        return username;
    }
        public void setPassword(String password){
        this.password=password;
    }
    public String getPassword(){
        return password;
    }

        public void setRole(Role role){
        this.role=role;
    }
    public Role getRole(){
        return role;
    }

    public boolean isActive(){
        return active == null || active;
    }
    public void setActive(boolean active){
        this.active = active;
    }



}
