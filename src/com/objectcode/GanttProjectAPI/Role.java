package com.objectcode.GanttProjectAPI;

 /**
   * Handles userdefined roles in GanttDiagram
   *    
   * license: LGPL v3
   * 
   * @author heyduk
   */
  public class Role {
    private String fId;           // internal unique ID of userdefined Role in GanttProject
    private String fName;         // Name/Identifier of role
    
    public Role(String anId, String aName) {
      fId = anId;
      fName = aName;
    }
  
    public String toString() {
      return "Role [Id="+fId+", Name="+fName+"]";
    }
  
    public String getId() {
      return fId;
    }
  
    public void setId(String id) {
      fId = id;
    }
  
    public String getName() {
      return fName;
    }
  
    public void setName(String name) {
      fName = name;
    }
  }