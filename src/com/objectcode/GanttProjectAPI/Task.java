package com.objectcode.GanttProjectAPI;

import java.util.HashSet;

import com.objectcode.GanttProjectAPI.GanttDiagram;
import com.objectcode.GanttProjectAPI.ResourceAllocation;

import java.text.ParseException;


  /**
   * Handles GanttDiagram-Tasks (Activities, Milestones, Phases)
   *    
   * license: LGPL v3
   * 
   * @author heyduk
   */
  public class Task{
    // private static final Logger LOGGER = Logger.getLogger(Task.class);
  
    private String fId;               // internal unique ID of task in GanttProject
    private String fName;             // name of task in GanttProject
    private String fNote;
    private boolean fIsActivity;
    private boolean fIsMilestone;
    private boolean fIsPhase;
    private String fParentId;
    private String fNextSiblingId;    // following child node (sibling) ; needed for re-adding of deleted tasks
    private java.sql.Date fStartDate;
    private java.sql.Date fEndDate;
    private int fDuration;            // duration in days! 
    private int fCompleteLevel;
    private String fPriority;
    private String fPath;
    private String fMeeting;
    private String fWebLink;
    private HashSet<ResourceAllocation> fResourceHash = new HashSet<ResourceAllocation>();  // HashSet with ResourceAllocation-Objects
    
    public Task(String anId, String aName) {
      fId = anId;
      fName = aName;
    }
  
    public String getType() {
      if (fIsActivity) return "Activity";
      if (fIsMilestone) return "Milestone";
      if (fIsPhase) return "Phase";
      return "INVALID";
    }
    
    public String toString() {
      return "Task[Id="+fId+", Name="+fName+", Type="+getType()+", Parent="+fParentId+", StartDate="+fStartDate+", Duration="+fDuration+", EndDate="+fEndDate+", CompleteLevel="+fCompleteLevel+", Priority="+fPriority+", Note="+fNote+", NextSibling="+fNextSiblingId+"]";
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
  
    public String getNote() {
      return fNote;
    }
  
    public void setNote(String note) {
      fNote = note;
    }
  
    public boolean isActivity() {
      return fIsActivity;
    }
  
    public void setActivity(boolean isActivity) {
      fIsActivity = isActivity;
    }
  
    public boolean isMilestone() {
      return fIsMilestone;
    }
  
    public void setMilestone(boolean isMilestone) {
      fIsMilestone = isMilestone;
    }
  
    public boolean isPhase() {
      return fIsPhase;
    }
  
    public void setPhase(boolean isPhase) {
      fIsPhase = isPhase;
    }
  
    public String getParentId() {
      return fParentId;
    }
  
    public void setParentId(String parentId) {
      fParentId = parentId;
      calculatePath(parentId);
    }

    public java.sql.Date getStartDate() {
      return fStartDate;
    }
  
    public void setStartDate(String startDate) {
      try {
        // e.g.: 2008-10-15
        java.util.Date date = GanttDiagram.gDATEFORMAT_YYYY_MM_DD.parse(startDate);
        fStartDate = new java.sql.Date(date.getTime());
      } catch (ParseException e) {
        GanttDiagram.log("Unparseable Date: '"+startDate+"'. Expected format is '"+GanttDiagram.gDATEFORMAT_YYYY_MM_DD.toPattern()+"'");
      }
    }
  
    public java.sql.Date getEndDate() {
      return fEndDate;
    }

    public int getDuration() {
      return fDuration;
    }
  
    public void setDuration(String duration) {
      fDuration = Integer.parseInt(duration);
      // calculate fEndDate:
      // if startDate = 08.12.2008 and fDuration = 1, then set endDate = 08.12.2008 (and NOT 09.12.2008) ; --> JPA, 09.12.2008
      if (getStartDate() == null) {
        fEndDate = null;
        return;
      }
      //fEndDate = new java.sql.Date(fStartDate.getTime() + ((fDuration-1) * GanttDiagram.gMILLISPERDAY));

      int aDuration = fDuration;
      	if (aDuration > 0) {
      		java.util.Date curDate = getStartDate();
      		int counter = 0;
      		while (counter < aDuration) {
      			String curDateType = GanttDiagram.fCalendars.get(GanttDiagram.gDATEFORMAT_YYYY_MM_DD.format(curDate));
      			if (curDateType != null && curDateType.equals("HOLIDAY") || 
      				curDate.getDay() == 0 ||	// Sunday
      				curDate.getDay() == 6		// Saturday
      			) {
      				aDuration += 1;
      			}
      			curDate = new java.util.Date(curDate.getTime() + GanttDiagram.gMILLISPERDAY);
      			counter++;
      		}
            fEndDate = new java.sql.Date(fStartDate.getTime() + ((aDuration-1) * GanttDiagram.gMILLISPERDAY));
      	} else {
      		fEndDate = getStartDate();
      	}
    
    }
  
    public int getCompleteLevel() {
      return fCompleteLevel;
    }
  
    public void setCompleteLevel(String completeLevel) {
      fCompleteLevel = Integer.parseInt(completeLevel);
    }
  
    public String getPriority() {
      return fPriority;
    }
  
    public void setPriority(String priority) {
      fPriority = priority;
    }

    public String getNextSiblingId() {
      return fNextSiblingId;
    }

    public void setNextSiblingId(String nextSiblingId) {
      fNextSiblingId = nextSiblingId;
    }

    private void calculatePath(String parentId) {
      StringBuffer pathBuffy = new StringBuffer(); 
      Task taskObject;
      for (int i=0; i<10; i++) {
        if (parentId == null || "NULL".equalsIgnoreCase(parentId)) break;
        // get taskObject from Activity/Milestone-Hashtable or from Phases-Hashtable:
        taskObject = (Task) GanttDiagram.fActivitiesAndMilestones.get(parentId);
        if (taskObject == null) {
          taskObject = (Task) GanttDiagram.fPhases.get(parentId);
        }
        if (taskObject == null) {
          GanttDiagram.log("ERROR: Unexpected: task with id "+parentId+" not found in Hashtables built from GanttDiagramm...");
        }
        pathBuffy.insert(0, taskObject.getName()+GanttDiagram.gPATH_DELIMITER);
        parentId = taskObject.getParentId();
      }
      pathBuffy.append(this.getName());
      fPath = pathBuffy.toString();
    }

    public String getPath() {
      return fPath;
    }

    public void addUser(String aResourceId, boolean isResponsible, String aFunction) {
      ResourceAllocation resourceAllocation = new ResourceAllocation(getId(), aResourceId, isResponsible, aFunction);
      fResourceHash.add(resourceAllocation);
    }
    
    public HashSet<ResourceAllocation> getResourceHash() {
      return fResourceHash;
    }

	public String getMeeting() {
		return fMeeting;
	}

	public void setMeeting(String Meeting) {
		fMeeting = Meeting;
	}
 
	public String getWebLink() {
		return fWebLink;
	}

	public void setWebLink(String WebLink) {
		fWebLink = WebLink;
	}
  }