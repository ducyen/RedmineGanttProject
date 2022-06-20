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
	    	loadDataFromRedmine(ganttDiagram, mgr, machineNames, startDateToCheck, asigneeName);
	    else if (args[0].equals("save"))
	    	saveDataToRedmine(ganttDiagram, mgr, asigneeName);
	    else
	    	log("Parameter not matched");
	    
	    log("\n------------------End-Program---------------------\n");
	}
	
	private static void loadDataFromRedmine(
		GanttDiagram ganttDiagram, 
		RedmineManager mgr,
		String[] machineNames,
		Date startDateToCheck,
		String asigneeNameCsv
	) {
	    try {
	    	ProjectManager projMgr = mgr.getProjectManager();
	    	List<Project> projects = new ArrayList<Project>();

	    	for (String machineName: machineNames) {
	    		projects.add(projMgr.getProjectByKey(machineName));
	    	}
	    	
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
			    // assignee:       0  1  
			    // assignee index:       #  #  #  #  #  #
			    // issue:                                  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #
			    
			    // Set Redmine project
				String gcProjId = String.valueOf(project.getId() | 0xC0000000);
				Task gcProject = ganttDiagram.getTaskById(gcProjId);
				if (gcProject != null) {
					ganttDiagram.modifyDiagram_deleteTask(gcProjId);
				}
				ganttDiagram.modifyDiagram_addTask(
					gcProjId, 
					null, null, 
					project.getName(), 
					0, 
					project.getCreatedOn(),
					GanttDiagram.TaskKind.PROJECT, 
					"1",
					project.getCreatedOn(),
					project.getDescription(),
					null,
					0
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
	    			/*
	    			if (version.getProject().getId() != project.getId()) {
	    				log("Not this project");
	    				continue;
	    			}
	    			*/
	    			if (startDateToCheck.compareTo(version.getDueDate()) > 0) {
	    				continue;
	    			}
	    			if (version.getStatus().compareToIgnoreCase("open") != 0) {
	    				continue;
	    			}
					String issueGrpId = String.valueOf(version.getId() | 0x80000000);
					ganttDiagram.modifyDiagram_addTask(
						issueGrpId, 
						gcProjId, null, 
						project.getName() + "_" + version.getName(), 
						0, 
						version.getDueDate(),
						GanttDiagram.TaskKind.MILESTONE, 
						"1",
						version.getDueDate(),
						version.getDescription(),
						null,
						0
					);
					
					String[] asigneeNames = asigneeNameCsv.split(",");
					int asigneeIndex = 0;
					for (String asigneeName: asigneeNames) {
						boolean milestoneAdded = false;
		    			for (int i = issues.size() - 1; i >= 0; i--) {
		    				Issue issue = issues.get(i);
		    				Version targetVersion = issue.getTargetVersion();
		    				Project targetProject = issue.getProject();
		    				if (issue.getStartDate() == null) {
		    					issue.setStartDate(startDateToCheck);
		    				}
		    				if (targetVersion != null && targetProject != null) {
			    				if (targetVersion.getId().equals(version.getId()) && targetProject.getId().equals(project.getId())) {
			    					User assignee = issue.getAssignee();
			    					String milestoneId = String.valueOf(version.getId() | 0x40000000 | (asigneeIndex << 24));
			    					if (assignee != null && assignee.getFullName().indexOf(asigneeName) >= 0) {
				    					log("        Found Issue: " + issue + " project: " + targetProject.getId() + " milestone: " + milestoneId + " asignee: " + assignee + " asigneeName: " + asigneeName);
				    					if (!milestoneAdded) {
				    						log("Added milestone " + milestoneId);
					    					ganttDiagram.modifyDiagram_addTask(
				    							milestoneId, 
				    							gcProjId, null, 
				    							"☺ " + version.getName() + "_" + assignee.getFullName(), 
				    							0, 
				    							version.getDueDate(),
				    							GanttDiagram.TaskKind.ACTIVITY, 
				    							"1",
				    							version.getDueDate(),
				    							version.getDescription(),
				    							null,
				    							0
				    						);
					    					milestoneAdded = true;
				    					}
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

				    		    		String parentId = milestoneId;
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
				    		    		
				    					ganttDiagram.modifyDiagram_addTask(
			    							taskId, 
			    							parentId, null, 
			    							"#" + taskId + ": " + issue.getSubject(), 
			    							issue.getDoneRatio(), 
			    							issue.getDueDate(),
			    							GanttDiagram.TaskKind.ACTIVITY, 
			    							"1",
			    							issue.getStartDate(),
			    							issue.getDescription(),
			    							depends,
			    							issue.getTracker() != null ? issue.getTracker().getId() : 0
				    					);
				    					
				    		    		// Remove the checked item
					    				issues.remove(i);
			    					}
		    					}
		    				}
	    				}
		    			asigneeIndex++;
	    			}
	    			
	    		}
	    		// No version items
				String[] asigneeNames = asigneeNameCsv.split(",");
				int asigneeIndex = 0;
				for (String asigneeName: asigneeNames) {
					boolean milestoneAdded = false;
	    			for (int i = issues.size() - 1; i >= 0; i--) {
	    				Issue issue = issues.get(i);
	    				Project targetProject = issue.getProject();
    					User assignee = issue.getAssignee();
	    				if (issue.getStartDate() == null) {
	    					issue.setStartDate(startDateToCheck);
	    				}
    					if (assignee != null && assignee.getFullName().indexOf(asigneeName) >= 0 && targetProject.getId().equals(project.getId())) {
	    					log("        Found Issue: " + issue + " project: " + targetProject.getId() + " milestone: none " + " asignee: " + assignee + " asigneeName: " + asigneeName);
	    					String milestoneId = String.valueOf(project.getId() | 0x40000000 | (asigneeIndex << 24));
	    					if (!milestoneAdded) {
	    						log("Added milestone " + milestoneId);
		    					ganttDiagram.modifyDiagram_addTask(
	    							milestoneId, 
	    							gcProjId, null, 
	    							"☺ " + project.getName() + "_" + assignee.getFullName(), 
	    							0, 
	    							project.getCreatedOn(),
	    							GanttDiagram.TaskKind.ACTIVITY, 
	    							"1",
	    							project.getCreatedOn(),
	    							project.getDescription(),
	    							null,
	    							0
	    						);
		    					milestoneAdded = true;
	    					}
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
	    		    		
	    		    		String parentId = milestoneId;
	    		    		if (issue.getParentId() != null) {
	    		    			/* find parent */
	    		    			for (Issue parentIssue: orgIssues){
	    		    				if (parentIssue.getId().equals(issue.getParentId()) &&
	    		    					parentIssue.getAssignee().equals(issue.getAssignee()) &&
	    		    					parentIssue.getTargetVersion() == null) 
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
    							"#" + taskId + ": " + issue.getSubject(), 
    							issue.getDoneRatio(), 
    							issue.getDueDate(),
    							GanttDiagram.TaskKind.ACTIVITY, 
    							"1",
    							issue.getStartDate(),
    							issue.getDescription(),
    							depends,
    							issue.getTracker() != null ? issue.getTracker().getId() : 0
	    					);
	    					
	    		    		// Remove the checked item
		    				issues.remove(i);
    					}
    				}
	    			asigneeIndex++;
    			}
	    		
	    	}
	    	
	    } catch (RedmineException e) {
	    	log("Connection Error");
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
	    		boolean dirty = false;
	    		int issueId = Integer.parseInt(task.getId());
	    		
	    		if (task.getWebLink() == null || task.getWebLink().trim().isEmpty()) {
	    			System.out.println("New task found: " + task.getName());
	    		} else {
	    			continue;
	    		}
	    		
			    // task id format (32bit)                .                       .                       .
			    //                31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
			    // project:        1  1
			    // version:        1  0
			    // assignee:       0  1  
			    // assignee index:       #  #  #  #  #  #
			    // issue:                                  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #  #
	    		// ----------- (1) Find project Id --------------
	    		String parentId;
	    		Task parentTsk = task;
	    		Task projectTsk = null;
	    		int projectId = 0;
	    		do {
		    		parentId = parentTsk.getParentId();
		    		parentTsk = ganttDiagram.getTaskById(parentId);
		    		if (parentTsk != null ) {
		    			projectTsk = parentTsk;
		    			projectId = Integer.parseInt(parentId) & 0x00FFFFFF;
		    		}
	    		} while (parentTsk != null);
	    		
	    		// ----------- (2) Set initial information --------------
	    		Issue issue = new Issue();
	    		
	    		if (issue != null) {
	    			Project project = mgr.getProjectManager().getProjectById(projectId);
	    			issue.setProject(project);
	    			for (Tracker tracker: issueMgr.getTrackers()) {
	    				System.out.println("Checking tracker: " + tracker.getId());
	    				if (tracker.getId() == Integer.valueOf(task.getCustomColumn(Task.CustomColumnKind.TRACKER))) {
	    	    			issue.setTracker(tracker);
	    	    			break;
	    				}
	    			}
	    			issue.setSubject(task.getName());
	    			issue.setDescription(task.getNote());
	    			issue.setStatusId(1);
	    			issue.setPriorityId(2);	    			
	    			issue.setAssignee(userMgr.getCurrentUser());
	    			issue.setParentId(Integer.valueOf(task.getParentId()));
	    			
	    			// custom fields
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 2, "要件責任者", task.getCustomColumn(Task.CustomColumnKind.PIC))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 3, "チームリーダー", task.getCustomColumn(Task.CustomColumnKind.LEADER))
	    			);
	    			issue.addCustomField(
	    				CustomFieldFactory.create( 4, "責任課長", task.getCustomColumn(Task.CustomColumnKind.MANAGER))
	    			);
	    			
	    			CustomField modelList = CustomFieldFactory.create(1);
	    			modelList.setName("対象機種");
	    			String[] modelsName = task.getCustomColumn(Task.CustomColumnKind.MODELS).split(",");
	    			modelList.setValues(Arrays.asList(modelsName));
	    			issue.addCustomField(modelList);
	    		}	    		
	    		
	    		// ----------- (3) Create new Issue on Server --------------
	    		try {
	    			issue =issueMgr.createIssue(issue);
		    		if (task.getStartDate() != null) {
		    			issue.setStartDate(task.getStartDate());
		    		}
		    		if (task.getEndDate() != null) {
		    			issue.setDueDate(task.getEndDate());
		    		}
	    			
	    			dirty = true;
	    		} catch (RedmineException e) {
	    			log("Error at issue creating " + e);
	    		}
	    		
	    		// ----------- (4) Commit change --------------	    		
	    		if (dirty) {
	    			issueMgr.update(issue);
	    			log("New Issue Added: " + issue.getId());
	    			dirty = false;
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
    				issueMgr.createRelation(												// by the new one has another delay
    					issueId, 
    					Integer.parseInt(dependObj.getId()), 
    					"precedes", 
    					Integer.parseInt(dependObj.getfDifference())
    				);
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
