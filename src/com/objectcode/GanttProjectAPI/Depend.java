package com.objectcode.GanttProjectAPI;

/**
 * Handles GanttDiagram-Task-Dependencies
 * 
 * license: LGPL v3
 * 
 * @author FBI
 */

public class Depend {
    private String fId;          
    private String fType;         // Type of a Depencency
    private String fDifference;   // Difference of Depend
    private String fHardness;
    
    public Depend(String anId) {
      fId = anId;

    }
  
    public String toString() {
      return "Depencency [Id="+fId+", Type="+fType+", Difference="+fDifference+"]";
    }
  
    public String getId() {
      return fId;
    }
  
    public void setId(String id) {
      fId = id;
    }
  
    public String getType() {
      return fType;
    }
  
    public void setType(String type) {
      fType = type;
    }

	public void setfDifference(String difference) {
		fDifference = difference;
	}

	public String getfDifference() {
		return fDifference;
	}

	public void setHardness(String hardness) {
		fHardness = hardness;
	}

	public String getHardness() {
		return fHardness;
	}
  }