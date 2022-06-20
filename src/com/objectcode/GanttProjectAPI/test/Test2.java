package com.objectcode.GanttProjectAPI.test;

import com.objectcode.GanttProjectAPI.GanttDiagram;
import com.objectcode.GanttProjectAPI.GanttDiagram.TaskKind;

/**
 * read data from gantt-xml-file, modify data and write back xml-file
 * 
 * there are some changes for testing preconfigured. Feel free to change it
 * use modifyDiagram_xxx methods
 *    
 * license: LGPL v3
 * 
 * @author heyduk, FBI
 */
public class Test2 {

  protected static void log(String aLogMsg) {
    // LOGGER.debug(aLogMsg);
    System.out.println(aLogMsg);  // for standalone execution of GanttDiagram.java
  }

  /**
   * Only for testing purposes...  (DateUtils-Calendar-usage NOT possible when using Standalone!)
   * 
   * Use Demo1b.gan for changes!
   * 
   * @param args
   */
  public static void main(String[] args) {
	String ganttDiagramFile = "/home/objectcode/workspace/GanttProjectAPI/data/Demo1b.gan";	//Use this ore an equal path for Unix-systems
//  String ganttDiagramFile = "C:\\Projects\\GanttProjectAPI\\data\\Demo1.gan";				//Use this ore an equal path for Windows-systems
    
    GanttDiagram ganttDiagram = new GanttDiagram(ganttDiagramFile);
    String msg = ganttDiagram.loadGanttDiagram();
    log("\n-------------------GanttDiagram loaded: "+ganttDiagram+" (Message="+msg+")-------------------\n\n");
    
    // read Gantt-Diagramm-Data:
    log("Project-Name: "+ganttDiagram.getName());
    log("Version: " +ganttDiagram.getVersionNo());
    log("View-Date: "+ ganttDiagram.getViewdate());
    log("View-Index: " + ganttDiagram.getViewIndex());
    log("Description: "+ganttDiagram.getDescription());
    log("DateProjectStart (calculated): "+ganttDiagram.getDateProjectStart());
    log("DateProjectEnd (calculated): "+ganttDiagram.getDateProjectEnd());
    log("Web-Link: " + ganttDiagram.getWeblink());
    log("Company: " + ganttDiagram.getCompany());
    log("PlannedResources (calculated): "+ganttDiagram.getPlannedResources());

    
    // Modify Gantt-Diagramm-Data:
    
    log("\n-----------------MODIFY GanttDiagram-------------------");

    //General-data:
    log("\n-----------------Change General-Data------------------\n");
    ganttDiagram.modifyDiagram_setProjectName("Demo1b new");
    ganttDiagram.modifyDiagram_setVersion("2.1");
    ganttDiagram.modifyDiagram_setViewDateToToday();
    ganttDiagram.modifyDiagram_setViewIndex("2");
    ganttDiagram.modifyDiagram_setDescriptionNew("New-Description");
    ganttDiagram.modifyDiagram_setWebLink("http://newtest");
    ganttDiagram.modifyDiagram_setCompany("NEW");

    //Task-data:
    log("\n------------------Change Task-Data---------------------\n");
    ganttDiagram.modifyDiagram_addTask("101", "3", "7", "NewTask", 0, new java.util.Date(), TaskKind.ACTIVITY, "1", new java.util.Date(), "created by Test2.java", null, 0);
    ganttDiagram.modifyDiagram_setTaskName("1", "new TaskName");
    ganttDiagram.modifyDiagram_setTaskMeeting("1", "true");
    ganttDiagram.modifyDiagram_setTaskStartDate("3", new java.util.Date());   
    ganttDiagram.modifyDiagram_setTaskDuration("1", "10");
    ganttDiagram.modifyDiagram_setTaskCompleteLevel("1", 50);
    ganttDiagram.modifyDiagram_setTaskPriority("1", "2");
    ganttDiagram.modifyDiagram_setTaskExpand("1", "false");
    
    //Resource-data:
    log("\n----------------Change Resource-Data--------------------\n");
    ganttDiagram.modifyDiagram_addResource("10", "NEW", "Not defined");
    ganttDiagram.modifyDiagram_setResourceName("0", "NEW");
    ganttDiagram.modifyDiagram_setResourceFunction("0", "Default:1");    
    ganttDiagram.modifyDiagram_setResourceContact("0", "TEST@abcde.de");   
    ganttDiagram.modifyDiagram_setResourcePhone("0", "55674");    
//  ganttDiagram.modifyDiagram_addTaskResourceAllocation("11", "MHE", "Project Manager"); //--> ACHTUNG: ERST M½GLICH, nachdem resources erneut eingeladen worden sind! modify_addResource() schreibt Daten NUR in xml-File, NICHT in interne Datenstruktur!
    ganttDiagram.modifyDiagram_setTaskResourceAllocation("8", "IMO", "CLI");
    
    //Role-data:
    log("\n----------------Set Resource-Data--------------------\n");
    ganttDiagram.modifyDiagram_setDriverAndCoach("MHE", "JPA");

    
    
    
      log("\n");
      // TEST: Write modifications into gantt-diagram-xml-file
      ganttDiagram.writeGanttDiagram();
      
      
      
      
      
  // check if its changed
      log ("\n\n----------------New Diagram-Data---------------\n");
  
  // General-data:  
      log("\n--------------New General-Data------------------\n");
      
      log("Project-Name: "+ganttDiagram.getName());
      log("Version: " +ganttDiagram.getVersionNo());
      log("View-Date: "+ ganttDiagram.getViewdate());
      log("View-Index: " + ganttDiagram.getViewIndex());
      log("Description: "+ganttDiagram.getDescription());
      log("DateProjectStart (calculated): "+ganttDiagram.getDateProjectStart());
      log("DateProjectEnd (calculated): "+ganttDiagram.getDateProjectEnd());
      log("Web-Link: " + ganttDiagram.getWeblink());
      log("Company: " + ganttDiagram.getCompany());
      log("PlannedResources (calculated): "+ganttDiagram.getPlannedResources());
     
      
  // Task-data:  
      log("\n--------------New Task-Data---------------------\n");
      
      log("New Task with ID= 101: "+ ganttDiagram.getTaskById("101").toString());
      log("Name from Task with Id=101: " + ganttDiagram.getTaskName("101"));
	  log("Is Task with ID=101 a Meeting?: " + ganttDiagram.getTaskMeeting("101"));
	  log("Start Date of Task with ID=101: " + ganttDiagram.getTaskStartDate("101"));
	  log("Duration of Task with ID=101: " + ganttDiagram.getTaskDuration("101"));
	  log("End Date of Task with ID=101: " + ganttDiagram.getTaskEndDate("101"));
	  log("Completelevel from Task 101: " + ganttDiagram.getTaskCompleteLevel("101"));
	  log("Priority from Task 101: " + ganttDiagram.getTaskPriority("101"));	 
	  log("Type of Task with ID=101: " + ganttDiagram.getTaskType("101"));
	  log("Dependency from Task ID=101: " + ganttDiagram.getDependencies().get("101"));    
     	
  // Resource-data
  	  log("\n-------------New Resource-Data-------------------\n");
  	
  	  log("New Resource with ID=10: " + ganttDiagram.getResourceObjectById("10"));
  	  log("Name of Resource with ID=0: " + ganttDiagram.getResourceName("0"));
      log("Funkion of Resource with Id=0: " + ganttDiagram.getResourceFunction("0"));
	  log("Contacts from Resource with ID=0: " + ganttDiagram.getResourceContacts("0"));
	  log("Phonenumber from Resource with ID=0: "+ ganttDiagram.getResourcePhone("0"));
  }

}
