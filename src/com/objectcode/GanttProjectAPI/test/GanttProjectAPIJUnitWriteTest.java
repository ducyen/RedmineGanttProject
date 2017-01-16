package com.objectcode.GanttProjectAPI.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.objectcode.GanttProjectAPI.GanttDiagram;

/**
 * JUnitTest-Class to check If writing in XML works
 * 
 * Tests with Demo1b.gan
 * 
 * Generates a Log (or console-output) from Xml and check if its all correctly
 *    
 * license: LGPL v3
 * 
 * @author FBI
 *
 */
public class GanttProjectAPIJUnitWriteTest {
	
    String ganttDiagramFile = "/home/objectcode/workspace/GanttProjectAPI/data/Demo1b.gan"; 		//Use this ore an equal path for Unix-systems
	//  String ganttDiagramFile = "C:\\Projects\\GanttProjectAPI\\data\\Demo1b.gan";				//Use this ore an equal path for Windows-systems
	    GanttDiagram ganttDiagram = new GanttDiagram(ganttDiagramFile);
	    String msg = ganttDiagram.loadGanttDiagram();

	
	protected static void log(String aLogMsg) {
	    // LOGGER.debug(aLogMsg);
	    System.out.println(aLogMsg);  // for standalone execution of GanttDiagram.java
	}
		
	
	 @Test
	 public void testData(){
	 
		 
     //check before change
	 assertEquals("Name", "Demo1b", ganttDiagram.getName());
	 assertEquals("version", "2.0", ganttDiagram.getVersionNo());
	 assertEquals("Task Name", "Innovation Radar", ganttDiagram.getTaskName("1"));
	 assertEquals("Resource funciton", "Driver", ganttDiagram.getResourceFunction("2"));
	 assertEquals("Property name", "info", ganttDiagram.getTaskPropertyName("tpd2"));

		 
	 
	 log("\n--------------Changes-------------------\n");
	    
	 ganttDiagram.modifyDiagram_setProjectName("Demo2");
	 ganttDiagram.modifyDiagram_setVersion("3.0");
	 ganttDiagram.modifyDiagram_setTaskName("1", "New Innovation Radar");
	 ganttDiagram.modifyDiagram_setResourceFunction("2", "0");
	 ganttDiagram.modifyDiagram_setTaskPropertyName("tpd2", "new info");
   
	 
	 
	 // Write modifications into gantt-diagram-xml-file	    
	 ganttDiagram.writeGanttDiagram();
	     
	 log("\n--------------Changed-------------------\n");
     	 
	 //check if Fields are Correct AFTER change
	 ganttDiagram.loadGanttDiagram();
	 assertEquals("New Name", "Demo2", ganttDiagram.getName());
	 assertEquals("New version", "3.0", ganttDiagram.getVersionNo());
	 assertEquals("New Task Name", "New Innovation Radar", ganttDiagram.getTaskName("1"));
	 assertEquals("New Resource function", "Coach", ganttDiagram.getResourceFunction("2"));
	 assertEquals("New Taskproperty Name", "new info", ganttDiagram.getTaskPropertyName("tpd2"));
	    	  
	 
	 
	 
	}    
}
