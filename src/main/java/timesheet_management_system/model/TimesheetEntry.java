package timesheet_management_system.model;

import java.time.LocalDate;
import java.util.List;


public class TimesheetEntry {
    private LocalDate date;
    private Employee employee;
    private Client client;
    private WorkingMonth workingmonth;
    private int totalMinutes;
    private List<String> actions;
    private String extranote;


public TimesheetEntry(LocalDate date,Employee employee,Client client,WorkingMonth workingmonth,
    int totalMinutes,List<String> actions, String extranote){
    this.date = date;
    this.employee = employee;
    this.client = client;
    this.workingmonth = workingmonth;
    this.totalMinutes = totalMinutes;
    this.actions = actions;
    this.extranote=extranote;

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
    this.actions = actions;
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
    return actions;
}
public String getExtraNote(){
    return extranote;
}


}