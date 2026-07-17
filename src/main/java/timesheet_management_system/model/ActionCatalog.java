package timesheet_management_system.model;
import java.util.ArrayList;
import java.util.List;

public class ActionCatalog {
    private List<String> actions;
    
    public ActionCatalog(){
        this.actions= new ArrayList<>();
        actions.add("");
    }

    public List<String> getActions(){
        return actions;
    }

    public void addActions(String action){
        if(!actions.contains(action)){
            actions.add(action);
        }
    }
}
