package timesheet_management_system.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class TimesheetEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    private WorkingMonth workingmonth;

    private int totalMinutes;

    @ElementCollection
    @CollectionTable(name = "timesheet_entry_actions", joinColumns = @JoinColumn(name = "timesheet_entry_id"))
    @Column(name = "action")
    private List<String> actions;

    private String extranote;


    public TimesheetEntry(LocalDate date,Employee employee,Client client,WorkingMonth workingmonth,
        int totalMinutes,List<String> actions, String extranote){
        this.date = date;
        this.employee = employee;
        this.client = client;
        this.workingmonth = workingmonth;
        this.totalMinutes = totalMinutes;
        this.actions = new ArrayList<>(actions);
        this.extranote=extranote;

    }
    protected TimesheetEntry(){

    }
    public void setClient(Client client){
        this.client = client;
    }
    public void setWorkingMonth(WorkingMonth workingmonth){
        this.workingmonth = workingmonth;
    }
    public void setTotalMinutes(int totalMinutes){
        this.totalMinutes = totalMinutes;
    }
    public void setActions(List<String> actions){
        this.actions = new ArrayList<>(actions);
    }
    public void setExtraNote(String extranote){
        this.extranote = extranote;
    }

    public LocalDate getDate(){
        return date;
    }
    public Employee getEmployee(){
        return employee;
    }
    public Client getClient(){
        return client;
    }
    public WorkingMonth getWorkingMonth(){
        return workingmonth;
    }
    public int getTotalMinutes(){
        return totalMinutes;
    }
    public List<String> getActions(){
        return new ArrayList<>(actions);
    }
    public String getExtraNote(){
        return extranote;
    }
    public Long getId(){
        return id;
    }


}