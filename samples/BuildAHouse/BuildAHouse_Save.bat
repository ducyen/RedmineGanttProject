echo off
call BuildAHouse_Common.bat
set ASSIGNEE=*
java %JAVA_FLAGS% -jar ..\..\release\RedmineToGanttProj.jar save
pause

