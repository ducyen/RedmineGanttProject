/**
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Duc Hoang
 *
 */

import java.text.ParseException;
import java.util.*;

import com.objectcode.GanttProjectAPI.*;
import com.taskadapter.redmineapi.*;
import com.taskadapter.redmineapi.bean.*;

public class Main {
	static Date startDateToCheck = null;
	protected static void log(String aLogMsg) {
		// LOGGER.debug(aLogMsg);
		System.out.println(aLogMsg);  // for standalone execution of GanttDiagram.java
	}
	private static GanttDiagram ganttDiagram; 
	private static RedmineManager mgr;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    // Parse series names
	    String[] machineNames = System.getenv("PROJECTS").split(",");
    	String asigneeName = System.getenv("ASSIGNEE");
    	String userIds = System.getenv("USER_ID");
    	try {
    		startDateToCheck = GanttDiagram.gDATEFORMAT_YYYY_MM_DD.parse(System.getenv("START_DATE"));
    	} catch (ParseException e) {
	    	log("start date to check format not matched");
    	}
	    
//	    String ganttDiagramFile = "/home/objectcode/workspace/GanttProjectAPI/data/Demo1.gan"; 	//Use this or an equal path for Unix-systems
		String ganttDiagramFile = System.getenv("GANTTPROJ_FILE");				//Use this or an equal path for Windows-systems
	    ganttDiagram = new GanttDiagram(ganttDiagramFile);
	    String msg = ganttDiagram.loadGanttDiagram();   
	    log("\nGanttDiagram loaded: "+ganttDiagram+" (Message="+msg+")\n\n");
	    
	// read out Gantt-Diagramm-Data:
	    
	    
	    
	// General-data:  
	    log("\n-----------------General-Data------------------\n");
	    
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
	    log("\n------------------Task-Data---------------------\n");
	    // Connect to Redmine
	    String uri = System.getenv("REDMINE_URL");
	    String apiAccessKey = System.getenv("API_KEY");

	    mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
	    
	    if (args[0].equals("load"))
	    	loadDataFromRedmine(machineNames, startDateToCheck, asigneeName, userIds.split(","));
	    else if (args[0].equals("save"))
	    	saveDataToRedmine(asigneeName);
	    else
	    	log("Parameter not matched");
	    
