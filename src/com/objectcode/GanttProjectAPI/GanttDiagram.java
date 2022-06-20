package com.objectcode.GanttProjectAPI;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.objectcode.GanttProjectAPI.util.XMLHelper;

/**
 * Wrapper-class for GanttDiagramms. Has ability to construct itself (load data) from GanttProject-XML-File.
 * Mainly data from gantt xml-file is READ.<br/><br/> 
 * 
 * But also some few modifications are possible to write back changes 
 * into the gantt-xml-file (see modifyDiagram_XXX-methods).
 * <li>modifyDiagram_setDriverAndCoach(String aDriverShortname, String aCoachShortname);</li>
 * <li>xxx</li>
 * <br/> 
 *
 * Zum Standalone-Testen von GanttDiagram.java:
 *  - private static void log(String aLogMsg)  --> System.out einkommentieren!
 *   
 * license: LGPL v3
 * 
 * @author heyduk, FBI
 */
public class GanttDiagram {
//  private static final Logger LOGGER = Logger.getLogger(GanttDiagram.class);
  public static final SimpleDateFormat gDATEFORMAT_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
  public static final String gPATH_DELIMITER = " / ";
  public enum TaskKind {
	PROJECT,  
	MILESTONE,  
	ACTIVITY  
  };
  
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  
  private org.w3c.dom.Document fDom; 
  private Element fRoot; 
  private NodeList nodelist;
  
  private String fXMLFile;
  private String fName;
  private String fVersionNo;
  private String fDescription;
  private String fViewdate;											// FBI
  private String fWeblink;											// FBI
  private String fCompany;											// FBI
  private String fViewIndex;										// FBI
  public static Hashtable<String, ArrayList<Depend>> fDependendcy = new Hashtable<String, ArrayList<Depend>>();			  // <DependId> | <Depend-Objekt> // FBI
  public static Hashtable<String, Task> fActivitiesAndMilestones = new Hashtable<String, Task>();     // <TaskId> | <Task-Object> 
  public static Hashtable<String, Task> fPhases = new Hashtable<String, Task>();                      // <TaskId> | <Task-Object> 
  public static Hashtable<String, String> fCalendars = new Hashtable<String, String>();
  private Hashtable<String, String> fParents = new Hashtable<String, String>();                     // <Child-TaskId> | <Parent-TaskId>
  private Hashtable<String, Role> fRole = new Hashtable<String, Role>();                        // <RoleID> | <Role-Object>
  private Hashtable<String, Resource> fResources = new Hashtable<String, Resource>();                   // <ResourceID> | <Resource-Object>
  private Hashtable<String, HashSet<ResourceAllocation>> fResourceAllocation = new Hashtable<String, HashSet<ResourceAllocation>>();          // <ResourceID> | <HashSet of ResourceAllocation-Objects>
  private Hashtable<String, TaskProperty> fTaskProperty = new Hashtable<String, TaskProperty>();				  // <TaskPropertyId> | <TaskProperty-Objekt> // FBI
  
  // calculated fields:
  private java.sql.Date fDateProjectStart;                          // calculated: minimum of all activities' StartDates
  private java.sql.Date fDateProjectEnd;                            // calculated: maximum of all activities' EndDates
  private int fPlannedResources;                                    // calculated: sum of all activities' durations (unit there is days!) ; unit here is hours!
  
  public static final String gTEMPLATE_STRING_DRIVER = "DDD";
  public static final String gTEMPLATE_STRING_COACH = "CCC";
  public static final int gTASK_COMPLETE_LEVEL_PLANNED = 0;
  public static final int gTASK_COMPLETE_LEVEL_INPROGRESS = 50;
  public static final int gTASK_COMPLETE_LEVEL_COMPLETED = 100;
  
  public static final int gHOURS_PER_WORKDAY = 8;
  public static final long gMILLISPERDAY = 24*3600*1000;
  
  public GanttDiagram(String aXMLFile) {
    fXMLFile = aXMLFile;
  }

  
  /**
   * Workaround for Bug: "[Fatal Error] :86:11: Content is not allowed in trailing section."<br>
   * --&gt; GanttProject sometimes writes illegal characters behind the last closing xml-tag,<br> 
   *     which is forbidden, so that the xml-parser throws an error. <br>
   *     These characters might be EOL (EndOfLine) or EOF (EndOfFile)-characters or is a characterset-problem (UTF-8 vs. ASCII?)<br>
   *     The problem does not occur under windows-OS, but only in the combination windows and linux. <br><br>
   * Workaround: read and rewrite xml file and delete all characters after the last closing xml-tag.  <br>
   * 
   * @return
   */
  public void deleteSpecialCharactersFromGanttDiagramFile() {
    try {
      String fixedXMLFile =  fXMLFile.substring(0, fXMLFile.length()-4) + "_fixed.gan";
      log("Workaround: trim xml-file (delete forbidden charactzers at end of file)");
      FileReader fr = new FileReader(fXMLFile); 
      FileWriter fw = new FileWriter(fixedXMLFile); 
      BufferedReader br = new BufferedReader(fr); 
      BufferedWriter bw = new BufferedWriter(fw); 
      boolean fileChanged = false;
      String zeile=br.readLine(); 
      while (zeile != null ) { 
        bw.write(zeile + LINE_SEPARATOR) ; 
        zeile=br.readLine(); 
        if (zeile.indexOf("</project>")>= 0) {
          int oldLineLength = zeile.length();
          log("zeile: "+zeile);
          zeile = zeile.substring(0, zeile.indexOf("</project>")+10);
          log("zeile: "+zeile);
          if (oldLineLength != zeile.length()) {
            log("more characters in line '</project>'!");
            fileChanged = true;
          }
          if (br.readLine() != null) {
            log("more characters in line AFTER '</project>'!");
            fileChanged = true;
          }
          break;
        }
      } 
      bw.write(zeile + LINE_SEPARATOR) ; 
      br.close(); 
      bw.close(); 
      log("==> fileChanged: "+fileChanged);
      
      // use new/fixed master gantt file from now on...
      if (fileChanged) {
        fXMLFile = fixedXMLFile;
        log("use new/fixed master gantt file from now on: "+fixedXMLFile+" (Workaround: trim xml-file and delete forbidden characters at end of file)");
      } else {
        File newFile = new File(fXMLFile+".tmp");
        boolean ok = newFile.delete();
        log(".tmp-file deleted: "+ok);
      }
    } catch (Exception e) {
      log("ERROR: Error rewriting .gan-file: "+e);
      e.printStackTrace();
    }
  }

  /**
   * load gantt diagram from xml-file and fill internal data structures
   * 
   * @return
   */
  public String loadGanttDiagram() {
    FileInputStream is=null;
    try {
      clearData();
      
      is = new FileInputStream(fXMLFile);
      fDom = XMLHelper.parseXmlFile(is);
      if (fDom == null) throw new Exception("Error parsing XMLFile!");
      fRoot = fDom.getDocumentElement();

      loadGeneralData();
      loadCalendars();
      loadTasks();
      loadRoles();
      loadResources();
      loadAllocations();
      loadTaskProperties(); // FBI

      return null;
    } catch (Exception e) {
      log("ERROR: Error loading/parsing GanttDiagram: "+e);
      e.printStackTrace();
      return e.toString();
    } finally {
      try {
        is.close();
      } catch (IOException e2) {
        log("ERROR: error closing input stream...:"+e2);
        e2.printStackTrace();
      }
    }
  }

  private void clearData() {
    fRole = new Hashtable<String, Role>();
    fActivitiesAndMilestones = new Hashtable<String, Task>(); 
    fPhases = new Hashtable<String, Task>(); 
    fParents = new Hashtable<String, String>();
    fResources = new Hashtable<String, Resource>();
    fResourceAllocation = new Hashtable<String, HashSet<ResourceAllocation>>();
    fDependendcy = new Hashtable<String, ArrayList<Depend>>(); // FAB
    fTaskProperty = new Hashtable<String, TaskProperty>(); // FAB
  }
  
