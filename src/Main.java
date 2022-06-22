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
	    GanttDiagram ganttDiagram = new GanttDiagram(ganttDiagramFile);
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

	    RedmineManager mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
	    
	    if (args[0].equals("load"))
	    	loadDataFromRedmine(ganttDiagram, mgr, machineNames, startDateToCheck, asigneeName, userIds.split(","));
	    else if (args[0].equals("save"))
	    	saveDataToRedmine(ganttDiagram, mgr, asigneeName);
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
	
	private static void loadDataFromRedmine(
		GanttDiagram ganttDiagram, 
		RedmineManager mgr,
		String[] machineNames,
		Date startDateToCheck,
		String asigneeNameCsv,
		String[] resourceIdCsv
	) {
	    try {
	    	ProjectManager projMgr = mgr.getProjectManager();
	    	List<Project> projects = new ArrayList<Project>();
	    	UserManager userMgr = mgr.getUserManager();
	    	
	    	// Load resources
	    	for (String rscId: resourceIdCsv) {
	    		int userId = Integer.valueOf(rscId);
	    		User user = userMgr.getUserById(userId);
	    		if (ganttDiagram.getResourceObjectById(rscId) != null) {
	    			ganttDiagram.modifyDiagram_deleteResource(rscId);
	    		}
	    		ganttDiagram.modifyDiagram_addResource(rscId, user.getLastName(), "開発者");
	    	}

	    	for (String machineName: machineNames) {
	    		projects.add(projMgr.getProjectByKey(machineName));
	    	}
	    	
	    	// Load project
	    	for (Project project: projects) {
	    		List<Version> versions = projMgr.getVersions(project.getId());
			    List<Issue> issues = mgr.getIssueManager().getIssues(project.getIdentifier(), null, Include.relations);
			    List<Issue> orgIssues = mgr.getIssueManager().getIssues(project.getIdentifier(), null, Include.relations);
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
			    // version:        1  0 
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
					null,
					0,
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
					String milestoneId = String.valueOf(version.getId() | 0x80000000);
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
						null,
						0,
						null
					);
					
					String[] asigneeNames = asigneeNameCsv.split(",");
	    			for (int i = issues.size() - 1; i >= 0; i--) {
	    				Issue issue = issues.get(i);
	    				Version targetVersion = issue.getTargetVersion();
	    				Project targetProject = issue.getProject();
	    				if (issue.getStartDate() == null) {
	    					issue.setStartDate(startDateToCheck);
	    				}
    					boolean eligibleToLoad = false;
    					User assignee = issue.getAssignee();
    					for (String asigneeName: asigneeNames) {
	    					if (assignee != null && assignee.getFullName().indexOf(asigneeName) >= 0) {
	    						eligibleToLoad = true;
	    						break;
	    					}		    						
    					}	    				
	    				if (targetVersion != null && targetProject != null && eligibleToLoad &&
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
	    		    					parentIssue.getAssignee().equals(issue.getAssignee()) &&
	    		    					issue.getTargetVersion().equals(parentIssue.getTargetVersion())) 
	    		    				{
			    		    			parentId = String.valueOf(issue.getParentId());
			    		    			log(taskId + " has parent " + parentId);
	    		    					break;
	    		    				}
	    		    			}
	    		    		}
	    		    		
	    		    		String[] customProperties = new String[Task.CustomColumnKind.values().length];
	    		    		if (issue.getCustomFieldByName("要件責任者") != null) {
	    		    			customProperties[Task.CustomColumnKind.PIC.ordinal()    ] = issue.getCustomFieldByName("要件責任者").getValue();
    						} else if (issue.getCustomFieldByName("要求責任者") != null) {
	    		    			customProperties[Task.CustomColumnKind.PIC.ordinal()    ] = issue.getCustomFieldByName("要求責任者").getValue();
    						}
	    		    		if (issue.getCustomFieldByName("チームリーダー") != null) {
	    		    			customProperties[Task.CustomColumnKind.LEADER.ordinal() ] = issue.getCustomFieldByName("チームリーダー").getValue();
	    		    		}
	    		    		if (issue.getCustomFieldByName("責任課長") != null) {
	    		    			customProperties[Task.CustomColumnKind.MANAGER.ordinal()] = issue.getCustomFieldByName("責任課長"      ).getValue();
	    		    		}
	    		    		if (issue.getCustomFieldByName("対象機種") != null) {
	    		    			String csvValues = "";
	    		    			int j = 0;
	    		    			for (String value: issue.getCustomFieldByName("対象機種").getValues()) {
	    		    				if( j == 0) {
	    		    					csvValues = value;
	    		    				} else {
	    		    					csvValues += "," + value;
	    		    				}
	    		    				j++;
	    		    			}
	    		    			customProperties[Task.CustomColumnKind.MODELS.ordinal()]  = csvValues;
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
    							depends,
    							issue.getTracker() != null ? issue.getTracker().getId() : 0,
    							customProperties
	    					);
	    					
	    					Task task = ganttDiagram.getTaskById(taskId);
	    					String resourceId = String.valueOf(issue.getAssignee().getId());
	    					task.addUser(resourceId, true, "developer");
	    		    		// Remove the checked item
		    				issues.remove(i);
	    				}
	    			}
	    			
					
    			}
	    		
	    	}
	    	
	    } catch (RedmineException e) {
	    	log("Connection Error " + e);
	    }
	    
	    ganttDiagram.writeGanttDiagram();
		
	}
	
	private static void saveDataToRedmine(GanttDiagram ganttDiagram, RedmineManager mgr, String assigneeName) {
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
			    // project:        1  1  0
			    // version:        1  0  0
			    // assignee:       0  1  0  #  #  #  #  # 
			    //  w/o version:   0  0  1  #  #  #  #  # 
			    // 						    assignee index      
			    // issue:                                  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #
	    		// ----------- (1) Find project Id --------------
	    		String parentId;
	    		Task parentTsk = task;
	    		int projectId = 0;
	    		String defModels = "";
	    		String defLeader = "";
	    		String defManager = "";
	    		String defPIC = "";
	    		int defVersion = -1;
	    		do {
		    		parentId = parentTsk.getParentId();
		    		parentTsk = ganttDiagram.getTaskById(parentId);
		    		if (parentTsk != null ) {
		    			projectId = Integer.parseInt(parentId) & 0x00FFFFFF;
		    			if (defModels.isEmpty()) {
		    				defModels = parentTsk.getCustomColumn(Task.CustomColumnKind.MODELS);
		    			}
		    			if (defLeader.isEmpty()) {
		    				defLeader = parentTsk.getCustomColumn(Task.CustomColumnKind.LEADER);
		    			}
		    			if (defManager.isEmpty()) {
		    				defManager = parentTsk.getCustomColumn(Task.CustomColumnKind.MANAGER);
		    			}
		    			if (defPIC.isEmpty()) {
		    				defPIC = parentTsk.getCustomColumn(Task.CustomColumnKind.PIC);
		    			}
		    			if (defVersion < 0) {
		    				if ((Integer.valueOf(parentTsk.getId()) & 0xE0000000) == 0x40000000 ||
		    					(Integer.valueOf(parentTsk.getId()) & 0xE0000000) == 0x80000000
		    				) {
		    					defVersion = Integer.valueOf(parentTsk.getId()) & 0x00FFFFFF;
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
	    			issue.setAssignee(userMgr.getCurrentUser());
	    			issue.setParentId(Integer.valueOf(task.getParentId()));
	    			
	    			// custom fields
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 2, "要件責任者", task.getCustomColumn(Task.CustomColumnKind.PIC).isEmpty() ? defPIC : task.getCustomColumn(Task.CustomColumnKind.PIC))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 3, "チームリーダー", task.getCustomColumn(Task.CustomColumnKind.LEADER).isEmpty() ? defLeader : task.getCustomColumn(Task.CustomColumnKind.LEADER))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 4, "責任課長", task.getCustomColumn(Task.CustomColumnKind.MANAGER).isEmpty() ? defManager : task.getCustomColumn(Task.CustomColumnKind.MANAGER))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create(12, "[詳細設計]完了予定日", "")
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create(21, "[単体テスト]完了予定日", "")
	    			);
	    			
	    			CustomField modelList = CustomFieldFactory.create(1);
	    			modelList.setName("対象機種");
	    			
	    			String[] modelsName;
	    			if (!task.getCustomColumn(Task.CustomColumnKind.MODELS).isEmpty()) {
	    				modelsName = task.getCustomColumn(Task.CustomColumnKind.MODELS).split(",");
	    			} else {
	    				modelsName = defModels.split(",");
	    			}
	    			modelList.setValues(Arrays.asList(modelsName));
	    			issue.addCustomField(modelList);
	    		}	    		
	    		
	    		// ----------- (2) Update optional info --------------
	    		if (defVersion >= 0) {
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
	    		int defVersion = -1;
	    		do {
		    		parentId = parentTsk.getParentId();
		    		parentTsk = ganttDiagram.getTaskById(parentId);
		    		if (parentTsk != null ) {
		    			projectId = Integer.parseInt(parentId) & 0x00FFFFFF;
		    			if (defVersion < 0) {
		    				if ((Integer.valueOf(parentTsk.getId()) & 0xE0000000) == 0x40000000 ||
		    					(Integer.valueOf(parentTsk.getId()) & 0xE0000000) == 0x80000000
		    				) {
		    					defVersion = Integer.valueOf(parentTsk.getId()) & 0x00FFFFFF;
			    			}
		    			}
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
	    		
	    		// ----------- (2) Update date-time --------------
	    		if (defVersion >= 0 && (issue.getTargetVersion() == null || defVersion != issue.getTargetVersion().getId())) {
		    		for (Version version: mgr.getProjectManager().getVersions(projectId)) {
		    			if (version.getId() == defVersion) {
			    			issue.setTargetVersion(version);
			    			dirty = true;
			    			break;
		    			}
		    		}
	    		}
	    		int nParentId = Integer.valueOf(task.getParentId()); 
	    		if ((nParentId & 0xFF000000) == 0 && issue.getParentId() != nParentId) {
	    			issue.setParentId(nParentId);
	    			dirty = true;
	    		}
	    		if (issue.getStartDate() == null || issue.getStartDate().compareTo(task.getStartDate()) != 0) {
	    			issue.setStartDate(task.getStartDate());
	    			dirty = true;
	    		}
	    		if (issue.getDueDate() == null || issue.getDueDate().compareTo(task.getEndDate()) != 0) {
	    			issue.setDueDate(task.getEndDate());
	    			dirty = true;
	    		}
	    		if (issue.getDoneRatio().compareTo(task.getCompleteLevel()) != 0) {
	    			issue.setDoneRatio(task.getCompleteLevel());
	    			dirty = true;
	    		}
	    		if (task.getNote() != null &&
	    		   (issue.getDescription() == null || 
	    		   issue.getDescription().replaceAll("\\r|\\n", "").compareTo(task.getNote().replaceAll("\\r|\\n", "")) != 0) 
	    		){
	    			issue.setDescription(task.getNote());
	    			dirty = true;
	    		}
	    		    		
	    		// ------------ Commit Update ------------
	    		if (dirty) {
	    			issueMgr.update(issue);
	    			log("Updated Date-Time of Issue: " + issueId);
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

	    		// Retrieve depend list
	    		Map<String, ArrayList<Depend>> dependencies = ganttDiagram.getDependencies();
	    		ArrayList<Depend> depends = dependencies.get(task.getId());
	    		
	    		// ----------- (3) Update relation existed in depend list -----------
    			for (Depend dependObj: depends) {
    				Task dependTsk = tasks.get(dependObj.getId());
    				if (dependTsk.isActivity()) {
    					issueMgr.createRelation(												// by the new one has another delay
	    					issueId, 
	    					Integer.parseInt(dependObj.getId()), 
	    					"precedes", 
	    					Integer.parseInt(dependObj.getfDifference())
	    				);
    				}
    				dirty = true;
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