	    log("\n------------------End-Program---------------------\n");
	}
	
	private static String findReqSymbol(Issue issue) {
		String symbol = "📁 ";
		if (issue.getTracker()!= null && issue.getTracker().getName().compareTo("要件") == 0) {
			symbol = "📄 ";
		}
		return symbol;
	}
	
	private static final String DEV = "Developer";
	private static final String PIC = "Handbook Writer";
	private static final String LEADER = "Packager";
	private static final String MANAGER = "Project Manager";
	private static String findRscByFunction(Task task, String aFunction) {
		Hashtable<String, HashSet<ResourceAllocation>> rscAllocTbl = ganttDiagram.getResourceAllocation();
		if (!PIC.equals(aFunction)) { 
			for (String key: rscAllocTbl.keySet()) {
				HashSet<ResourceAllocation> rscHash = rscAllocTbl.get(key);
				for (ResourceAllocation rscAlloc : rscHash) {	    			
					if (rscAlloc.getTaskId().equals(task.getId()) && ganttDiagram.getRoleNameForFunction(rscAlloc.getFunction()).equalsIgnoreCase(aFunction)) {
						return rscAlloc.getResourceId();
					}
				}
			}
		} else {
			for (String key: rscAllocTbl.keySet()) {
				HashSet<ResourceAllocation> rscHash = rscAllocTbl.get(key);
				for (ResourceAllocation rscAlloc : rscHash) {	    			
					if (rscAlloc.getTaskId().equals(task.getId()) && rscAlloc.isResponsible()) {
						return rscAlloc.getResourceId();
					}
				}
			}			
		}
		return "";
	}
	
	private static Task findTaskMilestone(Task task) {
		// Retrieve depend list
		Map<String, ArrayList<Depend>> dependencies = ganttDiagram.getDependencies();
		ArrayList<Depend> depends = dependencies.get(task.getId());
		if (depends != null) {
			for (Depend dependObj: depends) {
				Task dependTsk = ganttDiagram.getTaskById(dependObj.getId());
				if (dependTsk.isMilestone()) {
					return dependTsk;
				}
			}
		}
		return null;
	}
	
	private static void loadDataFromRedmine(
		String[] machineNames,
		Date startDateToCheck,
		String asigneeNameCsv,
		String[] resourceIdCsv
	) {
	    try {
	    	ProjectManager projMgr = mgr.getProjectManager();
	    	List<Project> projects = new ArrayList<Project>();
	    	UserManager userMgr = mgr.getUserManager();
			String[] asigneeNames = asigneeNameCsv.split(",");
	    	
	    	// Load resources
	    	for (String rscId: resourceIdCsv) {
	    		int userId = Integer.valueOf(rscId);
	    		User user = userMgr.getUserById(userId);
	    		if (ganttDiagram.getResourceObjectById(rscId) != null) {
	    			ganttDiagram.modifyDiagram_deleteResource(rscId);
	    		}
	    		ganttDiagram.modifyDiagram_addResource(rscId, user.getLastName(), DEV);
	    	}

	    	for (String machineName: machineNames) {
	    		projects.add(projMgr.getProjectByKey(machineName));
	    	}
	    	
	    	// Load project
	    	for (Project project: projects) {
	    		List<Version> versions = projMgr.getVersions(project.getId());
			    List<Issue> allIssues = mgr.getIssueManager().getIssues(project.getIdentifier(), null, Include.relations);
			    List<Issue> issues = new ArrayList<Issue>();
			    List<Issue> orgIssues = new ArrayList<Issue>();

			    for (Issue issue: allIssues) {
				    boolean eligibleToLoad = false;
					User assignee = issue.getAssignee();
					for (String asigneeName: asigneeNames) {
						if (assignee != null && assignee.getFullName().indexOf(asigneeName) >= 0) {
							issues.add(issue);
							orgIssues.add(issue);
							break;
						}		    						
					}	    				
			    }			    
			    
				Collections.sort(issues,
					new Comparator<Issue>() {
						public int compare(Issue lhs, Issue rhs) {
							int lhsParentId = lhs.getParentId() != null ? lhs.getParentId() : 0;
							int rhsParentId = rhs.getParentId() != null ? rhs.getParentId() : 0;
							int lhsId = lhs.getId();
							int rhsId = rhs.getId();
							if (lhsParentId == rhsId) {
								return -1;
							}
							if (rhsParentId == lhsId) {
								return 1;
							}							
							return 0;
						}
					}
				);
			    log("Project: " + project + " --> " + issues.size());
			    
			    // task id format (32bit)                .                       .                       .
			    //                31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
			    // project:        1  1 
			    // version:        0  1 
			    // issue:                #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #
			    
			    // Set Redmine project
				String gcProjId = String.valueOf(project.getId() | 0xC0000000);
				Task gcProject = ganttDiagram.getTaskById(gcProjId);
				if (gcProject != null) {
					ganttDiagram.modifyDiagram_deleteTask(gcProjId);
				}
				ganttDiagram.modifyDiagram_addTask(
					gcProjId, 
					null, null, 
					"🖨 " + project.getName(), 			// printer: 🖨； 企画：㊭
					0, 
					project.getCreatedOn(),
					GanttDiagram.TaskKind.PROJECT, 
					"1",
					project.getCreatedOn(),
					project.getDescription(),
					null
				);
				Collections.sort(versions,
					new Comparator<Version>() {
						public int compare(Version v1, Version v2) {
							java.util.Date dueDate1 = v1.getDueDate();
							if (dueDate1 == null) {
								return -1; 
							}
							java.util.Date dueDate2 = v2.getDueDate();
							if (dueDate2 == null) {
								return 1;
							}
							int result = dueDate1.compareTo(dueDate2);
							return result;
						}
					}
				);

	    		for (Version version: versions) {
	    			log("    Version: " + version + " --> " + version.getDueDate());
	    			if (version.getDueDate() == null) {
	    				log("No Due Date --> set to start date");
	    				version.setDueDate(startDateToCheck);
	    			}
	    			if (startDateToCheck.compareTo(version.getDueDate()) > 0) {
	    				continue;
	    			}
	    			if (version.getStatus().compareToIgnoreCase("open") != 0) {
	    				continue;
	    			}
					String milestoneId = String.valueOf(version.getId() | 0x40000000);
	    			for (int i = issues.size() - 1; i >= 0; i--) {
	    				Issue issue = issues.get(i);
	    				Version targetVersion = issue.getTargetVersion();
	    				Project targetProject = issue.getProject();
	    				if (issue.getStartDate() == null) {
	    					issue.setStartDate(startDateToCheck);
	    				}
	    				if (targetVersion != null && targetProject != null && 
		    				(targetVersion.getId().equals(version.getId()) && targetProject.getId().equals(project.getId()))
		    			) {
	    					String taskId = String.valueOf(issue.getId());
	    					// Check relations
	    		    		ArrayList<Depend> depends = new ArrayList<Depend>();
	    		    		for (IssueRelation relation: issue.getRelations()) {
	    		    			if (relation.getType().equals("precedes") &&
	    		    				!relation.getIssueToId().equals(issue.getId())
	    		    			) {
	    		    				log ("Added depencency");
	    		    				Depend dependObj = new Depend(String.valueOf(relation.getIssueToId()));
	    		    				dependObj.setfDifference(String.valueOf(relation.getDelay()));
	    		    				depends.add(dependObj);
	    		    			}
	    		    		}
	    		    		// Set version as milestone
		    				Depend dependObj = new Depend(milestoneId);
		    				dependObj.setHardness("Rubber");
	    		    		depends.add(dependObj);

	    		    		String parentId = gcProjId;
	    		    		if (issue.getParentId() != null) {
	    		    			/* find parent */
	    		    			for (Issue parentIssue: orgIssues){
	    		    				if (parentIssue.getId().equals(issue.getParentId()) &&
	    		    					issue.getTargetVersion().equals(parentIssue.getTargetVersion())) 
	    		    				{
			    		    			parentId = String.valueOf(issue.getParentId());
			    		    			log(taskId + " has parent " + parentId);
	    		    					break;
	    		    				}
	    		    			}
	    		    		}	    		    		

	    					ganttDiagram.modifyDiagram_addTask(
    							taskId, 
    							parentId, null, 
    							findReqSymbol(issue) + " #" + taskId + ": " + issue.getSubject(), 
    							issue.getDoneRatio(), 
    							issue.getDueDate(),
    							GanttDiagram.TaskKind.ACTIVITY, 
    							"1",
    							issue.getStartDate(),
    							issue.getDescription(),
    							depends
	    					);
	    					
	    					// Check if developer is the same as responsibility person
	    					String assigneeId = String.valueOf(issue.getAssignee().getId());
	    					String picId = null;
	    					if (issue.getCustomFieldByName("要件責任者") != null) {
	    						picId = issue.getCustomFieldByName("要件責任者").getValue();
	    					} else if (issue.getCustomFieldByName("要求責任者") != null){
	    						picId = issue.getCustomFieldByName("要求責任者").getValue();
	    					}
	    					String leaderId = null;
	    		    		if (issue.getCustomFieldByName("チームリーダー") != null) {
		    					leaderId = issue.getCustomFieldByName("チームリーダー").getValue();
	    		    		}
	    					String managerId = null;
	    		    		if (issue.getCustomFieldByName("責任課長") != null) {
		    					managerId = issue.getCustomFieldByName("責任課長").getValue();
	    		    		}
	    					
	    					String whoIsPic = null;
	    					if (picId != null) {
	    						if (assigneeId.equals(picId)) {
	    							whoIsPic = DEV;
	    						} else if (leaderId.equals(picId)) {
	    							whoIsPic = LEADER;
	    						} else if (managerId.equals(picId)) {
	    							whoIsPic = MANAGER;
	    						}
	    					}
	    					
	    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, String.valueOf(issue.getAssignee().getId()), DEV, 100, DEV.equals(whoIsPic));
	    					/*
	    		    		if (leaderId != null) {
		    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, leaderId, LEADER, 0, LEADER.equals(whoIsPic));
	    		    		}
	    		    		if (managerId != null) {
		    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, managerId, MANAGER, 0, MANAGER.equals(whoIsPic));
	    		    		}
	    		    		if (picId != null && !DEV.equals(whoIsPic) && !LEADER.equals(whoIsPic) && !MANAGER.equals(whoIsPic)) {
		    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, picId, PIC, 0, true);
    						}
	    		    		*/
	    		    		// Remove the checked item
		    				issues.remove(i);
	    				}
	    			}
					ganttDiagram.modifyDiagram_addTask(
						milestoneId, 
						gcProjId, null, 
						"🏁 " + version.getName(), 
						0, 
						version.getDueDate(),
						GanttDiagram.TaskKind.MILESTONE, 
						"1",
						version.getDueDate(),
						version.getDescription(),
						null
					);   			
					
    			}
	    		// No version items
    			for (int i = issues.size() - 1; i >= 0; i--) {
    				Issue issue = issues.get(i);
    				if (issue.getStartDate() == null) {
    					issue.setStartDate(startDateToCheck);
    				}
 				
    				if (true) {
    					String taskId = String.valueOf(issue.getId());
    					// Check relations
    		    		ArrayList<Depend> depends = new ArrayList<Depend>();
    		    		for (IssueRelation relation: issue.getRelations()) {
    		    			if (relation.getType().equals("precedes") &&
    		    				!relation.getIssueToId().equals(issue.getId())
    		    			) {
    		    				log ("Added depencency");
    		    				Depend dependObj = new Depend(String.valueOf(relation.getIssueToId()));
    		    				dependObj.setfDifference(String.valueOf(relation.getDelay()));
    		    				depends.add(dependObj);
    		    			}
    		    		}

    		    		String parentId = gcProjId;
    		    		if (issue.getParentId() != null) {
    		    			/* find parent */
    		    			for (Issue parentIssue: orgIssues){
    		    				if (parentIssue.getId().equals(issue.getParentId())) {
		    		    			parentId = String.valueOf(issue.getParentId());
		    		    			log(taskId + " has parent " + parentId);
    		    					break;
    		    				}
    		    			}
    		    		}
    		    		
    					ganttDiagram.modifyDiagram_addTask(
							taskId, 
							parentId, null, 
							findReqSymbol(issue) + " #" + taskId + ": " + issue.getSubject(), 
							issue.getDoneRatio(), 
							issue.getDueDate(),
							GanttDiagram.TaskKind.ACTIVITY, 
							"1",
							issue.getStartDate(),
							issue.getDescription(),
							depends
    					);
    					
    					// Check if developer is the same as responsibility person
    					String assigneeId = String.valueOf(issue.getAssignee().getId());
    					String picId = null;
    					if (issue.getCustomFieldByName("要件責任者") != null) {
    						picId = issue.getCustomFieldByName("要件責任者").getValue();
    					} else if (issue.getCustomFieldByName("要求責任者") != null){
    						picId = issue.getCustomFieldByName("要求責任者").getValue();
    					}
    					String leaderId = null;
    		    		if (issue.getCustomFieldByName("チームリーダー") != null) {
	    					leaderId = issue.getCustomFieldByName("チームリーダー").getValue();
    		    		}
    					String managerId = null;
    		    		if (issue.getCustomFieldByName("責任課長") != null) {
	    					managerId = issue.getCustomFieldByName("責任課長").getValue();
    		    		}
    					
    					String whoIsPic = null;
    					if (picId != null) {
    						if (assigneeId.equals(picId)) {
    							whoIsPic = DEV;
    						} else if (leaderId.equals(picId)) {
    							whoIsPic = LEADER;
    						} else if (managerId.equals(picId)) {
    							whoIsPic = MANAGER;
    						}
    					}
    					
    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, String.valueOf(issue.getAssignee().getId()), DEV, 100, DEV.equals(whoIsPic));
    					/*
    		    		if (leaderId != null) {
	    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, leaderId, LEADER, 0, LEADER.equals(whoIsPic));
    		    		}
    		    		if (managerId != null) {
	    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, managerId, MANAGER, 0, MANAGER.equals(whoIsPic));
    		    		}
    		    		if (picId != null && !DEV.equals(whoIsPic) && !LEADER.equals(whoIsPic) && !MANAGER.equals(whoIsPic)) {
	    					ganttDiagram.modifyDiagram_addTaskResourceAllocation(taskId, picId, PIC, 0, true);
						}
    		    		*/
    		    		
    		    		// Remove the checked item
	    				issues.remove(i);
    				}
    			}	    		
	    	}
	    	
	    } catch (RedmineException e) {
	    	log("Connection Error " + e);
	    }
	    
	    ganttDiagram.writeGanttDiagram();
		
	}
	
	private static void saveDataToRedmine(String assigneeName) {
    	int temp = 0;
	    try {
	    	IssueManager issueMgr = mgr.getIssueManager();
	    	UserManager  userMgr = mgr.getUserManager();
	    	Map<String, Task> tasks = ganttDiagram.getActivitiesAndMilestones();
	    	Iterator<String> it = tasks.keySet().iterator();
	    	
	    	// Add new issues
	    	while (it.hasNext()) {
	    		String key = it.next();
	    		Task task = tasks.get(key);	    		
	    		
	    		if (task.getWebLink() == null || task.getWebLink().trim().isEmpty()) {
	    			System.out.println("New task found: " + task.getName());
	    		} else {
	    			continue;
	    		}
	    		
			    // task id format (32bit)                .                       .                       .
			    //                31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
			    // project:        1  1 
			    // version:        1  0 
			    // issue:                #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #
	    		// ----------- (1) Find project Id --------------
	    		String parentId;
	    		Task parentTsk = task;
	    		int projectId = 0;
	    		String defLeader = "";
	    		String defManager = "";
	    		String defPIC = "";
	    		String defDev = "";
	    		int defVersion = -1;
	    		do {
		    		parentId = parentTsk.getParentId();
		    		parentTsk = ganttDiagram.getTaskById(parentId);
		    		if (parentTsk != null ) {
		    			projectId = Integer.parseInt(parentId) & 0x00FFFFFF;
		    			if (defLeader.isEmpty()) {
		    				defLeader = findRscByFunction(parentTsk, LEADER);
		    			}
		    			if (defManager.isEmpty()) {
		    				defManager = findRscByFunction(parentTsk, MANAGER);
		    			}
		    			if (defPIC.isEmpty()) {
		    				defPIC = findRscByFunction(parentTsk, PIC);
		    			}
		    			if (defDev.isEmpty()) {
		    				defDev = findRscByFunction(parentTsk, DEV);
		    			}
		    			if (defVersion < 0) {
		    				Task milestone = findTaskMilestone(parentTsk);
		    				if (milestone != null) {
		    					defVersion = Integer.valueOf(milestone.getId()) & 0x3FFFFFFF;
			    			}
		    			}
		    		}
	    		} while (parentTsk != null);
	    		
	    		// ----------- (2) Set initial information --------------
	    		Issue issue = new Issue();
	    		
	    		if (issue != null) {
	    			Project project = mgr.getProjectManager().getProjectById(projectId);
	    			issue.setProject(project);
	    			for (Tracker tracker: issueMgr.getTrackers()) {
	    				if (tracker.getName().compareTo("要件") == 0) {
	    	    			issue.setTracker(tracker);
	    	    			break;
	    				}
	    			}
	    			issue.setSubject(task.getName());
	    			if (task.getNote() != null && !task.getNote().isEmpty()) {
	    				issue.setDescription(task.getNote());
	    			} else {
	    				issue.setDescription(task.getName());
	    			}
	    			issue.setStatusId(1);
	    			issue.setPriorityId(2);
	    			
	    			String dev = findRscByFunction(task, DEV);
	    			if (dev.isEmpty()) {
	    				dev = defDev;
	    			}
	    			User assignee = userMgr.getCurrentUser();
	    			if (!dev.isEmpty()) {
	    				assignee = userMgr.getUserById(Integer.valueOf(dev));
	    				issue.setAssignee(assignee);
	    			}
	    			
	    			issue.setParentId(Integer.valueOf(task.getParentId()));
	    			
	    			// custom fields
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 2, "要件責任者", findRscByFunction(task, PIC).isEmpty() ? defPIC : findRscByFunction(task, PIC))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 3, "チームリーダー", findRscByFunction(task, LEADER).isEmpty() ? defLeader : findRscByFunction(task, LEADER))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 4, "責任課長", findRscByFunction(task, MANAGER).isEmpty() ? defManager : findRscByFunction(task, MANAGER))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create(12, "[詳細設計]完了予定日", "")
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create(21, "[単体テスト]完了予定日", "")
	    			);
	    			
	    			Issue parentIssue = issueMgr.getIssueById(issue.getParentId());
	    			CustomField modelsField = parentIssue.getCustomFieldByName("対象機種");
	    			if (modelsField != null) {
	    				issue.addCustomField(modelsField);
	    			}
	    		}	    		
	    		
	    		// ----------- (2) Update optional info --------------
	    		Task milestone = findTaskMilestone(task);
	    		if (milestone != null) {
		    		for (Version version: mgr.getProjectManager().getVersions(projectId)) {
		    			if (version.getId() == (Integer.valueOf(milestone.getId()) & 0x3FFFFFFF)) {
			    			issue.setTargetVersion(version);
			    			break;
		    			}
		    		}
	    		}else if (defVersion >= 0) {
		    		for (Version version: mgr.getProjectManager().getVersions(projectId)) {
		    			if (version.getId() == defVersion) {
			    			issue.setTargetVersion(version);
			    			break;
		    			}
		    		}
	    		}
	    		
	    		if (task.getStartDate() != null) {
	    			issue.setStartDate(task.getStartDate());
	    		}
	    		if (task.getEndDate() != null) {
	    			issue.setDueDate(task.getEndDate());
	    		}
	    		
	    		// ----------- (4) Create new Issue on Server --------------
	    		try {
	    			issue =issueMgr.createIssue(issue);
	    			System.out.println("Added issue " + issue.getId());
	    			task.setId(String.valueOf(issue.getId()));
	    			task.setWebLink("http://jpeaws482.apo.epson.net/redmine2/sot/issues/" + task.getId() + ".xml");
	    		} catch (RedmineException e) {
	    			log("Error at issue creating " + e);
	    		}

	    	}
	    	
	    	// Update existed issues
	    	it = tasks.keySet().iterator();
	    	while (it.hasNext()) {
	    		String key = it.next();
	    		Task task = tasks.get(key);	    		
	    		boolean dirty = false;
	    		int issueId = Integer.parseInt(task.getId());
	    		temp = issueId;
	    		if (((issueId >> 24) & 0xFF) != 0) {
	    			continue;
	    		}
	    		
	    		Issue issue = null;
	    		try {
	    			issue = mgr.getIssueManager().getIssueById(issueId, Include.relations);
	    		} catch (Exception e) {
	    			log("Error at updating " + issueId + " --> " + task.getName());
	    		}
	    		if (issue == null) {
	    			continue;
	    		}
	    		if (!issue.getAssignee().getFullName().equals(assigneeName) && !assigneeName.equals("*")) {
	    			continue;
	    		}
	    		
	    		// ----------- (1) Delete dependencies --------------
	    		// Filter relation list by items existed only in GanttProject
	    		for (IssueRelation relation: issue.getRelations()) {
	    			if (tasks.containsKey(String.valueOf(relation.getIssueId())) && 
	    				tasks.containsKey(String.valueOf(relation.getIssueToId())) &&
	    				relation.getIssueId() == issueId &&
	    				relation.getType().equals("precedes")
	    			) {
	    				issueMgr.deleteRelation(relation.getId());									// delete that item
	    				dirty = true;
	    			}
	    		}
	    		
	    		// Commit deleted relation before update date-time
	    		if (dirty) {
	    			issueMgr.update(issue);
	    			log("Deleted Information of Issue: " + issueId);
	    			dirty = false;
	    		}
	    	}
	    	
	    	it = tasks.keySet().iterator();
	    	while (it.hasNext()) {
	    		String key = it.next();
	    		Task task = tasks.get(key);	    		
	    		boolean dirty = false;
	    		int issueId = Integer.parseInt(task.getId());
	    		temp = issueId;
	    		if (((issueId >> 24) & 0xFF) > 0) {
	    			continue;
	    		}
	    		String parentId;
	    		Task parentTsk = task;
	    		int projectId = 0;
	    		do {
		    		parentId = parentTsk.getParentId();
		    		parentTsk = ganttDiagram.getTaskById(parentId);
		    		if (parentTsk != null ) {
		    			projectId = Integer.parseInt(parentId) & 0x3FFFFFFF;
		    		}
	    		} while (parentTsk != null);
	    		
	    		Issue issue = null;
	    		try {
	    			issue = mgr.getIssueManager().getIssueById(issueId, Include.relations);
	    		} catch (Exception e) {
	    			log("Error at updating " + issueId + " --> " + task.getName());
	    		}
	    		if (issue == null) {
	    			continue;
	    		}
	    		if (!issue.getAssignee().getFullName().equals(assigneeName) && !assigneeName.equals("*")) {
	    			continue;
	    		}
	    		
	    		// ----------- (2) Update data --------------
	    		Task milestone = findTaskMilestone(task);
	    		if (milestone != null && (issue.getTargetVersion() == null || (Integer.valueOf(milestone.getId()) & 0x3FFFFFFF) != issue.getTargetVersion().getId())) {
		    		for (Version version: mgr.getProjectManager().getVersions(projectId)) {
		    			if (version.getId() == (Integer.valueOf(milestone.getId()) & 0x3FFFFFFF)) {
			    			log("Updated Version of Issue: " + issueId + " to " + version.getId());
			    			issue.setTargetVersion(version);
			    			dirty = true;
			    			break;
		    			}
		    		}
	    		}	    		

	    		int nParentId = Integer.valueOf(task.getParentId()); 
	    		if ((nParentId & 0xFF000000) == 0 && issue.getParentId() != nParentId) {
	    			log("Updated Parent of Issue: " + issueId);
	    			issue.setParentId(nParentId);
	    			dirty = true;
	    		}
	    		if (issue.getStartDate() == null || issue.getStartDate().compareTo(task.getStartDate()) != 0) {
	    			log("Updated Start-Date of Issue: " + issueId);
	    			issue.setStartDate(task.getStartDate());
	    			dirty = true;
	    		}
	    		if (issue.getDueDate() == null || issue.getDueDate().compareTo(task.getEndDate()) != 0) {
	    			log("Updated Due-Date of Issue: " + issueId);
	    			issue.setDueDate(task.getEndDate());
	    			dirty = true;
	    		}
	    		if (issue.getDoneRatio().compareTo(task.getCompleteLevel()) != 0) {
	    			log("Updated Done-Ration of Issue: " + issueId);
	    			issue.setDoneRatio(task.getCompleteLevel());
	    			dirty = true;
	    		}
	    		if (task.getNote() != null &&
	    		   (issue.getDescription() == null || 
	    		   issue.getDescription().replaceAll("\\r|\\n", "").compareTo(task.getNote().replaceAll("\\r|\\n", "")) != 0) 
	    		){
	    			log("Updated Description of Issue: " + issueId);
	    			issue.setDescription(task.getNote());
	    			dirty = true;
	    		}
	    		
    			String dev = findRscByFunction(task, DEV);
    			User assignee = issue.getAssignee();
    			if (assignee == null || !dev.isEmpty() && !dev.equals(String.valueOf(assignee.getId()))) {
	    			log("update Developer of Issue: " + issueId);
    				assignee = userMgr.getUserById(Integer.valueOf(dev));
    				issue.setAssignee(assignee);
    				dirty = true;
    			}
	    		/*
	    		CustomField customField = issue.getCustomFieldByName("要件責任者");
	    		String cfValue = findRscByFunction(task, PIC);
	    		if (customField != null && !cfValue.isEmpty() && !cfValue.equals(customField.getValue())) {
	    			log("update Per-in-charge of Issue: " + issueId);
	    			customField.setValue(cfValue);
	    			dirty = true;
	    		}
	    		customField = issue.getCustomFieldByName("チームリーダー");
	    		cfValue = findRscByFunction(task, LEADER);
	    		if (customField != null && !cfValue.isEmpty() && !cfValue.equals(customField.getValue())) {
	    			log("update Team-leader of Issue: " + issueId);
	    			customField.setValue(cfValue);
	    			dirty = true;
	    		}
	    		customField = issue.getCustomFieldByName("責任課長");
	    		cfValue = findRscByFunction(task, MANAGER);
	    		if (customField != null && !cfValue.isEmpty() && !cfValue.equals(customField.getValue())) {
	    			log("update Manager of Issue: " + issueId);
	    			customField.setValue(cfValue);
	    			dirty = true;
	    		}
	    		*/
	    		    		
	    		// ------------ Commit Update ------------
	    		if (dirty) {
	    			issueMgr.update(issue);
	    			dirty = false;
	    		}
	    	}
	    	
	    	it = tasks.keySet().iterator();
	    	while (it.hasNext()) {
	    		String key = it.next();
	    		Task task = tasks.get(key);	    		
	    		boolean dirty = false;
	    		int issueId = Integer.parseInt(task.getId());
	    		temp = issueId;
	    		if (((issueId >> 24) & 0xFF) > 0) {
	    			continue;
	    		}
	    		
	    		Issue issue = null;
	    		try {
	    			issue = mgr.getIssueManager().getIssueById(issueId, Include.relations);
	    		} catch (Exception e) {
	    			log("Error at updating " + issueId + " --> " + task.getName());
	    		}
	    		if (issue == null) {
	    			continue;
	    		}
	    		if (!issue.getAssignee().getFullName().equals(assigneeName) && !assigneeName.equals("*")) {
	    			continue;
	    		}

	    		// ----------- (3) Update relation existed in depend list -----------
	    		// Retrieve depend list
	    		Map<String, ArrayList<Depend>> dependencies = ganttDiagram.getDependencies();
	    		ArrayList<Depend> depends = dependencies.get(task.getId());
    			for (Depend dependObj: depends) {
    				Task dependTsk = tasks.get(dependObj.getId());
    				if (dependTsk.isActivity()) {
    					issueMgr.createRelation(												// by the new one has another delay
	    					issueId, 
	    					Integer.parseInt(dependObj.getId()), 
	    					"precedes", 
	    					Integer.parseInt(dependObj.getfDifference())
	    				);
        				dirty = true;
    				}
	    		}

	    		// ----------- (4) Commit updates --------------
	    		if (dirty) {
	    			log("Updated Relationships of Issue: " + issueId);
	    			issueMgr.update(issue);
	    		}
	    	}
	    	
	    } catch (RedmineException e) {
	    	log("Connection Error " + temp + " --> " + e);
	    	e.printStackTrace();
	    }
		
	}

}