  private void loadGeneralData() throws Exception
  {
    try {
      fName = fRoot.getAttributeNode("name").getValue(); 
      fVersionNo = fRoot.getAttributeNode("version").getValue();
      fViewdate = fRoot.getAttributeNode("view-date").getValue();    // FBI
      fWeblink = fRoot.getAttributeNode("webLink").getValue();    	 // FBI
      fCompany = fRoot.getAttributeNode("company").getValue();    	 // FBI
      fViewIndex = fRoot.getAttributeNode("view-index").getValue();  // FBI
      fDescription = XMLHelper.getTextValueFromXmlElement(fRoot, "description"); 
      
      log("name='"+fName+"', VersionNo='"+fVersionNo+"', description='"+fDescription+"'"+", view-date= '"+fViewdate+"'"+", webLink= '"+fWeblink+"'"+", company= '"+fCompany+"'"); // FBI: view-date + Weblink + Company
    } catch (Exception e) {
      log("ERROR: Error loading GENERALDATA from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Load Tasks into fTasks-Hashtable (format: &lt;TaskID&gt; | &lt;Task-Object&gt;)
   * <br><br>
   * Task-Elements may contain inner task-elements (children). Then we call it a "Phase".<br>
   * Task-Elements without inner task-elements (children) we call "Activities".<br>
   * "Activities" are "Milestones", if the Milestone/Meilenstein-Checkbox is checked (AttributeNode: meeting).<br>
   * <br><br>
   * e.g. <br>
   * &lt;task id="0" name="Phase Idea" color="#8cb6ce" meeting="false" start="2008-09-09" duration="11" complete="27" priority="1" expand="true"&gt;<br>
   * &nbsp;&nbsp;  &lt;notes&gt;&lt;![CDATA[The customer specific article list has to be reviewed by every GE to finalise the article list]]&gt;&lt;/notes&gt;<br>
   * &lt;/task&gt;<br>
   * <br><br><br>
   *  
   * e.g.:<br>
   * &nbsp;&nbsp;  &lt;task id="2" name="Customer Specific Article List" meeting="false" start="2007-06-18" duration="30" complete="0" priority="1" expand="false"&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;notes&gt;&lt;![CDATA[The customer specific article list has to be reviewed by every GE to finalise the article list]]&gt;&lt;/notes&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;depend id="24" type="2" difference="0" hardness="Strong"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;depend id="128" type="2" difference="0" hardness="Strong"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;customproperty taskproperty-id="tpc0" value=""/&gt;<br>
   * &nbsp;&nbsp;  &lt;/task&gt;
   * 
   * @throws Exception
   */
  private void loadTasks() throws Exception
  {
    try {
      nodelist = fRoot.getElementsByTagName("tasks");
      Element tasks = (Element) nodelist.item(0);

      nodelist = tasks.getElementsByTagName("task"); // goes deep into XML-Tags
      fPlannedResources = 0;
      Element task, childTask, dependency;			// FBI: depencency;
      NodeList nodelistChildren, nodelistNotes, nodelistDepend; // FBI: nodelistDepend
      String taskId, childTaskId, parentId, dependencyId; // FBI: dependencyId;
      boolean isMilestone;
      int noTasks = nodelist.getLength();
      String lastTaskId="", lastParentId = "";
      for (int taskIdx=0; taskIdx<noTasks; taskIdx++) {
        task = (Element) nodelist.item(taskIdx);  //iteration
        // log("----------------------\ntask-Element["+taskIdx+"]="+task);
        nodelistChildren = task.getElementsByTagName("task"); // goes deep into XML-Tags
        taskId = task.getAttributeNode("id").getValue(); 
        Task taskObject = new Task(taskId, task.getAttributeNode("name").getValue());
        Node node;
        taskObject.setStartDate(task.getAttributeNode("start").getValue());
        taskObject.setDuration(task.getAttributeNode("duration").getValue());
        taskObject.setCompleteLevel(task.getAttributeNode("complete").getValue());
        if (task.getAttributeNode("webLink") != null ) {
        	taskObject.setWebLink(task.getAttributeNode("webLink").getValue());
        }
        node = task.getAttributeNode("priority");
        if (node != null) {
        	taskObject.setPriority(task.getAttributeNode("priority").getValue());
        } else {
        	taskObject.setPriority("1");
        }
        isMilestone = "TRUE".equalsIgnoreCase((String) task.getAttributeNode("meeting").getValue());
        parentId = (String) fParents.get(taskId);  // get parent from fParents-Hashtable, where all phases insert their children with format: <childTaskId> | <parentTaskId>
        taskObject.setParentId(parentId);

        // set next sibling task --> following child node (sibling) ; needed for re-adding of deleted tasks
        if (parentId != null) {
          // set previous task.nextSibling = this task
          if (parentId.equals(lastParentId)) {
            Task lastTaskObject = (Task) fActivitiesAndMilestones.get(lastTaskId);
            if (lastTaskObject != null) {
              lastTaskObject.setNextSiblingId(taskId);
            }
          }
        }
        lastTaskId = taskId;
        lastParentId = parentId;


//      log("task: "+task.getAttributeNode("id").getValue());
        nodelistDepend = task.getChildNodes();
        ArrayList<Depend> depends = new ArrayList<Depend>();
        for (int idx = 0; idx < nodelistDepend.getLength(); idx++) {
			if (nodelistDepend.item(idx).getNodeName().equals("depend")) {
				dependency = (Element) nodelistDepend.item(idx);
				dependencyId = dependency.getAttributeNode("id").getValue();
	//			log("- dependency-id: "+dependencyId);
				Depend dependObject = new Depend(dependencyId);
				if (dependency.getAttributeNode("difference") != null) {
					dependObject.setfDifference(dependency.getAttributeNode("difference").getValue());
				}
				dependObject.setType(dependency.getAttributeNode("type").getValue());
				dependObject.setHardness(dependency.getAttributeNode("hardness").getValue());
				depends.add(dependObject);
	//			log(dependObject.toString());
			}
		}
		fDependendcy.put(taskId, depends);
        
        // notes
        nodelistNotes = task.getChildNodes(); // cannot use task.getElementsByTagName("notes"), because you would find notes on deeper child-tasks! 
        for (int idx=0; idx<nodelistNotes.getLength(); idx++) {
          if (nodelistNotes.item(idx).getNodeName().equals("notes")) {
        	if (nodelistNotes.item(idx).getTextContent() == null) {
        		continue;
        	}
            String note = nodelistNotes.item(idx).getTextContent().trim();
            if (note.startsWith("[#cdata-section: "))
              note = note.substring(17); 
            if (note.startsWith("[#cdata-section:"))
              note = note.substring(16); 
            if (note.endsWith("]"))
              note = note.substring(0, note.length()-1);
            taskObject.setNote(note);
          }
        }
        
        if (nodelistChildren.getLength() == 0) {
          // tasks without subtasks are ACTIVITIES OR MILESTONES
          if (isMilestone) {
            taskObject.setMilestone(true);
          } else {
            taskObject.setActivity(true);
            // calculate fTotalDuration
            fPlannedResources += (taskObject.getDuration() * gHOURS_PER_WORKDAY); 
          }
          fActivitiesAndMilestones.put(taskId, taskObject);
          // calculate fDateProjectStart / fDateProjectEnd:
          if (fDateProjectStart == null || taskObject.getStartDate().before(fDateProjectStart)) {
            fDateProjectStart = taskObject.getStartDate();
          }
          if (fDateProjectEnd == null || taskObject.getEndDate().after(fDateProjectEnd)) {
            fDateProjectEnd = taskObject.getEndDate();
          }
        } else {
          // tasks with subtasks are PHASES
          taskObject.setPhase(true);
          // put all childs into fParents-Hashtable
          int noChildTasks = nodelistChildren.getLength();
          for (int taskIdx2=0; taskIdx2<noChildTasks; taskIdx2++) {
            childTask = (Element) nodelistChildren.item(taskIdx2);
            childTaskId = childTask.getAttributeNode("id").getValue();
            fParents.put(childTaskId, taskId);  // Each phase writes its children into fParents-Hashtable with format: <childTaskId> | <parentTaskId>
          }
          fPhases.put(taskId, taskObject);
        }
        // log("task["+taskIdx+"]: "+taskObject);
      }

      // Logging:
      logPhases();
      logActivitiesAndMilestones();
      logDependencies();
      

    } catch (Exception e) {
      log("ERROR: Error loading TASKS from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Load Resources into fResources-Hashtable (format: &lt;ResourceID&gt; | &lt;Resource-Object&gt;)
   * <br><br>
   * e.g.:  <br>
   * &nbsp;&nbsp;  &lt;resources&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;resource id="0" name="JPA" function="Default:0" contacts="" phone=""/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;resource id="1" name="IMO" function="Default:0" contacts="" phone=""/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;resource id="2" name="CLI" function="Default:0" contacts="" phone=""/&gt;<br>
   * &nbsp;&nbsp;  &lt;/resources&gt;
   *
   * @throws Exception
   */
  private void loadResources() throws Exception
  {
    try {
      nodelist = fRoot.getElementsByTagName("resources");
      Element resources = (Element) nodelist.item(0);

      nodelist = resources.getElementsByTagName("resource"); 

      Element resource;
      String resourceId, name, function;
      int noResources = nodelist.getLength();
      for (int idx=0; idx<noResources; idx++) {
        resource = (Element) nodelist.item(idx);
        // log("----------------------\ntask-Element["+taskIdx+"]="+task);
        resourceId = resource.getAttributeNode("id").getValue(); 
        name = resource.getAttributeNode("name").getValue();
        function = getRoleNameForFunction(resource.getAttributeNode("function").getValue());
        Resource resourceObject = new Resource(resourceId, name, function, resource.getAttributeNode("function").getValue(), resource.getAttributeNode("phone").getValue());
        fResources.put(resourceId, resourceObject);
      }
      
      // Logging:
      logResources();

    } catch (Exception e) {
      log("ERROR: Error loading RESOURCES from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }

  private void loadCalendars() throws Exception
  {
    try {
      nodelist = fRoot.getElementsByTagName("calendars");
      Element calendars = (Element) nodelist.item(0);

      nodelist = calendars.getElementsByTagName("date"); 

      Element theDate;
      int year, month, date;
      String type;
      int noDates = nodelist.getLength();
      for (int idx=0; idx<noDates; idx++) {
    	theDate = (Element) nodelist.item(idx);
        // log("----------------------\ntask-Element["+taskIdx+"]="+task);
        year = Integer.parseInt(theDate.getAttributeNode("year").getValue()); 
        month = Integer.parseInt(theDate.getAttributeNode("month").getValue());
        date = Integer.parseInt(theDate.getAttributeNode("date").getValue());
        type = theDate.getAttributeNode("type").getValue();
        java.util.Date dateObj = new java.util.Date(year - 1900, month-1, date);
        fCalendars.put(gDATEFORMAT_YYYY_MM_DD.format(dateObj), type);
      }
      
      // Logging:
      logCalendars();

    } catch (Exception e) {
      log("ERROR: Error loading RESOURCES from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }
  
  /**
   * a) predefined roles in GanttProject:<br>
   * &nbsp;&nbsp;  "Default:0"             = Not defined<br>
   * &nbsp;&nbsp;  "Default:1"             = Project Manager<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:2" = Developer<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:3" = Handbook Writer<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:4" = Tester<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:5" = Graphic Designer<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:6" = Translator<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:7" = Packager<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:8" = Analyzer<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:9" = Web Designer<br>
   * &nbsp;&nbsp;  "SoftwareDevelopment:10"= No special<br>
   *   <br>
   * b) userdefined roles in diagram:<br>
   * &nbsp;&nbsp; &lt;project&gt;<br>
   * &nbsp;&nbsp;&nbsp;   ...<br>
   * &nbsp;&nbsp;&nbsp;   &lt;roles&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     &lt;role id="0" name="<RoleName1>"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     &lt;role id="1" name="<RoleName2>"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;     ...<br>
   * &nbsp;&nbsp;&nbsp;   &lt;/roles&gt;<br>
   * &nbsp;&nbsp; &lt;/project&gt;<br>
   *  
   * @param aFunctionCode
   * @return
   */
  public String getRoleNameForFunction(String aFunctionCode) {
    if (aFunctionCode == null || aFunctionCode.length() == 0) return null;
    if (aFunctionCode.startsWith("Default")) {
      if (aFunctionCode.equals("Default:0")) return "Not defined"; 
      if (aFunctionCode.equals("Default:1")) return "Project Manager";
      return null;
    } else if (aFunctionCode.startsWith("SoftwareDevelopment")) {
      if (aFunctionCode.equals("SoftwareDevelopment:2")) return "Developer"; 
      if (aFunctionCode.equals("SoftwareDevelopment:3")) return "Handbook Writer"; 
      if (aFunctionCode.equals("SoftwareDevelopment:4")) return "Tester"; 
      if (aFunctionCode.equals("SoftwareDevelopment:5")) return "Graphic Designer"; 
      if (aFunctionCode.equals("SoftwareDevelopment:6")) return "Translator"; 
      if (aFunctionCode.equals("SoftwareDevelopment:7")) return "Packager"; 
      if (aFunctionCode.equals("SoftwareDevelopment:8")) return "Analyzer"; 
      if (aFunctionCode.equals("SoftwareDevelopment:9")) return "Web Designer"; 
      if (aFunctionCode.equals("SoftwareDevelopment:10")) return "No special"; 
      return null;
    } else {
      // userdefined roles in diagram:
      Role roleObject = (Role) fRole.get(aFunctionCode);
      if (roleObject != null)
        return roleObject.getName();
      return "";
    }
  }

  public String getFunctionNameForRole(String aRole) {
    if (aRole == null || aRole.length() == 0) return null;
    if (aRole.equals("Not defined"))
      return "Default:0"; 
    if (aRole.equals("Project Manager"))
      return "Default:1"; 
    if (aRole.equals("Developer"))
      return "SoftwareDevelopment:2"; 
    if (aRole.equals("Handbook Writer"))
      return "SoftwareDevelopment:3"; 
    if (aRole.equals("Tester"))
      return "SoftwareDevelopment:4"; 
    if (aRole.equals("Graphic Designer"))
      return "SoftwareDevelopment:5"; 
    if (aRole.equals("Translator"))
      return "SoftwareDevelopment:6"; 
    if (aRole.equals("Packager"))
      return "SoftwareDevelopment:7"; 
    if (aRole.equals("Analyzer"))
      return "SoftwareDevelopment:8"; 
    if (aRole.equals("Web Designer"))
      return "SoftwareDevelopment:9"; 
    if (aRole.equals("No special"))
      return "SoftwareDevelopment:10";

    Iterator<String> it = fRole.keySet().iterator();
    while (it.hasNext()) {
      String functionCode = (String) it.next();
      Role roleObject = (Role) fRole.get(functionCode);
      if (roleObject.getName().equals(aRole))
        return roleObject.getId();
    }
    return "";
  }
  
  /**
   * Load ResourceAllocations (N:M-relationship) into fResourceAllocation-Hashtable (format: <ResourceID> | <HashSet of ResourceAllocation-Objects>)
   * <br><br>
   * e.g.<br>
   * &nbsp; &lt;allocations&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;allocation task-id="1" resource-id="2" function="Default:0" responsible="false" load="100.0"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;allocation task-id="2" resource-id="0" function="Default:0" responsible="true" load="100.0"/&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;    &lt;allocation task-id="4" resource-id="1" function="Default:0" responsible="true" load="100.0"/&gt;<br>
   * &nbsp; &lt;/allocations>
   *  
   * @throws Exception
   */
  private void loadAllocations() throws Exception
  {
    try {
      nodelist = fRoot.getElementsByTagName("allocations");
      Element allocations  = (Element) nodelist.item(0);

      nodelist = allocations.getElementsByTagName("allocation");

      Task taskObject; 
      Element resourceAllocation;
      String taskId, resourceId, responsible, function;
      HashSet<ResourceAllocation> resourceAllocationSet;
      int noAllocations = nodelist.getLength();
      for (int idx=0; idx<noAllocations; idx++) {
        resourceAllocation = (Element) nodelist.item(idx);
        // log("----------------------\ntask-Element["+taskIdx+"]="+task);
        taskId = resourceAllocation.getAttributeNode("task-id").getValue(); 
        resourceId = resourceAllocation.getAttributeNode("resource-id").getValue(); 
        responsible = resourceAllocation.getAttributeNode("responsible").getValue();
        function = resourceAllocation.getAttributeNode("function").getValue();
        ResourceAllocation resourceAllocationObject = new ResourceAllocation(taskId, resourceId, responsible, function);
        resourceAllocationSet = (HashSet<ResourceAllocation>) fResourceAllocation.get(resourceId);
        if (resourceAllocationSet == null) {
          resourceAllocationSet = new HashSet<ResourceAllocation>();
          resourceAllocationSet.add(resourceAllocationObject);
          fResourceAllocation.put(resourceId, resourceAllocationSet);
        } else {
          resourceAllocationSet.add(resourceAllocationObject);
        }
        
        // add user also to task-object.fResources
        taskObject = (Task) fActivitiesAndMilestones.get(taskId);
        if (taskObject == null) {
          if (fPhases.get(taskId) != null) {
            log("ignore resource allocation for phase "+taskId);
            continue;
          } else {
            log("task id=\""+taskId+"\" occurs in allocations, but not found in GanttDiagram!");
            continue;
          }
        }
        taskObject.addUser(resourceAllocationObject.fResourceId, resourceAllocationObject.isResponsible(), resourceAllocationObject.getFunction());
      }
      
      // Logging:
      logResourceAllocation();

    } catch (Exception e) {
      log("ERROR: Error loading ALLOCATIONS from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }
  
  
  
  /**
   * Load Properties of Tasks into fTaskProperty-Hashtable (format: <TaskPropertyID> | <Hashtable of TaskProperty-Objects>)
   * <br><br>
   * e.g.<br>
   * &lt;taskproperties&gt;<br>
   * &nbsp;&lt;taskproperty id="tpd0" name="type" type="default" valuetype="icon"/&gt;<br>
   * &nbsp;&lt;taskproperty id="tpd1" name="priority" type="default" valuetype="icon"/&gt;<br>
   * &nbsp;&lt;taskproperty id="tpd2" name="info" type="default" valuetype="icon"/&gt;<br>
   * &nbsp;...<br>
   * &nbsp;&lt;taskproperty id="tpd9" name="predecessorsr" type="default" valuetype="text"/&gt;<br>
   * &lt;/taskproperties&gt;
   *  
   * @throws Exception
   * @author FBI
   */
  private void loadTaskProperties() throws Exception
  {
    try {
      
      nodelist = fRoot.getElementsByTagName("taskproperties");
      Element taskproperties = null;
      for (int i=0; i<nodelist.getLength(); i++) {
    	  taskproperties = (Element) nodelist.item(i);
      }
      if (taskproperties == null) return;

      nodelist = taskproperties.getElementsByTagName("taskproperty");

      TaskProperty TaskPropertyObject; 
      Element taskproperty;
      String taskpropertyId;
      int noTaskProperties = nodelist.getLength();
      for (int idx=0; idx<noTaskProperties; idx++) {
        taskproperty = (Element) nodelist.item(idx);
        taskpropertyId = taskproperty.getAttributeNode("id").getValue(); 
        TaskPropertyObject = new TaskProperty(taskpropertyId, taskproperty.getAttributeNode("name").getValue(),taskproperty.getAttributeNode("type").getValue(),taskproperty.getAttributeNode("valuetype").getValue());
        fTaskProperty.put(taskpropertyId, TaskPropertyObject);
      }
      
      // Logging:
      logTaskProperties();
      
    } catch (Exception e) {
      log("ERROR: Error loading TaskProperties from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }
  
  
  
  /**
   * Load userdefined roles into fRole-Hashtable (format: <RoleID> | <HashSet of Role-Objects>)
   * <br><br>
   * e.g.<br>
   * &lt;project&gt<br>
   * ...<br>
   * &lt;roles roleset-name="Default"/&gt<br>
   * &nbsp;&nbsp   &lt;roles roleset-name="SoftwareDevelopment"/&gt<br>
   * &nbsp;&nbsp   &lt;roles&gt<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;     &lt;role id="0" name="Coach"/&gt<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;     &lt;role id="1" name="Driver"/&gt<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;     &lt;role id="2" name="Team"/&gt<br>
   * &nbsp;&nbsp   &lt;/roles&gt<br>
   * &lt;/project&gt
   *  
   * @throws Exception
   */
  private void loadRoles() throws Exception
  {
    try {
      
      nodelist = fRoot.getElementsByTagName("roles");
      Element roles = null;
      for (int i=0; i<nodelist.getLength(); i++) {
        roles = (Element) nodelist.item(i);
        if (roles.getAttributeNode("roleset-name") == null) {
          // read that roles-tag without attribute 'roleset-name'
          break;
        } else {
          // ignore those roles-tags with attribute 'roleset-name'
          log("\nignore <roles>-tag with attribute 'roleset-name'="+roles.getAttributeNode("roleset-name").getValue());
        }
      }
      if (roles == null) return;

      nodelist = roles.getElementsByTagName("role");

      Role roleObject; 
      Element role;
      String roleId;
      int noRoles = nodelist.getLength();
      for (int idx=0; idx<noRoles; idx++) {
        role = (Element) nodelist.item(idx);
        roleId = role.getAttributeNode("id").getValue(); 
        roleObject = new Role(roleId, role.getAttributeNode("name").getValue());
        fRole.put(roleId, roleObject);
      }
      
      // Logging:
      logRoles();
      
    } catch (Exception e) {
      log("ERROR: Error loading ROLES from GanttDiagram-xml-file: "+e);
      e.printStackTrace();
      throw e;
    }
  }
  
  public void addRole(String aRoleId, String aRoleName) {
    Role roleObject = new Role(aRoleId, aRoleName);
    fRole.put(aRoleId, roleObject);
  }


  /**
   * get responsible user for task (use GanttDiagram.fResourceAllocation)
   * <br><br>
   * Requirements:<br>
   * <br>- Obwohl man in Gantt einem Task (Activity) mehrere Resourcen zuordnen kann, wird hier nur ein User (Resource) pro Task/Activity unterstuetzt!
   * <br>- Wenn es nur eine Resource gibt --> dann den User nehmen
   * <br>- wenn es mehrere Resourcen gibt, dann den mit IsResponsible=true
   * <br>- Wenn keiner den verantwortlich-Haken gesetzt hat, dann die erste Resource nehmen
   *  
   * @param taskObject
   * @return
   */
  public Resource getResponsibleResourceForTask(Task taskObject) {
    Iterator<ResourceAllocation> it2 = ((HashSet<ResourceAllocation>) taskObject.getResourceHash()).iterator();
    ResourceAllocation tmpResourceAllocation; 
    String responsibleResourceId = null;
    while (it2.hasNext()) {
      tmpResourceAllocation = (ResourceAllocation) it2.next();
      if (responsibleResourceId == null)
        responsibleResourceId = tmpResourceAllocation.getResourceId();
      if (tmpResourceAllocation.isResponsible()) {
        responsibleResourceId = tmpResourceAllocation.getResourceId();
      }
    }
    if (responsibleResourceId != null) {
      Resource responsibleResource = (Resource) getResources().get(responsibleResourceId);
      return responsibleResource;
    }
    return null;
  }

  public String getResourceId(String aResourceShortname) {
    Iterator<String> it = getResources().keySet().iterator();  // <ResourceID> | <Resource-Object>
    Resource resourceObject;
    String resourceId;
    while (it.hasNext()) {
      resourceId = (String) it.next();
      resourceObject = (Resource) getResources().get(resourceId);
      if (resourceObject.getName().equals(aResourceShortname))
        return resourceId;
    }
    return null;
  }
  
  /**
   * get the ID of a Task by its Name
   * 
   * @param aTaskName
   * @return taskId
   * @author FBI
   */
  public String getTaskId(String aTaskName){
	Iterator<String> it = getActivitiesAndMilestones().keySet().iterator();
	Iterator<String> it2 = getPhases().keySet().iterator();
	Task taskObject;
	String taskId;
	while (it.hasNext()){
		taskId = (String) it.next();
		taskObject = (Task) getActivitiesAndMilestones().get(taskId);
		if (taskObject.getName().equals(aTaskName))
			return taskId;
	}
	while (it2.hasNext()){
		taskId = (String) it2.next();
		taskObject = (Task) getPhases().get(taskId);
		if (taskObject.getName().equals(aTaskName))
			return taskId;
	}
	return null;
  }
  
  /**
   * get the Name of a Task with ID=?
   * 
   * @param aTaskId
   * @return taskName
   * @author FBI
   */
  public String getTaskName(String aTaskId){
	  String taskName=getTaskById(aTaskId).getName();
	  return taskName;
  }
  
  /**
   * get the Meeting status of a Task with ID=?
   * 
   * @param aTaskId
   * @return taskMeeting
   * @author FBI
   */
  public String getTaskMeeting(String aTaskId){
	  String taskMeeting=getTaskElementById(aTaskId).getAttributeNode("meeting").getValue();
	  return taskMeeting;
  }
  
  /**
   * get the Type of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskName
   * @author FBI
   */
  public String getTaskType(String aTaskIdorName){
	  		String taskType = getTaskByIdOrName(aTaskIdorName).getType();
			return taskType;
  }
  
  /**
   * get the StartDate of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskStartDate
   * @author FBI
   */
  public String getTaskStartDate(String aTaskIdorName){
			String taskStartDate = getTaskByIdOrName(aTaskIdorName).getStartDate().toString();
			return taskStartDate;
  }
  
  /**
   * get the EndDate of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskEndDate
   * @author FBI
   */
  public String getTaskEndDate(String aTaskIdorName){
			String taskEndDate = getTaskByIdOrName(aTaskIdorName).getEndDate().toString();
			return taskEndDate;
  }
  
  /**
   * get the Duration of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskDuration
   * @author FBI
   */
  public int getTaskDuration(String aTaskIdorName){
			int taskDuration = getTaskByIdOrName(aTaskIdorName).getDuration();
			return taskDuration;
  }
  
  /**
   * get the Task-Complete-Level of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskCompleteLevel
   * @author FBI
   */
  public int getTaskCompleteLevel(String aTaskIdorName){
			int taskCompleteLevel = getTaskByIdOrName(aTaskIdorName).getCompleteLevel();
			return taskCompleteLevel;
  }
  
  /**
   * get the Priority of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskPriority
   * @author FBI
   */
  public String getTaskPriority(String aTaskIdorName){
			String taskPriority = getTaskByIdOrName(aTaskIdorName).getPriority();
			return taskPriority;
  }
  /**
   * get the Note of a Task with ID or Name
   * 
   * @param aTaskId or aTaskName
   * @return taskNote
   * @author FBI
   */
  public String getTaskNote(String aTaskIdorName){
			String taskNote = getTaskByIdOrName(aTaskIdorName).getNote();
			return taskNote;
  }

  /**
   * get the Name of a Resource with ID
   * 
   * 
   * @param aResourceId
   * @return resourceName
   * @author FBI
   */
  public String getResourceName(String aResourceId){
			String resourceName = getResourceObjectById(aResourceId).getName();
			return resourceName;
	}
  

  /**
   * get the Function of a Resource with ID
   * 
   * @param aResourceId
   * @return resourceFunktion
   * @author FBI
   */
  public String getResourceFunction(String aResourceId){

			String resourceFunction = getResourceObjectById(aResourceId).getFunction();
			return resourceFunction;

  }

  /**
   * get the Functioncode of a Resource with ID
   * 
   * @param aResourceId
   * @return resourceFunktionCode
   * @author FBI
   */
  public String getResourceFunctionCode(String aResourceId){
	  
	  String resourceFunction = getResourceObjectById(aResourceId).getFunctionCode();
	  return resourceFunction;
	  
  }
  
  
  /**
   * get the contacts of a Resource with ID
   * 
   * @param aResourceId
   * @return resourceContacts
   * @author FBI
   */
  public String getResourceContacts(String aResourceId){

			String resourceContacts = getResourceElementById(aResourceId).getAttributeNode("contacts").getValue();
			return resourceContacts;

  }

  /**
   * get the ID of a TaskProperty by Name 
   * 
   * @param aTaskPropertyName
   * @return taskPropertyId
   * @author FBI
   */
  public String getTaskPropertyId(String aTaskPropertyName){
	Iterator<String> it = getTaskProperty().keySet().iterator();
	TaskProperty propertyObject;
	String propertyId;
	while (it.hasNext()){
		propertyId = (String) it.next();
		propertyObject = (TaskProperty) getTaskProperty().get(propertyId);
		if (propertyObject.getName().equals(aTaskPropertyName))
			return propertyId;
	}
	return null;
  }

  /**
   * get the Name of a TaskProperty with ID
   * 
   * @param aTaskPropertyId
   * @return taskPropertyName
   * @author FBI
   */
  public String getTaskPropertyName(String aTaskPropertyId){
	  
	  String taskPropertyName = getTaskPropertyObjectById(aTaskPropertyId).getName();
	  return taskPropertyName;
	  
  }
  
  /**
   * get the Type of a TaskProperty with ID
   * 
   * @param aTaskPropertyId
   * @return taskPropertyType
   * @author FBI
   */
  public String getTaskPropertyType(String aTaskPropertyId){
	  String taskPropertyType = getTaskPropertyObjectById(aTaskPropertyId).getType();
	  return taskPropertyType;  
  }
  
  /**
   * get the valuetype of a TaskProperty with ID
   * 
   * @param aTaskPropertyId
   * @return taskPropertyValueType
   * @author FBI
   */
  public String getTaskPropertyValueType(String aTaskPropertyId){
	  String taskPropertyValueType = getTaskPropertyObjectById(aTaskPropertyId).getValuetype();
	  return taskPropertyValueType;  
  }
  
  /**
   * get the TaskId of a ResourceAllocation with ResourceID
   * 
   * 
   * @param aResourceId
   * @return TaskId
   * @author FBI
   */
	public List<String> getResourceAllocationTaskIds(String aResourceId) {
		List<String> result = new ArrayList<String>();
		HashSet<ResourceAllocation> resourceAllocationSet = (HashSet<ResourceAllocation>) fResourceAllocation.get(aResourceId);
		Iterator<ResourceAllocation> it = resourceAllocationSet.iterator();	
		while (it.hasNext()) {
			ResourceAllocation allocation = (ResourceAllocation) it.next();
			result.add(allocation.getTaskId());
//			log(allocation.getTaskId());
		}
		return result;
	}
	
  /**
   * get the phone number of a Resource with ID
   * 
   * 
   * @param aResourceId
   * @return PhoneNumber
   * @author FBI
   */
  public String getResourcePhone(String aResourceId){
			String PhoneNumber = getResourceObjectById(aResourceId).getPhone();
			return PhoneNumber;
  }
	
  
  /**
   * get planned Resources for all tasks where the given resource/teammember is assigned to or responsible for
   * 
   * @param aResourceId
   * @return
   */
  public double getPlannedResourcesForResource(String aResourceId) {
    try {
      log("getPlannedResourceForTeamMember(ResourceId="+aResourceId+")");
      double plannedResourcesForResource = 0;
      Iterator<String> it = fActivitiesAndMilestones.keySet().iterator();
      String key;
      Task taskObject;
      while (it.hasNext()) {
        key = (String) it.next();
        taskObject = (Task) fActivitiesAndMilestones.get(key);
        Resource responsibleResource = getResponsibleResourceForTask(taskObject);
        if (responsibleResource == null) continue;  // task without resource assigned
        if (responsibleResource.fId.equals(aResourceId)) {
          plannedResourcesForResource += (taskObject.getDuration() * gHOURS_PER_WORKDAY);
        }
      }
      Resource resourceObject = (Resource) fResources.get(aResourceId);
      log(resourceObject.fName+" [ID="+resourceObject.fId+"]: PlannedResources="+plannedResourcesForResource);
      return plannedResourcesForResource;
    } catch (Exception e) {
      log("ERROR: Error calculating PlannedResourced for a single Resource (TeamMember)!"+e);
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Write the project name into the gantt diagram (if changed)
   * @return true, if name was changed 
   */
  public boolean modifyDiagram_setProjectName(String aProjectName) {
    fName = fRoot.getAttributeNode("name").getValue(); 
    log("name in .gan-file    : "+fName);
    log("name in project : "+aProjectName);
    if (!fName.equals(aProjectName)) {
      fRoot.getAttributeNode("name").setValue(aProjectName);
      log("ProjectName in GanttDiagram changed from '"+fName+"' to '"+aProjectName+"'.");
      fName = aProjectName;
      return true;
    }else log("no change on name!");
    return false;
  }
  
  /**
   * Write the project Version into the gantt diagram (if changed)
   * @return true, if name was changed 
   */
  public boolean modifyDiagram_setVersion(String aVersionNr) {
    fVersionNo = fRoot.getAttributeNode("version").getValue(); 
    log("version in .gan-file    : "+fVersionNo);
    log("version in project : "+aVersionNr);
    if (!fVersionNo.equals(aVersionNr)) {
      fRoot.getAttributeNode("version").setValue(aVersionNr);
      log("Version in GanttDiagram changed from '"+fVersionNo+"' to '"+aVersionNr+"'.");
      fVersionNo = aVersionNr;
      return true;
    }else log("no change on version!");
    return false;
  }
  
  /**
   * Write the project company into the gantt diagram (if changed)
   * @return true, if name was changed 
   */
  public boolean modifyDiagram_setCompany(String aCompany) {
	  fCompany = fRoot.getAttributeNode("company").getValue(); 
    log("company in .gan-file    : "+fCompany);
    log("company in project : "+aCompany);
    if (!fCompany.equals(aCompany)) {
      fRoot.getAttributeNode("company").setValue(aCompany);
      log("Version in GanttDiagram changed from '"+fCompany+"' to '"+aCompany+"'.");
      fCompany = aCompany;
      return true;
    }else log("no change on company!");
    return false;
  }
  
  /**
   * Write the project webLink into the gantt diagram (if changed)
   * @return true, if name was changed 
   */
  public boolean modifyDiagram_setWebLink(String aWebLink) {
	  fWeblink = fRoot.getAttributeNode("webLink").getValue(); 
    log("webLink in .gan-file    : "+fWeblink);
    log("webLink in project : "+aWebLink);
    if (!fWeblink.equals(aWebLink) && fWeblink.contains("http://")) {
      fRoot.getAttributeNode("webLink").setValue(aWebLink);
      log("Version in GanttDiagram changed from '"+fWeblink+"' to '"+aWebLink+"'.");
      fWeblink = aWebLink;
      return true;
    }else log("no change on webLink! Check that it contains \"http://\"");
    return false;
  }
  
  /**
   * Write the project view-index into the gantt diagram (if changed)
   * @return true, if name was changed 
   */
  public boolean modifyDiagram_setViewIndex(String aViewIndex) {
	  fViewIndex = fRoot.getAttributeNode("view-index").getValue(); 
    log("webLink in .gan-file    : "+fViewIndex);
    log("webLink in project : "+aViewIndex);
    if (!fViewIndex.equals(aViewIndex)) {
      fRoot.getAttributeNode("view-index").setValue(aViewIndex);
      log("Version in GanttDiagram changed from '"+fViewIndex+"' to '"+aViewIndex+"'.");
      fViewIndex = aViewIndex;
      return true;
    }else log("no change on view-index!");
    return false;
  }
  
  
	/**
	 * Change the "Description"-Tag
	 * 
	 * @param aNewDescription
	 * @author FBI
	 */
	public void modifyDiagram_setDescriptionNew(String aNewDescription) {
		log("description in .gan-file    : " + fDescription);
		log("description in project : " + aNewDescription);
		// replace description tag in gantt diagram:
		if (!fDescription.equals(aNewDescription)){
		NodeList nodelist = getDom().getDocumentElement().getElementsByTagName("description");
		Element descriptionTag = (Element) nodelist.item(0);
		if (descriptionTag != null) {
			getDom().getDocumentElement().removeChild(descriptionTag);
		}
		Element notes = getDom().createElement("description");
		CDATASection noteData = getDom().createCDATASection(aNewDescription); // set
																				// text:
																				// aNewDescription
		notes.appendChild(noteData);
		getDom().getDocumentElement().appendChild(notes);

		log("description in GanttDiagram changed  from '" + fDescription
				+ "' to '" + aNewDescription + "'.");
		}
		else log("no change on description!");
	}

  /**
   * Change the "Project"-Tag, Attribute "ViewDate" to todays date
   * @return true, if date was changed 
   */
  public boolean modifyDiagram_setViewDateToToday() {
    // <project name="MH14" ... view-date="2008-11-17" ...>
    String viewDate = fRoot.getAttributeNode("view-date").getValue();
    java.util.Date today = new java.util.Date();
    String dateToday = gDATEFORMAT_YYYY_MM_DD.format(today);
    log("viewDate in .gan-file    : "+viewDate);
    log("date today               : "+dateToday);
    if (!viewDate.equals(dateToday)) {
      fRoot.getAttributeNode("view-date").setValue(dateToday);
      log("view-date in GanttDiagram changed from '"+viewDate+"' to '"+dateToday+"'.");
      return true;
    }
    return false;
  }
  
  /**
   * write gantt-diagram xml-document (fDom) into xml-file on filesystem
   */
  public void writeGanttDiagram() {
    FileOutputStream os=null;
    try {
      String filename = fXMLFile;

      // For Testing purpose: for all files except MasterGantt: add ".new" to filename 
      // filename = fXMLFile.substring(0, fXMLFile.length()-4) + "_NEW.gan";
      
      log("writing gantt diagram to "+filename);
      os = new FileOutputStream(filename);
      XMLHelper.writeXmlFile(os, fDom);
    } catch (Exception e) {
      log("ERROR: Error writing GanttDiagram to filesystem: "+e);
      e.printStackTrace();
    } finally {
      try {
        os.close();
        os=null;
      } catch (IOException e) {
        log("ERROR: Error closing GanttDiagram-file: "+e);
        e.printStackTrace();
      }
    }
  }
  

  /**
   * Add resource to gantt-diagram.<br><br>
   * 
   * e.g.:<br>
   * <br>
   *   &lt resources &gt <br>
   *     &lt resource id="0" name="JPA" function="Default:0" contacts="" phone=""/ &gt <br>
   *     ...<br>
   *   &lt /resources &gt
   *   
   */
  public void modifyDiagram_addResource(String aResourceId, String aResourceName, String aFunction) {
    log("modifyDiagram_addResource(resourceId="+aResourceId+", Name="+aResourceName+", Function="+aFunction);

    // create new resource-Element
    Element newResource = fDom.createElement("resource");
    // set variables
    newResource.setAttribute("id", aResourceId);
    newResource.setAttribute("name", aResourceName);
    newResource.setAttribute("function", getFunctionNameForRole(aFunction));
    // set constants
    newResource.setAttribute("contacts", "");
    newResource.setAttribute("phone", "");

    NodeList nl_res = fRoot.getElementsByTagName("resources");
    nl_res.item(0).appendChild(newResource);

  }
  
  /**
   * Add task to gantt-diagram.<br>
   *<br> - Case A: If aParentTaskId is null, then append task at rear 
   *<br> - Case B: If aParentTaskId is NOT null and if aNextSiblingTaskId is null, then append this task at the end of the parent node.
   *<br> - Case C: If aParentTaskId is NOT null and if aNextSiblingTaskId is NOT null, then insert this task into the parent node, before the next sibling (following) node. If the next sibling node cannot be found, then goto Case B!. 
   */
  public void modifyDiagram_addTask(String aTaskId, String aParentTaskId, String aNextSiblingTaskId, 
      String aTaskName, int aCompleteLevel, java.util.Date aDateEnd, TaskKind taskKind, String aPriority, 
      java.util.Date aDateStart, String aNotes, ArrayList<Depend> depends) {
    log("modifyDiagram_addTask(taskId="+aTaskId+", ParentTaskId="+aParentTaskId+", NextSiblingTaskId="+aNextSiblingTaskId);
    log("data: Name="+aTaskName+", CompleteLevel="+aCompleteLevel+", EndDate="+aDateEnd+", IsMilestone="+(taskKind == TaskKind.MILESTONE)+", Priority="+aPriority+", DateStart="+aDateStart);
    if (aNotes != null)
      log("add notes-tag: "+aNotes);
    
    // create new task-Element
    Element newTask = fDom.createElement("task");
    // set variables
    newTask.setAttribute("id", aTaskId);
    newTask.setAttribute("name", aTaskName);
    newTask.setAttribute("complete", String.valueOf(aCompleteLevel));
    newTask.setAttribute("webLink", "http://jpeaws482.apo.epson.net/redmine2/sot/issues/" + aTaskId + ".xml");
    
    int aDuration = 0;
    if (aDateEnd != null && aDateStart != null) {
    	aDuration = (int)( (aDateEnd.getTime() - aDateStart.getTime()) / GanttDiagram.gMILLISPERDAY + 1 );
    	
    	if (aDuration > 0) {
    		java.util.Date curDate = aDateStart;
    		while (curDate.compareTo(aDateEnd) <= 0 && aDuration > 0) {
    			String curDateType = fCalendars.get(gDATEFORMAT_YYYY_MM_DD.format(curDate));
    			if (curDateType != null && curDateType.equals("HOLIDAY") || 
    				curDate.getDay() == 0 ||	// Sunday
    				curDate.getDay() == 6		// Saturday
    			) {
    				aDuration -= 1;
    			}
    			curDate = new java.util.Date(curDate.getTime() + GanttDiagram.gMILLISPERDAY);
    		}
    	} else {
    		aDuration = 0;
    	}
    }    
    newTask.setAttribute("duration", String.valueOf(aDuration));
    newTask.setAttribute("meeting", ((taskKind == TaskKind.MILESTONE)?"true":"false"));
    if(taskKind == TaskKind.PROJECT) {
    	newTask.setAttribute("project", "true");
    }
    newTask.setAttribute("priority", aPriority);
    newTask.setAttribute("start", gDATEFORMAT_YYYY_MM_DD.format(aDateStart));
    // set constants
    newTask.setAttribute("color", "#8cb6ce");
    newTask.setAttribute("expand", "true");

    if (aNotes != null) {
      // e.g. <notes><![CDATA[Moin auch 12.02.09 - 00:00:00 ]]></notes>
      
      Element notes = fDom.createElement("notes");
      CDATASection noteData = fDom.createCDATASection(aNotes);
      notes.appendChild(noteData);
      newTask.appendChild(notes);
      // log("");
    }
    if (depends != null) {
    	for (Depend dependObject: depends) {
	        // e.g. <notes><![CDATA[Moin auch 12.02.09 - 00:00:00 ]]></notes>
	        
	        Element depend = fDom.createElement("depend");
	        newTask.appendChild(depend);
	        depend.setAttribute("id", dependObject.getId());
	        depend.setAttribute("type", "2");
	        depend.setAttribute("difference", dependObject.getfDifference());
	        depend.setAttribute("hardness", "Strong");
	        // log("");
    	}
      }
      
    if (aParentTaskId == null) {
      NodeList nl_tasks = fRoot.getElementsByTagName("tasks");
      nl_tasks.item(0).appendChild(newTask);
      return;
    }
    if (aNextSiblingTaskId != null) {
      // the following task (sibling node) is known! So try to insert this task before that node in the same parent
      NodeList nl_tasks = fRoot.getElementsByTagName("tasks");
      NodeList task_nodelist = ((Element) nl_tasks.item(0)).getElementsByTagName("task"); // goes deep into XML-Tags
      int noTasks = task_nodelist.getLength();
      boolean nextSiblingFound = false;
      // search following task (next sibling)
      for (int taskIdx=0; taskIdx<noTasks; taskIdx++) {
        Element taskFollowing = (Element) task_nodelist.item(taskIdx);
        if (taskFollowing.getAttribute("id").equals(aNextSiblingTaskId)) {
          Node nodeParent = taskFollowing.getParentNode();
          nodeParent.insertBefore(newTask, taskFollowing);
          nextSiblingFound = true;
          break;
        }
      }
      if (nextSiblingFound == false) {
        aNextSiblingTaskId = null;
        log("Next Sibling with Task-id='"+aNextSiblingTaskId+"' was NOT FOUND!");
      }
    }
    
    if (aNextSiblingTaskId == null) {
      // the following task (sibling node) is unknown! So append this task at the end of the parent node
      NodeList nl_tasks = fRoot.getElementsByTagName("tasks");
      NodeList task_nodelist = ((Element) nl_tasks.item(0)).getElementsByTagName("task"); // goes deep into XML-Tags
      int noTasks = task_nodelist.getLength();
      // search parent task
      for (int taskIdx=0; taskIdx<noTasks; taskIdx++) {
        Element parentElement = (Element) task_nodelist.item(taskIdx);
        log("parentElement="+dumpNode(parentElement));
        if (parentElement.getAttribute("id").equals(aParentTaskId)) {
          Node parentNode = (Node) task_nodelist.item(taskIdx);
          log("parentNode="+dumpNode(parentNode));
          parentNode.appendChild(newTask);
          break;
        }
      }
    }
  }
  
  
  
  private String dumpNode(Node aNode) {
    String attributes = "";
    if (aNode.getAttributes() != null && aNode.getAttributes().getLength() > 0)
      attributes = ""+aNode.getAttributes().item(0).getNodeValue();
    return "Type="+aNode.getNodeType()+", Name="+aNode.getNodeName()+", Value="+aNode.getNodeValue()+", Attributes="+attributes+", child="+aNode.getFirstChild();
  }

  /**
   * The Driver and Coach of the project should be inserted into the gantt diagram when creating the gantt diagram from the projectType-gant-template.
   * Replace Resources 'DDD' and 'CCC' with user shortnames of the driver and the coach. 
   */
  public void modifyDiagram_setDriverAndCoach(String aDriverShortname, String aCoachShortname) {
    nodelist = fRoot.getElementsByTagName("resources");
    Element resources = (Element) nodelist.item(0);
    nodelist = resources.getElementsByTagName("resource"); 
    Element resource;
    String name;
    int noResources = nodelist.getLength();
    for (int idx=0; idx<noResources; idx++) {
      resource = (Element) nodelist.item(idx);
      // log("----------------------\nresource-Element["+idx+"]="+resource);
      name = resource.getAttributeNode("name").getValue();
      if (gTEMPLATE_STRING_DRIVER.equals(name)) {
        if (aDriverShortname != null) {
          resource.getAttributeNode("name").setValue(aDriverShortname);
          log(" set driver in gantt-diagram-template from '"+gTEMPLATE_STRING_DRIVER+"' to '"+aDriverShortname+"'.");
        } else {
          log(" cannot set driver in gantt-diagram-template, because driver is not specified in project!");
        }
      }
      if (gTEMPLATE_STRING_COACH.equals(name)) {
        if (aCoachShortname != null) {
          resource.getAttributeNode("name").setValue(aCoachShortname);
          log(" set coach in gantt-diagram-template from '"+gTEMPLATE_STRING_COACH+"' to '"+aCoachShortname+"'.");
        } else {
          log(" cannot set coach in gantt-diagram-template, because coach is not yet specified in project!");
        }
      }
    }
  }
  
  
  /**
   * set StartDate of a task in the gantt-diagram
   * 
   * @param aTask
   * @param aStartDate
   */
  public void modifyDiagram_setTaskStartDate(String aTaskId, java.util.Date aStartDate) {
	  if (aStartDate == null) return;
	  String startDateStr = gDATEFORMAT_YYYY_MM_DD.format(aStartDate);
	  String oldStartDate = getTaskElementById(aTaskId).getAttributeNode("start").getValue();
	  getTaskElementById(aTaskId).getAttributeNode("start").setValue(startDateStr);
	  log(" set start-date in task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldStartDate+"' to '"+startDateStr+"'.");
  }
    
  /**
   * set duration of a task in the gantt-diagram
   * 
   * @param aTask
   * @param aDuration
   */
  public void modifyDiagram_setTaskDuration(String aTaskId, String aDuration) {
	String oldDuration = getTaskElementById(aTaskId).getAttributeNode("duration").getValue(); 
    getTaskElementById(aTaskId).getAttributeNode("duration").setValue(aDuration);
    log(" set name of task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldDuration+"' to '"+aDuration+"'.");
  }
    
  
  /**
   * set name of a task in the gantt-diagram
   * 
   * @param aTaskId
   * @param aNewName
   * @author FBI
   */
  public void modifyDiagram_setTaskName(String aTaskId, String aNewName) {
	String oldName = getTaskElementById(aTaskId).getAttributeNode("name").getValue(); 
    getTaskElementById(aTaskId).getAttributeNode("name").setValue(aNewName);
    log(" set name of task '"+oldName+"' (id="+aTaskId+") to '"+aNewName+"'.");
  }
  
  /**
   * set meeting of a task in the gantt-diagram
   * 
   * @param aTaskId
   * @param isMeeting true or false
   * @author FBI
   */
  public void modifyDiagram_setTaskMeeting(String aTaskId, String isMeeting) {
	  if (isMeeting.equals("true") || isMeeting.equals("false")) {  
        String oldMeeting = getTaskElementById(aTaskId).getAttributeNode("meeting").getValue(); 
        getTaskElementById(aTaskId).getAttributeNode("meeting").setValue(isMeeting);
        log(" set Meeting of task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldMeeting+"' to '"+isMeeting+"'.");
      }
  }
  
  /**
   * set expand of a task in the gantt-diagram
   * 
   * @param aTaskId
   * @param isExpand true or false
   * @author FBI
   */
  public void modifyDiagram_setTaskExpand(String aTaskId, String isExpand) {
	  if (isExpand.equals("true") || isExpand.equals("false")) {  
        String oldExpand = getTaskElementById(aTaskId).getAttributeNode("expand").getValue(); 
        getTaskElementById(aTaskId).getAttributeNode("expand").setValue(isExpand);
        log(" set expand of task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldExpand+"' to '"+isExpand+"'.");
      }
  }
  
  /**
   * set priority of a task in the gantt-diagram
   * <br><br>
   *  aNewPriority can be:<br>
   * <li>"0" = Low<br></li>
   * <li>"1" = Normal<br></li>
   * <li>"2" = High<br><br></li>
   * 
   * @param aTaskId
   * @param aNewPriority "0" or "1" or "2"
   * @author FBI
   */
  public void modifyDiagram_setTaskPriority(String aTaskId, String aNewPriority) {
	  if (aNewPriority.equals("0") || aNewPriority.equals("1") || aNewPriority.equals("2")) {  
        String oldPriority = getTaskElementById(aTaskId).getAttributeNode("priority").getValue(); 
        getTaskElementById(aTaskId).getAttributeNode("priority").setValue(aNewPriority);
        log(" set priority of task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldPriority+"' to '"+aNewPriority+"'.");
      }
  }
  
  /**
   * set Name of an Taskproperty new
   * 
   * @param aTaskPropertyId
   * @param aNewName
   */
  public void modifyDiagram_setTaskPropertyName(String aTaskPropertyId, String aNewName){
		String oldName = getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("name").getValue(); 
	    getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("name").setValue(aNewName);
	    log(" set name of taskproperty '"+oldName+"' (id="+aTaskPropertyId+") to '"+aNewName+"'.");
  }
  
  /**
   * set Type of an Taskproperty new
   * 
   * @param aTaskPropertyId
   * @param aNewType
   */
  public void modifyDiagram_setTaskPropertyType(String aTaskPropertyId, String aNewType){
		String oldType = getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("type").getValue(); 
	    getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("type").setValue(aNewType);
	    log(" set type of taskproperty '"+oldType+"' (id="+aTaskPropertyId+") to '"+aNewType+"'.");
  }
  
  /**
   * set ValueType of an Taskproperty new
   * 
   * @param aTaskPropertyId
   * @param aNewValueType
   */
  public void modifyDiagram_setTaskPropertyValueType(String aTaskPropertyId, String aNewValueType){
		String oldValueType = getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("valuetype").getValue(); 
	    getTaskPropertyElementById(aTaskPropertyId).getAttributeNode("valuetype").setValue(aNewValueType);
	    log(" set valuetype of taskproperty '"+oldValueType+"' (id="+aTaskPropertyId+") to '"+aNewValueType+"'.");
  }
  
  //TODO: FBI: modifyDiaram_setTaskColor sinvoll???
  
  /**
   * Add Resource-Allocation for a task in the gantt-diagram
   * 
   * @param aTaskId
   * @param aOldResourceShortname
   * @param aNewResourceShortname
   */
  public void modifyDiagram_addTaskResourceAllocation(String aTaskId, String aResourceShortname, String aFunction) {
    // log("aTaskId="+aTaskId+", aOldResourceShortname="+aOldResourceShortname+", aNewResourceShortname="+aNewResourceShortname);
    if (aTaskId == null || aTaskId.length() <= 0) return;
    if (aResourceShortname == null || aResourceShortname.length() <= 0) return;
    String aResourceId = getResourceId(aResourceShortname);
    if (aResourceId == null || aResourceId.length() <= 0) {
      log("ERROR: unkwown resource: ResourceShortname="+aResourceShortname+" ; ResourceId="+aResourceId);
      return;
    }
    
    // create new allocation-Element
    // <allocation function="Default:0" load="100.0" resource-id="0" responsible="true" task-id="1"/>
    Element newAllocation = fDom.createElement("allocation");
    // set variables
    newAllocation.setAttribute("task-id", aTaskId);
    newAllocation.setAttribute("resource-id", aResourceId);
    newAllocation.setAttribute("function", getFunctionNameForRole(aFunction));  // TODO: get Role from db: OC_ProjectTeam.Role 
    // set constants
    // newAllocation.setAttribute("function", getFunctionNameForRole("Not defined"));  // TODO: get Role from db: OC_ProjectTeam.Role 
    newAllocation.setAttribute("load", "100.0");
    newAllocation.setAttribute("responsible", "true");
    
    NodeList nl_allocations = fRoot.getElementsByTagName("allocations");
    nl_allocations.item(0).appendChild(newAllocation);

    log(" add allocation for task with id="+aTaskId+" and resource with id='"+aResourceId+"'.");
  }
  
  /**
   * set Resource-Allocation of a task in the gantt-diagram
   * 
   * @param aTaskId
   * @param aOldResourceShortname
   * @param aNewResourceShortname
   */
  public void modifyDiagram_setTaskResourceAllocation(String aTaskId, String aOldResourceShortname, String aNewResourceShortname) {
    // log("aTaskId="+aTaskId+", aOldResourceShortname="+aOldResourceShortname+", aNewResourceShortname="+aNewResourceShortname);
    if (aTaskId == null || aTaskId.length() <= 0) return;
    if (aOldResourceShortname == null || aOldResourceShortname.length() <= 0) return;
    if (aNewResourceShortname == null || aNewResourceShortname.length() <= 0) return;
    String aOldResourceId = getResourceId(aOldResourceShortname);
    String aNewResourceId = getResourceId(aNewResourceShortname);
    if (aOldResourceId == null || aOldResourceId.length() <= 0) {
      log("ERROR: unkwown resource: oldResourceShortname="+aOldResourceShortname+" ; aOldResourceId="+aOldResourceId);
      return;
    }
    if (aNewResourceId == null || aNewResourceId.length() <= 0) {
      log("ERROR: unkwown resource: newResourceShortname="+aNewResourceShortname+" ; newResourceId="+aNewResourceId);
      return;
    }
    
    nodelist = fRoot.getElementsByTagName("allocations");
    Element resources = (Element) nodelist.item(0);
    nodelist = resources.getElementsByTagName("allocation"); 
    Element allocation;
    String taskId, resourceId;
    int noAllocations = nodelist.getLength();
    for (int idx=0; idx<noAllocations; idx++) {
      allocation = (Element) nodelist.item(idx);
      // log("----------------------\nallocation-Element["+idx+"]="+allocation.getNodeValue());
      taskId = allocation.getAttributeNode("task-id").getValue();
      resourceId = allocation.getAttributeNode("resource-id").getValue();
      if (aTaskId.equals(taskId) && aOldResourceId.equals(resourceId)) {
        allocation.getAttributeNode("resource-id").setValue(aNewResourceId);
        log(" set resource in task with id="+aTaskId+" from resource with id='"+aOldResourceId+"' to '"+aNewResourceId+"'.");
      }
    }
  }

  /**
   * set Complete-Level of a task in the gantt-diagram
   * <br><br>
   * CompleteLevel can be:
   * <li>"0" 	= Planed</li>
   * <li>"50" 	= In Prograss</li>
   * <li>"100"	= Completed</li>
   * <br>
   * @param aTaskId
   * @param aCompleteLevel "0" or "50" or "100"
   * @author FBI
   */
  public void modifyDiagram_setTaskCompleteLevel(String aTaskId, int aCompleteLevel) {
	  if (aCompleteLevel == 0 || aCompleteLevel == 50 || aCompleteLevel == 100){
	   String oldCompleteLevel = getTaskElementById(aTaskId).getAttributeNode("complete").getValue(); 
	   getTaskElementById(aTaskId).getAttributeNode("complete").setValue(String.valueOf(aCompleteLevel));
       log(" set complete level in task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldCompleteLevel+"' to '"+aCompleteLevel+"'.");
	  }
  }

//  /**
//   * set ID of a task in the gantt-diagram
//   * 
//   * @param aTask
//   * @param aStartDate
//   */
//  public void modifyDiagram_setTaskID(String aTaskId, int aNewId) {
//    String oldId = getTaskElementById(aTaskId).getAttributeNode("id").getValue(); 
//    getTaskElementById(aTaskId).getAttributeNode("id").setValue(String.valueOf(aNewId));
//    log(" set ID in task '"+getTaskElementById(aTaskId).getAttributeNode("name").getValue()+"' (id="+aTaskId+") from '"+oldId+"' to '"+aNewId+"'.");
//  }
   
  /**
   * set name of a Resource in the gantt-diagram
   * 
   * @param aResourceID 
   * @param aNewName
   * @author FBI
   */
  public void modifyDiagram_setResourceName(String aResourceId, String aNewName) {
	String oldName = getResourceElementById(aResourceId).getAttributeNode("name").getValue(); 
    if(oldName.equals(aNewName))
    	log(" No Change on Name!");
    else{
    	getResourceElementById(aResourceId).getAttributeNode("name").setValue(aNewName);
    	log(" set Name from Resource '"+oldName+"' (id="+aResourceId+") to '"+aNewName+"'.");
    }
  }
  
  /**
   * set Function of a Resource in the gantt-diagram<br>
   * <br>
   * aNewFunction can be:
   * <br>
   * <li>"Default:0" = undefined</li>
   * <li>"Default:1" = project manager</li>
   * <li>"0"		 = coach</li>
   * <li>"1"		 = driver</li>
   * 
   * 
   * @param aResourceID 
   * @param aNewFunction Default:0 or Default:1 or 0 or 1
   * @author FBI
   */
  public void modifyDiagram_setResourceFunction(String aResourceId, String aNewFunction) {
	String oldFunction = getResourceElementById(aResourceId).getAttributeNode("function").getValue(); 
    if((!oldFunction.equals(aNewFunction)) && (aNewFunction.equals("Default:0") || aNewFunction.equals("Default:1") || aNewFunction.equals("0") || aNewFunction.equals("1"))){
    	getResourceElementById(aResourceId).getAttributeNode("function").setValue(aNewFunction);
    log(" set Function from Resource '"+getResourceElementById(aResourceId).getAttributeNode("name").getValue()+"' (id="+aResourceId+") from '"+oldFunction+"' to '"+aNewFunction+"'.");
    }	
    else
    	log(" No Change on Function!");
  }
  
  /**
   * set contacts (mail) of a Resource in the gantt-diagram
   * <br><br>
   * aNewContact have to be a valid emailadress! 
   * 
   * @param aResourceID 
   * @param aNewContact
   * @author FBI
   */
  public void modifyDiagram_setResourceContact(String aResourceId, String aNewContact) {

	  String oldContact = getResourceElementById(aResourceId).getAttributeNode("contacts").getValue(); 
      if(!oldContact.equals(aNewContact) && (aNewContact.contains("@"))){
      getResourceElementById(aResourceId).getAttributeNode("contacts").setValue(aNewContact);
      log(" set Contact (Mail) from Resource '"+getResourceElementById(aResourceId).getAttributeNode("name").getValue()+"' (id="+aResourceId+") from '"+oldContact+"' to '"+aNewContact+"'.");
      }	
      else
     	log(" No Change on Contact. Have to be a ne new and valid Emailadress!");
    }
  
  /**
   * set Phonenumber of a Resource in the gantt-diagram
   * 
   * @param aResourceID and aNewPhoneNr
   * @author FBI
   */
  public void modifyDiagram_setResourcePhone(String aResourceId, String aNewPhoneNr) {
	String oldPhone = getResourceElementById(aResourceId).getAttributeNode("phone").getValue(); 
    if(oldPhone.equals(aNewPhoneNr))
    	log(" No Change on Phone!");
    else{
    getResourceElementById(aResourceId).getAttributeNode("phone").setValue(String.valueOf(aNewPhoneNr));
    log(" set Phonenumber in Resource '"+getResourceElementById(aResourceId).getAttributeNode("name").getValue()+"' (id="+aResourceId+") from '"+oldPhone+"' to '"+aNewPhoneNr+"'.");
    }
  }
	  
  
  /**
   * remove Task vom GanttDiagram
   * 
   * removes also Dependency and Allocation
   * 
   * @param aTaskId
   */
  public void modifyDiagram_deleteTask(String aTaskId){
	   
	  
	  //Delete Allocaton if Task is deleted
	  boolean allocationDeleted=false;
	  do{   
	  nodelist = fRoot.getElementsByTagName("allocations");
	  Element allocations = (Element) nodelist.item(0);
	  nodelist = allocations.getElementsByTagName("allocation"); 
	  Element allocation;
	  String alloid;
	  int noAllocations = nodelist.getLength();
	  for (int alloidx=0; alloidx<noAllocations; alloidx++) {
		   allocation = (Element) nodelist.item(alloidx);
		   alloid = allocation.getAttributeNode("task-id").getValue();
		   allocationDeleted=false;
		   if (aTaskId.equals(alloid)){ 
				  allocation.getParentNode().removeChild(allocation);
				  allocationDeleted=true;
				  break;
			  }
		  }   
	  }while(allocationDeleted==true);
		  
	  //Delete Dependency if Task is deleted
	  boolean dependencyDeleted=false;
	  do{  
	    nodelist = fRoot.getElementsByTagName("tasks");
	    Element Task = (Element) nodelist.item(0);
	    nodelist = Task.getElementsByTagName("depend"); 
	    Element task;
	    String id;
	    int noTask = nodelist.getLength();
	    for (int idx=0; idx<noTask; idx++) {
	      task = (Element) nodelist.item(idx);
	      id = task.getAttributeNode("id").getValue();
	      dependencyDeleted=false;
	      if (aTaskId.equals(id)){ 
	    	 task.getParentNode().removeChild(getDependencyElementById(aTaskId));
	    	 dependencyDeleted=true;
	    	 break;
	      }
	    }
	  }while(dependencyDeleted==true);
	  //Delete Task
	  getTaskElementById(aTaskId).getParentNode().removeChild(getTaskElementById(aTaskId));
  }
   
  /**
   * remove Resource vom GanttDiagram
   * 
   * removes also Allocation
   * 
   * @param aResourceId
   */
  public void modifyDiagram_deleteResource(String aResourceId){

	  
	  //Delete Allocaton if Resource is deleted
	  boolean allocationDeleted=false;
	  do{   
	  nodelist = fRoot.getElementsByTagName("allocations");
	  Element allocations = (Element) nodelist.item(0);
	  nodelist = allocations.getElementsByTagName("allocation"); 
	  Element allocation;
	  String alloid;
	  allocationDeleted=false;
	  int noAllocations = nodelist.getLength();
	  for (int alloidx=0; alloidx<noAllocations; alloidx++) {
		   allocation = (Element) nodelist.item(alloidx);
		   alloid = allocation.getAttributeNode("resource-id").getValue();
		   if (aResourceId.equals(alloid)){ 
				  allocation.getParentNode().removeChild(allocation);
				  allocationDeleted=true;
				  break;
			  }
		  }   
	  }while(allocationDeleted==true);

	  
	  //Delete Resource
	  getResourceElementById(aResourceId).getParentNode().removeChild(getResourceElementById(aResourceId));
  }
  
  /**
   * remove TaskProperty vom GanttDiagram
   * 
   * @param aTaskPropertyId
   */
  public void modifyDiagram_deleteTaskProperty(String aTaskPropertyId){
	  getTaskPropertyElementById(aTaskPropertyId).getParentNode().removeChild(getTaskPropertyElementById(aTaskPropertyId));
  
  }
  
  /**
   * remove Dependency vom GanttDiagram
   * 
   * @param aDependId
   */
  public void modifyDiagram_deleteDependency(String aDependId){
	  getDependencyElementById(aDependId).getParentNode().removeChild(getDependencyElementById(aDependId));
	  
  }
  
  /**
   * remove Role vom GanttDiagram
   * 
   * @param aRoleId
   */
  public void modifyDiagram_deleteRole(String aRoleId){  
	  nodelist = fRoot.getElementsByTagName("roles");
	  Element roles = (Element) nodelist.item(0);
	  nodelist = roles.getElementsByTagName("role"); 
	  Element role;
	  String id;
	  int noRoles = nodelist.getLength();
	  for (int idx=0; idx<noRoles; idx++) {
		  role = (Element) nodelist.item(idx);
		  id = role.getAttributeNode("id").getValue();
		  if (aRoleId.equals(id)) 
			  role.getParentNode().removeChild(role);
	  }
  }
  
  
  public Hashtable<String, Role> getRoles() {
    return fRole;
  }
  
  public java.sql.Date getDateProjectStart() {
    return fDateProjectStart;
  }
  
  public java.sql.Date getDateProjectEnd() {
    return fDateProjectEnd;
  }
  
  protected static void log(String aLogMsg) {
    // log(aLogMsg);
    System.out.println(aLogMsg);  // for standalone execution of GanttDiagram.java
  }
  
  public String getName() {
    return fName;
  }
  
  public String toString() {
    return "GanttDiagram (GanttProject): name="+fName+", version="+fVersionNo;
  }

  public Hashtable<String, Task> getActivitiesAndMilestones() {
    return fActivitiesAndMilestones;
  }

  public void setActivitiesAndMilestones(Hashtable<String, Task> activitiesAndMilestones) {
    fActivitiesAndMilestones = activitiesAndMilestones;
  }

  public Hashtable<String, Task> getPhases() {
    return fPhases;
  }

  public void setPhases(Hashtable<String, Task> phases) {
    fPhases = phases;
  }

  public Hashtable<String, Resource> getResources() {
    return fResources;
  }

  public Hashtable<String, String> getCalendars() {
    return fCalendars;
  }
  
  public void setResources(Hashtable<String, Resource> resources) {
    fResources = resources;
  }
  

  public Hashtable<String, HashSet<ResourceAllocation>> getResourceAllocation() {
    return fResourceAllocation;
  }

  public void setResourceAllocation(Hashtable<String, HashSet<ResourceAllocation>> resourceAllocation) {
    fResourceAllocation = resourceAllocation;
  }

  public int getPlannedResources() {
    return fPlannedResources;
  }

  public void setPlannedResources(int plannedResources) {
    fPlannedResources = plannedResources;
  }

  public org.w3c.dom.Document getDom() {
    return fDom;
  }

  public void setDom(org.w3c.dom.Document dom) {
    fDom = dom;
  }

  public void logPhases() {
    log("\nPhases:");
    Iterator<String> it = getPhases().keySet().iterator();
    Task taskObject;
    String key;
    while (it.hasNext()) {
      key = (String) it.next();
      taskObject = (Task) getPhases().get(key);
      log(taskObject.toString());
    }
  }
 
  public void logActivitiesAndMilestones() {
    log("\nActivities & Milestones:");
    Iterator<String> it = getActivitiesAndMilestones().keySet().iterator();
    Task taskObject;
    String key;
    while (it.hasNext()) {
      key = (String) it.next();
      taskObject = (Task) getActivitiesAndMilestones().get(key);
      log(taskObject.toString());
    }
  }
  
  //FBI
  public void logDependencies() {
	log("\nDependencies between Tasks:");
    Iterator<String> it = fDependendcy.keySet().iterator();
    Hashtable<String, Task> Tasks = new Hashtable<String, Task>();
    Tasks.putAll(fActivitiesAndMilestones);
    Tasks.putAll(fPhases);
    Task taskObject;
    String key;
    while (it.hasNext()) {
      key = (String) it.next();
      taskObject = (Task) Tasks.get(key);
      log(taskObject.toString());								//shows Task wich has an dependency to another Task
      Iterator<String> it2 = fDependendcy.keySet().iterator();
      while (it2.hasNext()) {
    	  	String anId = (String) it2.next();
    	    if(getTaskById(anId).getNextSiblingId() == anId){
    	    	ArrayList<Depend> depends = fDependendcy.get(anId);
    	    	for (Depend dependObject: depends) {
    	    	      log("  "+dependObject.toString()); 						//shows Dependency
    	    	      log(getTaskById(dependObject.getId()).toString()+"\n"); 	//shows the other Task	  
    	    	}
    	  	}
      }
    }
  }
  
  // FBI
  public void logTaskProperties() {
    log("\nTask Properties:");
    Iterator<String> it = getTaskProperty().keySet().iterator();
    String TaskPropertyId;
    TaskProperty TaskPropertyObject;
    while (it.hasNext()) {
      TaskPropertyId = (String) it.next();
      TaskPropertyObject = (TaskProperty) getTaskProperty().get(TaskPropertyId);
      log(TaskPropertyObject.toString());
    }
  }
	  
	  
  public void logRoles() {
    log("\nRoles:");
    Iterator<String> it = getRoles().keySet().iterator();
    String roleId;
    Role roleObject;
    while (it.hasNext()) {
      roleId = (String) it.next();
      roleObject = (Role) getRoles().get(roleId);
      log(roleObject.toString());
    }
  }

  public void logResources() {
    log("\nResources:");
    Iterator<String> it = getResources().keySet().iterator();
    Resource resourceObject;
    String key;
    while (it.hasNext()) {
      key = (String) it.next();
      resourceObject = (Resource) getResources().get(key);
      log(resourceObject.toString());
    }
  }
 
  public void logCalendars() {
    log("\nDates:");
    Iterator<String> it = getCalendars().keySet().iterator();
    String key;
    String type;
    while (it.hasNext()) {
      key = (String) it.next();
      type = (String) getCalendars().get(key);
      log(key.toString() + " --> " + type);
    }
  }
  
  public void logResourceAllocation() {
    log("\nResourceAllocation:");
    Iterator<String> it = fResourceAllocation.keySet().iterator();
    ResourceAllocation resourceAllocationObject;
    Resource resourceObject;
    HashSet<ResourceAllocation> resourceAllocationSet;
    String resourceId;
    while (it.hasNext()) {
      resourceId = (String) it.next();
      resourceAllocationSet = (HashSet<ResourceAllocation>) fResourceAllocation.get(resourceId);
      resourceObject = (Resource) fResources.get(resourceId);
      log(resourceObject.toString());
      Iterator<ResourceAllocation> it2 = resourceAllocationSet.iterator();
      while (it2.hasNext()) {
        resourceAllocationObject = (ResourceAllocation) it2.next();
        log("  "+resourceAllocationObject);
      }
    }
  }
  
  
  
	/*
	 * Getters and Setters
	 * 
	 * @author FBI  
	 */
  
  	public String getViewdate() {
		return fViewdate;
	}


	public void setViewdate(String Viewdate) {
		this.fViewdate = Viewdate;
	}


	public String getDescription() {
		return fDescription;
	}


	public void setDescription(String Description) {
		this.fDescription = Description;
	}


	public String getVersionNo() {
		return fVersionNo;
	}


	public void setVersionNo(String VersionNo) {
		this.fVersionNo = VersionNo;
	}

	public Hashtable<String, ArrayList<Depend>> getDependencies() {
		return fDependendcy;
	}
	
	public Hashtable<String, TaskProperty> getTaskProperty() {
	    return fTaskProperty;
}


	public void setWeblink(String weblink) {
		this.fWeblink = weblink;
	}


	public String getWeblink() {
		return fWeblink;
	}


	public void setCompany(String Company) {
		this.fCompany = Company;
	}


	public String getCompany() {
		return fCompany;
	}
	
	public String getViewIndex() {
		return fViewIndex;
	}


	public void setViewIndex(String ViewIndex) {
		this.fViewIndex = ViewIndex;
	}
	

	/**
	 * Get an Taskobject by its ID
	 * 
	 * @param aTaskId
	 * @return taskObject
	 * @author FBI
	 */
	public Task getTaskById(String aTaskId) {
		Iterator<String> it = getActivitiesAndMilestones().keySet().iterator();
		Iterator<String> it2 = getPhases().keySet().iterator();
		Task taskObject;
		String taskId;
		while (it.hasNext()) {
			taskId = (String) it.next();
			taskObject = (Task) getActivitiesAndMilestones().get(taskId);
			if (taskObject.getId().equals(aTaskId))
				return taskObject;
		}
		while (it2.hasNext()) {
			taskId = (String) it2.next();
			taskObject = (Task) getPhases().get(taskId);
			if (taskObject.getId().equals(aTaskId))
				return taskObject;
		}
		return null;
	}

	/**
	 * Get an Taskobject by its ID or Name
	 * 
	 * @param aTaskIdorName
	 * @return taskObject
	 * @author FBI
	 */
	public Task getTaskByIdOrName(String aTaskIdorName) {
		Iterator<String> it = getActivitiesAndMilestones().keySet().iterator();
		Iterator<String> it2 = getPhases().keySet().iterator();
		Task taskObject;
		String taskId;
		while (it.hasNext()) {
			taskId = (String) it.next();
			taskObject = (Task) getActivitiesAndMilestones().get(taskId);
			if (taskObject.getId().equals(aTaskIdorName) || taskObject.getName().equals(aTaskIdorName))
				return taskObject;
		}
		while (it2.hasNext()) {
			taskId = (String) it2.next();
			taskObject = (Task) getPhases().get(taskId);
			if (taskObject.getId().equals(aTaskIdorName) || taskObject.getName().equals(aTaskIdorName))
				return taskObject;
		}
		return null;
	}

	/**
	 * Get an Resourceobject by its ID
	 * 
	 * @param aResourceId
	 * @return ResourceObject
	 * @author FBI
	 */
	public Resource getResourceObjectById(String aResourceId) {
		Iterator<String> it = getResources().keySet().iterator();
		Resource ResourceObject;
		String resourceId;
		while (it.hasNext()) {
			resourceId = (String) it.next();
			ResourceObject = (Resource) getResources().get(resourceId);
			if (ResourceObject.getId().equals(aResourceId))
				return ResourceObject;
		}
		return null;
	}
	
	/**
	 * Get an Resourceelement by its ID
	 * 
	 * @param aResourceId
	 * @return ResourceElement
	 * @author FBI
	 */
	public Element getResourceElementById(String aResourceId){
		if (aResourceId == null || aResourceId.length() <= 0) return null;
	    
	    nodelist = fRoot.getElementsByTagName("resources");
	    Element resources = (Element) nodelist.item(0);
	    nodelist = resources.getElementsByTagName("resource"); 
	    Element resource;
	    String id;
	    int noResource = nodelist.getLength();
	    for (int idx=0; idx<noResource; idx++) {
	      resource = (Element) nodelist.item(idx);
	      id = resource.getAttributeNode("id").getValue();
	      if (aResourceId.equals(id)) 
	    	return resource;
	    }
	    return null;
	}
	
	/**
	 * Get an Taskelement by its ID
	 * 
	 * @param aTaskId
	 * @return taskElement
	 * @author FBI
	 */
	public Element getTaskElementById(String aTaskId){
		if (aTaskId == null || aTaskId.length() <= 0) return null;
	    
	    nodelist = fRoot.getElementsByTagName("tasks");
	    Element Task = (Element) nodelist.item(0);
	    nodelist = Task.getElementsByTagName("task"); 
	    Element task;
	    String id;
	    int noTask = nodelist.getLength();
	    for (int idx=0; idx<noTask; idx++) {
	      task = (Element) nodelist.item(idx);
	      id = task.getAttributeNode("id").getValue();
	      if (aTaskId.equals(id)) 
	    	return task;
	    }
	    return null;
	}

	
	/**
	 * Get an Taskpropertyobject by its ID
	 * 
	 * @param aTaskPropertyId
	 * @return TaskPropertyObject
	 * @author FBI
	 */
	public TaskProperty getTaskPropertyObjectById(String aTaskPropertyId) {
		Iterator<String> it = getTaskProperty().keySet().iterator();
		TaskProperty PropertyObject;
		String PropertyId;
		while (it.hasNext()) {
			PropertyId = (String) it.next();
			PropertyObject = (TaskProperty) getTaskProperty().get(PropertyId);
			if (PropertyObject.getId().equals(aTaskPropertyId))
				return PropertyObject;
		}
		return null;
	}
	
	/**
	 * Get an Taskpropertyelement by its ID
	 * 
	 * @param aTaskPropertyId
	 * @return TaskPropertyElement
	 * @author FBI
	 */
	public Element getTaskPropertyElementById(String aTaskPropertyId){
		if (aTaskPropertyId == null || aTaskPropertyId.length() <= 0) return null;
	    
	    nodelist = fRoot.getElementsByTagName("taskproperties");
	    Element properties = (Element) nodelist.item(0);
	    nodelist = properties.getElementsByTagName("taskproperty"); 
	    Element property;
	    String id;
	    int noResource = nodelist.getLength();
	    for (int idx=0; idx<noResource; idx++) {
	    	property = (Element) nodelist.item(idx);
	      id = property.getAttributeNode("id").getValue();
	      if (aTaskPropertyId.equals(id)) 
	    	return property;
	    }
	    return null;
	}

	/**
	 * Get an dependencyobject by its ID
	 * 
	 * @param aDependId
	 * @return dependObject
	 * @author FBI
	 */
	/*public ArrayList<Depend> getDependencyObjectById(String aDependId) {
		Iterator<String> it = getDependencies().keySet().iterator();
		ArrayList<Depend> dependObjects;
		String dependId;
		while (it.hasNext()) {
			dependId = (String) it.next();
			dependObjects = getDependencies().get(dependId);
			if (dependObjects.getId().equals(aDependId))
				return dependObject;
		}
		return null;
	}
	*/
	
	/**
	 * Get an Dependencyelement by its ID
	 * 
	 * @param aTaskId
	 * @return taskElement
	 * @author FBI
	 */
	public Element getDependencyElementById(String aDependId){
		if (aDependId == null || aDependId.length() <= 0) return null;
	    
	    nodelist = fRoot.getElementsByTagName("tasks");
	    Element Task = (Element) nodelist.item(0);
	    nodelist = Task.getElementsByTagName("depend"); 
	    Element task;
	    String id;
	    int noTask = nodelist.getLength();
	    for (int idx=0; idx<noTask; idx++) {
	      task = (Element) nodelist.item(idx);
	      id = task.getAttributeNode("id").getValue();
	      if (aDependId.equals(id)) 
	    	return task;
	    }
	    return null;
	}
	
}
