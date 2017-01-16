echo off
set API_KEY=bbda35ce24663d405685a13f9f2403f56db7f98b
set REDMINE_URL=http://www.hostedredmine.com/
set GANTTPROJ_FILE=DemoRedmine.gan
set PROJECTS=redmineganttproject
set ASSIGNEE=Duc Hoang
set START_DATE=2015-11-01
java -jar ..\..\release\RedmineToGanttProj.jar save
pause
