package com.objectcode.GanttProjectAPI.test;

import com.objectcode.GanttProjectAPI.GanttDiagram;

/**
 * delete data from gantt-xml-file
 *    
 * license: LGPL v3
 * 
 * @author heyduk, FBI
 */
public class Test3 {

  protected static void log(String aLogMsg) {
    // LOGGER.debug(aLogMsg);
    System.out.println(aLogMsg);  // for standalone execution of GanttDiagram.java
  }

  /**
   * Only for testing purposes...
   * 
   * @param args
   */
  public static void main(String[] args) {
     String ganttDiagramFile = "/home/objectcode/workspace/GanttProjectAPI/data/Demo3.gan"; 	//Use this or an equal path for Unix-systems
    // String ganttDiagramFile = "C:\\Projects\\GanttProjectAPI\\data\\Demo3.gan";				//Use this or an equal path for Windows-systems

    GanttDiagram ganttDiagram = new GanttDiagram(ganttDiagramFile);
    String msg = ganttDiagram.loadGanttDiagram();   
    log("\nGanttDiagram loaded: "+ganttDiagram+" (Message="+msg+")\n\n");
    
    //delete-data:
    
    log("\n---------------delete data------------------\n");
    ganttDiagram.modifyDiagram_deleteTask("5");
    ganttDiagram.modifyDiagram_deleteResource("1");
    ganttDiagram.modifyDiagram_deleteTaskProperty("tpd1");
    ganttDiagram.modifyDiagram_deleteDependency("2");
    ganttDiagram.writeGanttDiagram();
    
    log("\n----------------deleted---------------------\n");
    
    // load-data:
    ganttDiagram.loadGanttDiagram();

  }

}
