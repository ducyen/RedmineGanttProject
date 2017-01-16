package com.objectcode.GanttProjectAPI;


/**
 * Handles GanttDiagram-Task-Properties
 *    
 * license: LGPL v3
 * 
 * @author FBI
 */
public class TaskProperty {
    private String fId;           // internal unique ID of TaskProperty in GanttProject
    private String fName;         // Name of Property
    private String fType;     
    private String fValuetype;
    
    public TaskProperty(String anId, String aName, String aType, String aValuetype) {
        fId = anId;
        fName = aName;
        fType = aType;
        fValuetype = aValuetype;
    }
    
    public String toString() {
        return "TaskProperty [Id="+fId+", Name="+fName+", Type="+fType+", ValueType="+fValuetype+ "]";
    }
        
	public void setId(String Id) {
		fId = Id;
	}
	public String getId() {
		return fId;
	}
	public void setName(String Name) {
		fName = Name;
	}
	public String getName() {
		return fName;
	}
	public void setType(String Type) {
		fType = Type;
	}
	public String getType() {
		return fType;
	}
	public void setValuetype(String Valuetype) {
		fValuetype = Valuetype;
	}
	public String getValuetype() {
		return fValuetype;
	}   
}
