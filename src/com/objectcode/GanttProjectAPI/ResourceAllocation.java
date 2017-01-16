package com.objectcode.GanttProjectAPI;

/**
 * Handles GanttDiagram-ResourceAllocations (N:M-relationship between Resources and Tasks)
 *    
 * license: LGPL v3
 * 
 * @author heyduk
 */
public class ResourceAllocation {
  private String fTaskId;         // task
  String fResourceId;     // resource/user
  private boolean fIsResponsible; // is this reosurce/user responsible for that task?
  private String fFunction;       // function / Rolename (predefined from GanttProject or userdefined in diagram per roles-tag!
  // private float fLoad;
  
  public ResourceAllocation(String aTaskId, String aResourceId, String aIsResponsible, String aFunction) {
    fTaskId = aTaskId;
    fResourceId = aResourceId;
    setResponsible(aIsResponsible);
    fFunction = aFunction;
  }

  public ResourceAllocation(String aTaskId, String aResourceId, boolean aIsResponsible, String aFunction) {
    fTaskId = aTaskId;
    fResourceId = aResourceId;
    fIsResponsible = aIsResponsible;
    fFunction = aFunction;
  }

  public String toString() {
    return "ResourceAllocation[TaskId="+fTaskId+", ResourceId="+fResourceId+", IsResponsible="+fIsResponsible+", Function="+fFunction+"]";
  }

  public String getTaskId() {
    return fTaskId;
  }

  public void setTaskId(String taskId) {
    fTaskId = taskId;
  }

  public String getResourceId() {
    return fResourceId;
  }

  public void setResourceId(String resourceId) {
    fResourceId = resourceId;
  }

  public boolean isResponsible() {
    return fIsResponsible;
  }

  public void setResponsible(String isResponsible) {
    fIsResponsible = "TRUE".equalsIgnoreCase(isResponsible);
  }

  public void setResponsible(boolean isResponsible) {
    fIsResponsible = isResponsible;
  }
  
  public String getFunction() {
    return fFunction;
  }

  public void setFunction(String function) {
    fFunction = function;
  }
  
}
