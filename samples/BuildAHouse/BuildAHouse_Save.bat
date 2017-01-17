echo off
call BuildAHouse_Common.bat
set ASSIGNEE=Duc Hoang
java %JAVA_FLAGS% -jar ..\..\release\RedmineToGanttProj.jar save
pause

